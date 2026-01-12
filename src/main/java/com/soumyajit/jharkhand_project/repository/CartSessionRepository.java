package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.CartSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CartSessionRepository extends JpaRepository<CartSession, Long> {

    // Standard method (keep this for lightweight checks like in addToCart)
    Optional<CartSession> findBySessionId(String sessionId);

    // --- FIX IS HERE: OPTIMIZED FETCH QUERY ---
    // This loads the Session + Items + Product details in ONE single database query.
    // Use this specifically in your getCart() method to make it fast.
    @Query("SELECT c FROM CartSession c " +
            "LEFT JOIN FETCH c.items i " +
            "LEFT JOIN FETCH i.product " +
            "WHERE c.sessionId = :sessionId")
    Optional<CartSession> findBySessionIdWithItems(@Param("sessionId") String sessionId);

    @Modifying
    @Query("DELETE FROM CartSession c WHERE c.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    boolean existsBySessionId(String sessionId);
}