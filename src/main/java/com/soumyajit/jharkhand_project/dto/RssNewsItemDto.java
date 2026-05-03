package com.soumyajit.jharkhand_project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified DTO for news items — works for both DB news and RSS feed news.
 * Used by the merged feed endpoint to return a single sorted list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RssNewsItemDto implements Comparable<RssNewsItemDto> {

    private String id;              // DB id (as string) or RSS hash
    private String title;
    private String description;     // Short content/summary
    private String imageUrl;        // Primary image URL
    private String sourceUrl;       // Original article URL (null for DB news)
    private String sourceName;      // "News18", "iNext Ranchi", or "Jharkhand Bihar Updates"
    private String sourceLogo;      // Favicon URL of the source
    private String author;          // Author name
    private String authorAvatar;    // Author avatar URL (for DB news)
    private LocalDateTime publishedAt;
    @JsonProperty("isExternal")
    private boolean isExternal;     // true = RSS, false = DB news
    private String stateName;       // "Jharkhand" or "Bihar"

    @Override
    public int compareTo(RssNewsItemDto other) {
        // Sort by publishedAt DESC (most recent first)
        if (this.publishedAt == null && other.publishedAt == null) return 0;
        if (this.publishedAt == null) return 1;
        if (other.publishedAt == null) return -1;
        return other.publishedAt.compareTo(this.publishedAt);
    }
}
