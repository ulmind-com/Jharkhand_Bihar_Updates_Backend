package com.soumyajit.jharkhand_project.repository;


import com.soumyajit.jharkhand_project.entity.CommunityPost;
import com.soumyajit.jharkhand_project.entity.PostStatus;
import com.soumyajit.jharkhand_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findByStatusOrderByCreatedAtDesc(PostStatus status);
    List<CommunityPost> findByAuthorOrderByCreatedAtDesc(User author);
    org.springframework.data.domain.Page<CommunityPost> findByAuthorOrderByCreatedAtDesc(User author, org.springframework.data.domain.Pageable pageable);

    List<CommunityPost> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(PostStatus status, LocalDateTime after);

    long countByAuthor(User author);
    Page<CommunityPost> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);


}