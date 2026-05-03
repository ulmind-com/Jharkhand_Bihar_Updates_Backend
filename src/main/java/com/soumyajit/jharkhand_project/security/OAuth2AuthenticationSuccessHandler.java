package com.soumyajit.jharkhand_project.security;

import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.repository.UserRepository;
import com.soumyajit.jharkhand_project.service.GeoIpService;
import com.soumyajit.jharkhand_project.service.LoginHistoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final LoginHistoryService loginHistoryService;
    private final GeoIpService geoIpService;

    @Value("${app.oauth2.redirect-uri:https://jharkhandbiharupdates.com/auth/google/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        // Find user in database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 authentication"));

        // Generate JWT token
        String token = jwtUtils.generateTokenFromUsername(user.getEmail());

        String device = request.getHeader("User-Agent");
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();
        String location = geoIpService.getLocation(ip);

        loginHistoryService.saveLoginHistory(user, device, ip, location, user.getAuthProvider(), true);
        log.info("Login history saved for OAuth user: {}", email);

        // ── Detect mobile deep-link flow via cookie ────────────────────────
        String mobileRedirectUri = null;
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if ("MOBILE_AUTH_REDIRECT".equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                    mobileRedirectUri = c.getValue();
                    // Clear the cookie immediately
                    c.setMaxAge(0);
                    c.setPath("/");
                    response.addCookie(c);
                    break;
                }
            }
        }

        String targetUrl;
        if (mobileRedirectUri != null) {
            // Mobile: redirect to app deep link (exp:// in Expo Go, jharkhand:// in production)
            targetUrl = mobileRedirectUri
                    + (mobileRedirectUri.contains("?") ? "&" : "?")
                    + "accessToken=" + token
                    + "&role=ROLE_" + user.getRole().toString();
            log.info("[MobileAuth] Redirecting mobile OAuth user {} to: {}", email, mobileRedirectUri);
        } else {
            // Web: existing behaviour
            targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", token)
                    .queryParam("role", "ROLE_" + user.getRole().toString())
                    .build().toUriString();
            log.info("OAuth2 authentication successful for user: {}. Redirecting to: {}", email, targetUrl);
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
