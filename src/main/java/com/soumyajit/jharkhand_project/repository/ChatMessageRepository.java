package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    void deleteByUserId(Long userId);
}
