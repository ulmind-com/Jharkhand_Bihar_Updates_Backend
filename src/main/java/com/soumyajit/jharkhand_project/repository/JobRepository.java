package com.soumyajit.jharkhand_project.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.soumyajit.jharkhand_project.entity.Job;
import com.soumyajit.jharkhand_project.entity.PostStatus;
import com.soumyajit.jharkhand_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatusOrderByCreatedAtDesc(PostStatus status);

    Page<Job> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    List<Job> findByAuthorOrderByCreatedAtDesc(User author);
    org.springframework.data.domain.Page<Job> findByAuthorOrderByCreatedAtDesc(User author, org.springframework.data.domain.Pageable pageable);
    List<Job> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(PostStatus status, LocalDateTime after);

    long countByAuthor(User author);

}
