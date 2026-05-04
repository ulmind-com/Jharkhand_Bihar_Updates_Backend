package com.soumyajit.jharkhand_project.repository;

import com.soumyajit.jharkhand_project.entity.PostStatus;
import com.soumyajit.jharkhand_project.entity.Property;
import com.soumyajit.jharkhand_project.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.soumyajit.jharkhand_project.entity.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    List<Property> findByStatusOrderByCreatedAtDesc(PostStatus status);

    List<Property> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(PostStatus status, LocalDateTime dateThreshold);

    @Query("SELECT p FROM Property p WHERE p.author.id = :userId ORDER BY p.createdAt DESC")
    List<Property> findByAuthorId(Long userId);

    Page<Property> findByStatus(PostStatus status, Pageable pageable);
    long countByAuthor(User author);
    List<Property> findByAuthorOrderByCreatedAtDesc(User author);
    org.springframework.data.domain.Page<Property> findByAuthorOrderByCreatedAtDesc(User author, org.springframework.data.domain.Pageable pageable);

    Page<Property> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    // ✅ NEW: Get all properties with pagination
    Page<Property> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
