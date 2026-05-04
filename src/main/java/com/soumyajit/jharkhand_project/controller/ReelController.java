package com.soumyajit.jharkhand_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.CreateReelRequest;
import com.soumyajit.jharkhand_project.dto.ReelItemDto;
import com.soumyajit.jharkhand_project.entity.User;
import com.soumyajit.jharkhand_project.service.StateReelService;
import com.soumyajit.jharkhand_project.service.YouTubeReelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Isolated Reels Controller — provides merged reels feed (Admin MP4 + YouTube RSS).
 * Does NOT modify any existing endpoints or controllers.
 *
 * Endpoints:
 *   GET  /reels/{stateName}/merged?page=0&size=10  — merged feed (DB + YouTube)
 *   POST /reels                                     — admin upload reel
 *   DELETE /reels/{id}                               — admin delete reel
 */
@RestController
@RequestMapping("/reels")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ReelController {

    private final StateReelService stateReelService;
    private final YouTubeReelService youTubeReelService;

    /**
     * Merged reels feed — combines admin-uploaded MP4 + YouTube RSS,
     * sorted chronologically (latest first), paginated.
     */
    @GetMapping("/{stateName}/merged")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMergedReels(
            @PathVariable String stateName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // 1. Fetch YouTube reels (cached — fast)
            List<ReelItemDto> ytReels;
            try {
                ytReels = youTubeReelService.getReelsForState(stateName);
            } catch (Exception e) {
                log.error("YouTube reel fetch failed for state: {} — using DB only", stateName, e);
                ytReels = Collections.emptyList();
            }

            // 2. Fetch DB reels (admin-uploaded)
            int dbFetchSize = Math.min((page + 1) * size, 200);
            Page<ReelItemDto> dbPage = stateReelService.getReelsByState(stateName, 0, dbFetchSize);

            // 3. Merge into unified list
            List<ReelItemDto> allReels = new ArrayList<>();
            allReels.addAll(dbPage.getContent());
            allReels.addAll(ytReels);

            // 4. Sort by publishedAt DESC
            allReels.sort(Comparator.comparing(ReelItemDto::getPublishedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));

            // 5. Paginate
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, allReels.size());

            List<ReelItemDto> pageItems;
            boolean hasMore;

            if (fromIndex >= allReels.size()) {
                pageItems = Collections.emptyList();
                hasMore = false;
            } else {
                pageItems = allReels.subList(fromIndex, toIndex);
                long totalMerged = ytReels.size() + dbPage.getTotalElements();
                hasMore = (long) (page + 1) * size < totalMerged;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", pageItems);
            result.put("currentPage", page);
            result.put("pageSize", size);
            result.put("hasMore", hasMore);

            return ResponseEntity.ok(ApiResponse.success("Reels fetched successfully", result));

        } catch (Exception e) {
            log.error("Error fetching merged reels for state: {}", stateName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch reels"));
        }
    }

    /**
     * Admin uploads a new reel (video + title + state).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('REPORTER')")
    public ResponseEntity<ApiResponse<ReelItemDto>> createReel(
            @RequestPart("reel") String reelJson,
            @RequestPart("video") MultipartFile videoFile,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            ObjectMapper mapper = new ObjectMapper();
            CreateReelRequest request = mapper.readValue(reelJson, CreateReelRequest.class);

            ReelItemDto reel = stateReelService.createReel(request, videoFile, user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Reel uploaded successfully", reel));
        } catch (Exception e) {
            log.error("Error creating reel", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload reel: " + e.getMessage()));
        }
    }

    /**
     * Admin deletes a reel.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteReel(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            stateReelService.deleteReel(id, user);
            return ResponseEntity.ok(ApiResponse.success("Reel deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting reel ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete reel"));
        }
    }
}
