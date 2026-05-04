package com.soumyajit.jharkhand_project.repository;


import com.soumyajit.jharkhand_project.entity.Event;
import com.soumyajit.jharkhand_project.entity.PostStatus;
import com.soumyajit.jharkhand_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatusOrderByCreatedAtDesc(PostStatus status);
    List<Event> findByAuthorOrderByCreatedAtDesc(User author);
    org.springframework.data.domain.Page<Event> findByAuthorOrderByCreatedAtDesc(User author, org.springframework.data.domain.Pageable pageable);
    List<Event> findByStatus(PostStatus status);
    // Add this method to your EventRepository interface
    List<Event> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(PostStatus status, LocalDateTime after);

    long countByAuthor(User author);

}

