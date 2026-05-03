package com.soumyajit.jharkhand_project.controller;

import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.service.ExternalNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/external-news")
@RequiredArgsConstructor
public class ExternalNewsController {

    private final ExternalNewsService externalNewsService;

    @GetMapping("/top-headlines")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTopHeadlines() {
        Map<String, Object> newsResponse = externalNewsService.fetchTopHeadlines();
        return ResponseEntity.ok(ApiResponse.success("Successfully fetched external headlines", newsResponse));
    }
}
