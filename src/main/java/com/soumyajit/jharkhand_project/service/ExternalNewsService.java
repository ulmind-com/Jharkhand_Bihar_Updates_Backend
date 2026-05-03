package com.soumyajit.jharkhand_project.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ExternalNewsService {

    private final RestTemplate restTemplate;

    @Value("${external-news.api-keys}")
    private String apiKeysConfig; // comma-separated list of API keys

    // Tracks which key to start with (rotates when one is exhausted)
    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    public ExternalNewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String[] getApiKeys() {
        return apiKeysConfig.split(",");
    }

    /**
     * Tries a GNews API call with key rotation.
     * If one key is rate-limited (403/429), automatically tries the next one.
     */
    private ResponseEntity<Map> fetchWithKeyRotation(String urlTemplate) {
        String[] keys = getApiKeys();
        int startIdx = currentKeyIndex.get() % keys.length;

        for (int attempt = 0; attempt < keys.length; attempt++) {
            int idx = (startIdx + attempt) % keys.length;
            String key = keys[idx].trim();
            String url = urlTemplate + key;

            try {
                ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
                if (resp.getStatusCode().is2xxSuccessful()) {
                    // This key works — remember it for next call
                    currentKeyIndex.set(idx);
                    return resp;
                }
            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();
                if (status == 403 || status == 429) {
                    // Rate limited — try next key
                    log.warn("GNews API key {} exhausted (HTTP {}), trying next key...", idx + 1, status);
                    continue;
                }
                log.error("GNews API error with key {}: {} {}", idx + 1, status, e.getMessage());
            } catch (Exception e) {
                log.error("GNews API exception with key {}: {}", idx + 1, e.getMessage());
            }
        }

        log.error("All {} GNews API keys exhausted!", keys.length);
        return null;
    }

    /**
     * Fetches top Indian headlines + hot global news from GNews.io.
     * Uses API key rotation — if one key's limit is exhausted, falls back to the next.
     */
    public Map<String, Object> fetchTopHeadlines() {
        try {
            List<Map<String, Object>> allArticles = new ArrayList<>();

            // Fetch Indian headlines
            String indiaUrlTemplate = "https://gnews.io/api/v4/top-headlines?country=in&lang=en&max=10&apikey=";
            log.info("Fetching Indian headlines from GNews.io");
            ResponseEntity<Map> indiaResp = fetchWithKeyRotation(indiaUrlTemplate);

            if (indiaResp != null && indiaResp.getBody() != null) {
                List<Map<String, Object>> indiaArticles = (List<Map<String, Object>>) indiaResp.getBody().get("articles");
                if (indiaArticles != null) {
                    for (Map<String, Object> article : indiaArticles) {
                        addSourceLogo(article);
                        article.put("category", "india");
                    }
                    allArticles.addAll(indiaArticles);
                }
            }

            // Fetch hot global/world news
            String worldUrlTemplate = "https://gnews.io/api/v4/top-headlines?category=world&lang=en&max=10&apikey=";
            log.info("Fetching world headlines from GNews.io");
            ResponseEntity<Map> worldResp = fetchWithKeyRotation(worldUrlTemplate);

            if (worldResp != null && worldResp.getBody() != null) {
                List<Map<String, Object>> worldArticles = (List<Map<String, Object>>) worldResp.getBody().get("articles");
                if (worldArticles != null) {
                    for (Map<String, Object> article : worldArticles) {
                        addSourceLogo(article);
                        article.put("category", "world");
                    }
                    Set<String> existingUrls = new HashSet<>();
                    for (Map<String, Object> a : allArticles) {
                        existingUrls.add((String) a.get("url"));
                    }
                    for (Map<String, Object> wa : worldArticles) {
                        if (!existingUrls.contains(wa.get("url"))) {
                            allArticles.add(wa);
                        }
                    }
                }
            }

            log.info("Total articles fetched: {}", allArticles.size());

            Map<String, Object> result = new HashMap<>();
            result.put("articles", allArticles);
            result.put("totalResults", allArticles.size());
            return result;

        } catch (Exception e) {
            log.error("Exception fetching GNews headlines: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Adds a sourceLogo URL using Google's favicon service.
     */
    private void addSourceLogo(Map<String, Object> article) {
        try {
            Map<String, Object> source = (Map<String, Object>) article.get("source");
            if (source != null) {
                String sourceUrl = (String) source.get("url");
                if (sourceUrl != null && !sourceUrl.isEmpty()) {
                    String domain = sourceUrl.replaceAll("https?://", "").replaceAll("/.*", "");
                    source.put("logo", "https://www.google.com/s2/favicons?domain=" + domain + "&sz=128");
                }
            }
        } catch (Exception e) {
            // Skip logo
        }
    }
}
