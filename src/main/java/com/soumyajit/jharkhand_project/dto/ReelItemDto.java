package com.soumyajit.jharkhand_project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified DTO for reel items — works for both admin-uploaded MP4 and YouTube RSS reels.
 * The frontend uses the `type` field to decide how to render:
 *   - NATIVE_MP4 → expo-av Video player
 *   - YOUTUBE    → react-native-youtube-iframe
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReelItemDto implements Comparable<ReelItemDto> {

    private String id;                // DB id or "yt_<videoId>"
    private String title;
    private String videoUrl;          // Cloudinary MP4 URL (for NATIVE_MP4)
    private String youtubeVideoId;    // YouTube video ID (for YOUTUBE type)
    private String thumbnailUrl;      // Thumbnail image
    private String author;
    private String authorAvatar;
    private String sourceName;        // "Jharkhand Bihar Updates" or "JH Update" etc.
    private String sourceLogo;
    private LocalDateTime publishedAt;
    private String stateName;

    @JsonProperty("type")
    private String type;              // "NATIVE_MP4" or "YOUTUBE"

    @Override
    public int compareTo(ReelItemDto other) {
        if (this.publishedAt == null && other.publishedAt == null) return 0;
        if (this.publishedAt == null) return 1;
        if (other.publishedAt == null) return -1;
        return other.publishedAt.compareTo(this.publishedAt);
    }
}
