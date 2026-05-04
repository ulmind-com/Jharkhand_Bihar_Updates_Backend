package com.soumyajit.jharkhand_project.controller;


import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.UpdateUserProfileRequest;
import com.soumyajit.jharkhand_project.dto.UserProfileDto;
import com.soumyajit.jharkhand_project.dto.UserStatsDto;
import com.soumyajit.jharkhand_project.dto.UserContentDto;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserProfile(@AuthenticationPrincipal User user) {
        UserProfileDto profile = userService.getUserProfile(user);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateUserProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserProfileDto updatedProfile = userService.updateUserProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
    }

    @PutMapping("/profile/image")
    public ResponseEntity<ApiResponse<Void>> updateUserProfileImage(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile imageFile) {

        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Image file is required."));
        }
        userService.updateUserProfileImage(user, imageFile);
        return ResponseEntity.ok(ApiResponse.success("Profile image updated successfully", null));
    }


    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsDto>> getUserStats(@AuthenticationPrincipal User user) {
        UserStatsDto stats = userService.getUserStats(user);
        return ResponseEntity.ok(ApiResponse.success("User statistics retrieved successfully", stats));
    }

    @GetMapping("/my-content")
    public ResponseEntity<ApiResponse<UserContentDto>> getMyContent(@AuthenticationPrincipal User user) {
        UserContentDto content = userService.getUserContent(user);
        return ResponseEntity.ok(ApiResponse.success("User content retrieved successfully", content));
    }

    @GetMapping("/my-posts")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<?>>> getMyPosts(
            @AuthenticationPrincipal User user,
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Page<?> posts = userService.getUserPostsByCategory(user, category, page, size);
        return ResponseEntity.ok(ApiResponse.success("User posts retrieved successfully", posts));
    }

    @PutMapping("/onesignal-id")
    public ResponseEntity<ApiResponse<Void>> updateOnesignalPlayerId(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User user) {
        System.out.println("🔥 updateOnesignalPlayerId HIT for user: " + user.getEmail());

        String playerId = request.get("playerId");

        if (playerId == null || playerId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Player ID is required"));
        }

        userService.updateOnesignalPlayerId(user, playerId);

        return ResponseEntity.ok(ApiResponse.success("OneSignal Player ID updated", null));
    }

}
