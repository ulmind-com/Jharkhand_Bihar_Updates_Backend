package com.soumyajit.jharkhand_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    private String type;          // "StateNews", "Job", "Event", "Property", "CommunityPost"
    private Long id;
    private String title;
    private String content;       // Truncated for AI context
    private String location;      // City/Location if available
    private LocalDateTime createdAt;

    // Richer fields for AI context
    private String company;       // For Jobs
    private String salaryRange;   // For Jobs
    private String deadline;      // For Jobs (applicationDeadline) / Events (eventDate)
    private String price;         // For Properties
    private String propertyType;  // For Properties
    private String eventDate;     // For Events (formatted)
    private String stateName;     // For News — which state
}
