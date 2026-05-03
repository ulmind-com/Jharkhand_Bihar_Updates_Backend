package com.soumyajit.jharkhand_project.controller;

import com.soumyajit.jharkhand_project.Response.ApiResponse;
import com.soumyajit.jharkhand_project.dto.RssNewsItemDto;
import com.soumyajit.jharkhand_project.dto.StateNewsDto;
import com.soumyajit.jharkhand_project.service.RssFeedService;
import com.soumyajit.jharkhand_project.service.StateNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Isolated RSS Feed Controller — provides merged news feed (DB + RSS).
 * Does NOT modify any existing endpoints or controllers.
 *
 * DB items are returned in their ORIGINAL StateNewsDto format (preserving author, imageUrls, etc.)
 * with extra fields (isExternal, publishedAt) added for sorting/type detection.
 *
 * RSS items are returned as RssNewsItemDto with isExternal=true.
 */
@RestController
@RequestMapping("/rss-feed")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class RssFeedController {

    private final RssFeedService rssFeedService;
    private final StateNewsService stateNewsService;

    /**
     * Returns a merged feed of DB news + RSS news, sorted by date (most recent first).
     *
     * DB items keep their ORIGINAL format (author object, imageUrls array, createdAt, etc.)
     * so the frontend NewsCard component works perfectly without any changes.
     *
     * Page 0: Includes RSS items merged with DB news, all sorted by publishedAt DESC.
     * Page 1+: Only DB news (RSS items are already shown on page 0).
     */
    @GetMapping("/{stateName}/merged")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMergedFeed(
            @PathVariable String stateName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // 1. Fetch ALL RSS items (cached — fast, in-memory after first call)
            List<RssNewsItemDto> rssItems;
            try {
                rssItems = rssFeedService.getNewsForState(stateName);
            } catch (Exception e) {
                log.error("RSS feed fetch failed for state: {} — using DB only", stateName, e);
                rssItems = Collections.emptyList();
            }

            // 2. Fetch enough DB items to cover this virtual page
            //    We need at most (page+1)*size DB items to merge with RSS and slice correctly
            int dbFetchSize = Math.min((page + 1) * size, 200); // Cap at 200 to avoid huge queries
            Page<StateNewsDto> dbPage = stateNewsService.getNewsByState(stateName, 0, dbFetchSize);

            // 3. Build unified list — DB items keep original format, RSS items wrapped
            List<Map<String, Object>> allItems = new ArrayList<>();
            for (StateNewsDto dbItem : dbPage.getContent()) {
                allItems.add(wrapDbNews(dbItem));
            }
            for (RssNewsItemDto rssItem : rssItems) {
                allItems.add(wrapRssNews(rssItem));
            }

            // 4. Sort ALL items by publishedAt DESC (most recent first)
            allItems.sort((a, b) -> {
                LocalDateTime dateA = (LocalDateTime) a.get("publishedAt");
                LocalDateTime dateB = (LocalDateTime) b.get("publishedAt");
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            });

            // 5. Slice to get exactly this page's items (virtual pagination)
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, allItems.size());

            List<Map<String, Object>> pageItems;
            boolean hasMore;

            if (fromIndex >= allItems.size()) {
                // No more items in the merged pool — check if DB has more
                pageItems = Collections.emptyList();
                hasMore = !dbPage.isLast();
            } else {
                pageItems = allItems.subList(fromIndex, toIndex);
                // hasMore = there are more items in merged pool OR more DB items exist
                long totalMerged = rssItems.size() + dbPage.getTotalElements();
                hasMore = (long) (page + 1) * size < totalMerged;
            }

            log.info("Merged feed page {} for {}: {} items (RSS: {}, DB: {})",
                    page, stateName, pageItems.size(), rssItems.size(), dbPage.getContent().size());

            // 6. Build response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("items", pageItems);
            response.put("currentPage", page);
            response.put("hasMore", hasMore);
            response.put("totalDbPages", dbPage.getTotalPages());
            response.put("totalRssItems", rssItems.size());

            return ResponseEntity.ok(ApiResponse.success("Merged feed retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching merged feed for state: {}", stateName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve merged feed"));
        }
    }

    /**
     * Returns ONLY RSS feed items for a state (no DB news).
     */
    @GetMapping("/{stateName}/rss-only")
    public ResponseEntity<ApiResponse<List<RssNewsItemDto>>> getRssOnly(
            @PathVariable String stateName) {
        try {
            List<RssNewsItemDto> rssItems = rssFeedService.getNewsForState(stateName);
            return ResponseEntity.ok(ApiResponse.success(
                    "RSS feed retrieved successfully (" + rssItems.size() + " items)", rssItems));
        } catch (Exception e) {
            log.error("Error fetching RSS feed for state: {}", stateName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve RSS feed"));
        }
    }

    /**
     * Force refresh the RSS cache for a state.
     */
    @PostMapping("/{stateName}/refresh")
    public ResponseEntity<ApiResponse<String>> refreshCache(@PathVariable String stateName) {
        try {
            rssFeedService.evictCache(stateName);
            List<RssNewsItemDto> freshItems = rssFeedService.getNewsForState(stateName);
            return ResponseEntity.ok(ApiResponse.success(
                    "Cache refreshed — " + freshItems.size() + " items fetched", null));
        } catch (Exception e) {
            log.error("Error refreshing RSS cache for state: {}", stateName, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to refresh cache"));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Wraps a DB StateNewsDto into a Map — preserving ALL original fields
     * so the frontend NewsCard component works exactly as before.
     * Only adds isExternal=false and publishedAt for sorting/type detection.
     */
    private Map<String, Object> wrapDbNews(StateNewsDto dbNews) {
        Map<String, Object> map = new LinkedHashMap<>();

        // ✅ Preserve ALL original StateNewsDto fields exactly as-is
        map.put("id", dbNews.getId());
        map.put("title", dbNews.getTitle());
        map.put("content", dbNews.getContent());
        map.put("stateName", dbNews.getStateName());
        map.put("author", dbNews.getAuthor());              // Full AuthorDto object (firstName, lastName, profileImageUrl, role)
        map.put("imageUrls", dbNews.getImageUrls());         // Full array of image URLs
        map.put("comments", dbNews.getComments());
        map.put("createdAt", dbNews.getCreatedAt());         // Original createdAt
        map.put("updatedAt", dbNews.getUpdatedAt());
        map.put("category", dbNews.getCategory());

        // ✅ Add unified fields for sorting & type detection
        map.put("isExternal", false);
        map.put("publishedAt", dbNews.getCreatedAt());       // Map createdAt → publishedAt for unified sorting
        map.put("sourceName", "Jharkhand Bihar Updates");

        return map;
    }

    /**
     * Wraps an RSS news item into a Map for the unified response.
     */
    private Map<String, Object> wrapRssNews(RssNewsItemDto rssItem) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", rssItem.getId());
        map.put("title", rssItem.getTitle());
        map.put("description", rssItem.getDescription());
        map.put("imageUrl", rssItem.getImageUrl());
        map.put("sourceUrl", rssItem.getSourceUrl());
        map.put("sourceName", rssItem.getSourceName());
        map.put("sourceLogo", rssItem.getSourceLogo());
        map.put("author", rssItem.getAuthor());
        map.put("authorAvatar", rssItem.getAuthorAvatar());
        map.put("publishedAt", rssItem.getPublishedAt());
        map.put("isExternal", true);
        map.put("stateName", rssItem.getStateName());

        return map;
    }
}
