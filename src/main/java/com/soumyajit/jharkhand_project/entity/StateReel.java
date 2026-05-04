package com.soumyajit.jharkhand_project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for admin-uploaded short video reels.
 * Completely isolated from StateNews — separate table, separate flow.
 */
@Entity
@Table(name = "state_reels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateReel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "video_url", nullable = false)
    private String videoUrl;         // Cloudinary MP4 URL

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;     // Cloudinary auto-generated thumbnail

    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Builder.Default
    private Boolean published = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
