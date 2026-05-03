package com.soumyajit.jharkhand_project.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private String sessionId;

    private String userMessage;
    private String aiResponse;
    private List<String> sources;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
