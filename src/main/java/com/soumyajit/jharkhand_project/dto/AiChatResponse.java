package com.soumyajit.jharkhand_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {
    private String answer;
    private String sessionId;
    private List<SourceRef> sources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceRef {
        private String type;   // "StateNews", "Job", "Event", "Property", "CommunityPost"
        private Long id;
        private String title;
    }
}
