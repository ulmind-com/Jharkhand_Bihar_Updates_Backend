package com.soumyajit.jharkhand_project.controller;

import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.*;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.repository.UserRepository;
import com.soumyajit.jharkhand_project.security.JwtUtils;
import com.soumyajit.jharkhand_project.service.AuthService;
import com.soumyajit.jharkhand_project.service.GeoIpService;
import com.soumyajit.jharkhand_project.service.LoginHistoryService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final GeoIpService geoIpService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final LoginHistoryService loginHistoryService;

    /**
     * Entry point for mobile Google OAuth login.
     * Sets a short-lived cookie so the success handler knows to redirect
     * to the jharkhand:// deep-link scheme instead of the web URL.
     */
    @GetMapping("/mobile-google-start")
    public void mobileGoogleStart(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "redirect", required = false, defaultValue = "jharkhand://auth") String redirectUri
    ) throws IOException {
        // Store the app's redirect URI in a short-lived cookie
        // In Expo Go: exp://192.168.x.x:8081/--/auth
        // In production: jharkhand://auth
        Cookie mobileCookie = new Cookie("MOBILE_AUTH_REDIRECT", redirectUri);
        mobileCookie.setMaxAge(600);
        mobileCookie.setPath("/");
        mobileCookie.setHttpOnly(true);
        mobileCookie.setSecure(true);
        response.addCookie(mobileCookie);

        // Redirect to Spring Security Google OAuth — respect Cloudflare HTTPS
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String scheme = (forwardedProto != null && !forwardedProto.isEmpty()) ? forwardedProto : request.getScheme();
        String contextPath = request.getContextPath();
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isEmpty()) host = request.getServerName();
        String targetUrl = scheme + "://" + host + contextPath + "/oauth2/authorization/google";
        response.sendRedirect(targetUrl);
        log.info("[MobileAuth] Started Google OAuth → redirect after auth: {}", redirectUri);
    }


    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent to email"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(Map.of("message", "Signup successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String device = request.getHeader("User-Agent");
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();
        String location = geoIpService.getLocation(ip);
        String loginTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm z"));

        // Call login and get reply object
        LoginReply loginReply = authService.login(body.get("email"), body.get("password"), device, location, loginTime);

        // Save login history
        User user = userRepository.findByEmail(body.get("email"))
                .orElseThrow(() -> new RuntimeException("User not found"));
        loginHistoryService.saveLoginHistory(user, device, ip, location, User.AuthProvider.LOCAL, true);

        return ResponseEntity.ok(Map.of(
                "accessToken", loginReply.getToken(),
                "tokenType", "Bearer",
                "role", loginReply.getRole()
        ));
    }

    // OAuth2 Google Login - Get JWT Token after authentication
    @GetMapping("/google/token")
    public ResponseEntity<?> getGoogleToken(@AuthenticationPrincipal OAuth2User oauth2User, HttpServletRequest request) {
        try {
            if (oauth2User == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Google"));
            }

            String email = oauth2User.getAttribute("email");
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found after OAuth2 authentication"));

            // Generate JWT token
            String token = jwtUtils.generateTokenFromUsername(user.getEmail());

            // Extract request info for login history
            String device = request.getHeader("User-Agent");
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null) ip = request.getRemoteAddr();
            String location = geoIpService.getLocation(ip);

            // Save login history for OAuth login
            loginHistoryService.saveLoginHistory(user, device, ip, location, User.AuthProvider.GOOGLE, true);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", token);
            response.put("tokenType", "Bearer");
            response.put("role", user.getRole().toString());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("authProvider", user.getAuthProvider().toString());

            log.info("JWT token generated for Google OAuth user: {}", email);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating token for Google OAuth user", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.sendPasswordResetEmail(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Password reset email sent successfully", null));
        } catch (Exception e) {
            log.error("Error processing forgot password request", e);
            return ResponseEntity.ok(ApiResponse.success("Password reset email sent successfully", null)); // Same response for security
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
        } catch (Exception e) {
            log.error("Error resetting password", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired token"));
        }
    }
}
