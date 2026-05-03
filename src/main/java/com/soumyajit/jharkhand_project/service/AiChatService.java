package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.AiChatRequest;
import com.soumyajit.jharkhand_project.dto.AiChatResponse;
import com.soumyajit.jharkhand_project.dto.SearchResult;
import com.soumyajit.jharkhand_project.entity.ChatMessage;
import com.soumyajit.jharkhand_project.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the AI chat pipeline:
 * 1. Search PostgreSQL for relevant content (time-filtered, expiry-checked)
 * 2. Build rich context from results
 * 3. Call OpenRouter API
 * 4. Store chat history in MongoDB
 * 5. Return response with structured clickable sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final ContentSearchService contentSearchService;
    private final OpenRouterClient openRouterClient;
    private final ChatMessageRepository chatMessageRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /**
     * Process a user's chat question and return an AI-generated answer.
     */
    public AiChatResponse chat(AiChatRequest request, Long userId) {
        String question = request.getQuestion();
        String sessionId = request.getSessionId();

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        // 1. Search PostgreSQL for relevant content
        List<SearchResult> searchResults = contentSearchService.searchAll(question);

        // 2. Build rich context string from search results
        String contextData = buildRichContext(searchResults);

        // 3. Load recent chat history for this session
        List<Map<String, String>> chatHistory = loadChatHistory(sessionId);

        // 4. Call OpenRouter API
        String aiAnswer = openRouterClient.chat(contextData, question, chatHistory);

        // 5. Build structured source references (for clickable navigation)
        List<AiChatResponse.SourceRef> sources = searchResults.stream()
                .map(r -> AiChatResponse.SourceRef.builder()
                        .type(r.getType())
                        .id(r.getId())
                        .title(r.getTitle())
                        .build())
                .collect(Collectors.toList());

        // 6. Store in MongoDB
        try {
            List<String> sourceStrings = sources.stream()
                    .map(s -> s.getType() + ":" + s.getId() + ":" + s.getTitle())
                    .collect(Collectors.toList());

            ChatMessage chatMessage = ChatMessage.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .userMessage(question)
                    .aiResponse(aiAnswer)
                    .sources(sourceStrings)
                    .build();
            chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            log.error("Failed to save chat message to MongoDB: {}", e.getMessage());
        }

        // 7. Return response
        return AiChatResponse.builder()
                .answer(aiAnswer)
                .sessionId(sessionId)
                .sources(sources)
                .build();
    }

    public List<ChatMessage> getChatHistory(Long userId) {
        try {
            return chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } catch (Exception e) {
            log.error("Failed to fetch chat history: {}", e.getMessage());
            return List.of();
        }
    }

    public void clearChatHistory(Long userId) {
        try {
            chatMessageRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to clear chat history: {}", e.getMessage());
        }
    }

    /**
     * Build a rich context string with all available metadata for the AI.
     */
    private String buildRichContext(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "No relevant content found in the database for this query. The database may not have content matching the user's question.";
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("Found ").append(results.size()).append(" relevant items from the database:\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            ctx.append(i + 1).append(". [").append(r.getType()).append("] ");
            ctx.append(r.getTitle());

            // Add rich metadata based on type
            if (r.getLocation() != null && !r.getLocation().isBlank()) {
                ctx.append(" | Location: ").append(r.getLocation());
            }
            if (r.getCompany() != null && !r.getCompany().isBlank()) {
                ctx.append(" | Company: ").append(r.getCompany());
            }
            if (r.getSalaryRange() != null && !r.getSalaryRange().isBlank()) {
                ctx.append(" | Salary: ").append(r.getSalaryRange());
            }
            if (r.getDeadline() != null && !r.getDeadline().isBlank()) {
                ctx.append(" | Deadline: ").append(r.getDeadline());
            }
            if (r.getPrice() != null && !r.getPrice().isBlank()) {
                ctx.append(" | Price: ").append(r.getPrice());
            }
            if (r.getPropertyType() != null && !r.getPropertyType().isBlank()) {
                ctx.append(" | Type: ").append(r.getPropertyType());
            }
            if (r.getEventDate() != null && !r.getEventDate().isBlank()) {
                ctx.append(" | Event Date: ").append(r.getEventDate());
            }
            if (r.getStateName() != null && !r.getStateName().isBlank()) {
                ctx.append(" | State: ").append(r.getStateName());
            }
            if (r.getCreatedAt() != null) {
                ctx.append(" | Posted: ").append(r.getCreatedAt().format(DATE_FMT));
            }
            ctx.append("\n");

            if (r.getContent() != null && !r.getContent().isBlank()) {
                ctx.append("   Summary: ").append(r.getContent()).append("\n");
            }
            ctx.append("\n");
        }
        return ctx.toString();
    }

    private List<Map<String, String>> loadChatHistory(String sessionId) {
        try {
            List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            List<Map<String, String>> messages = new ArrayList<>();
            int start = Math.max(0, history.size() - 3);
            for (int i = start; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                messages.add(Map.of("role", "user", "content", msg.getUserMessage()));
                messages.add(Map.of("role", "assistant", "content", msg.getAiResponse()));
            }
            return messages;
        } catch (Exception e) {
            log.error("Failed to load chat history: {}", e.getMessage());
            return List.of();
        }
    }
}
