package com.soumyajit.jharkhand_project.controller;

import com.soumyajit.jharkhand_project.dto.AiChatRequest;
import com.soumyajit.jharkhand_project.dto.AiChatResponse;
import com.soumyajit.jharkhand_project.entity.ChatMessage;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.repository.UserRepository;
import com.soumyajit.jharkhand_project.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final UserRepository userRepository;

    /**
     * POST /api/v1/ai/chat — Main AI chat endpoint
     */
    @PostMapping
    public ResponseEntity<?> chat(@RequestBody AiChatRequest request,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty"));
        }

        Long userId = getUserId(userDetails);
        AiChatResponse response = aiChatService.chat(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/ai/chat/history — Get user's chat history
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<ChatMessage> history = aiChatService.getChatHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * DELETE /api/v1/ai/chat/history — Clear user's chat history
     */
    @DeleteMapping("/history")
    public ResponseEntity<?> clearChatHistory(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        aiChatService.clearChatHistory(userId);
        return ResponseEntity.ok(Map.of("message", "Chat history cleared"));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
