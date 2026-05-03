package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.RssNewsItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated RSS Feed Service — fetches, parses, and caches RSS feeds.
 * Completely independent of existing news logic.
 * Supports News18 (media:content) and iNext (custom img tag) formats.
 */
@Service
@Slf4j
public class RssFeedService {

    private final RestTemplate restTemplate;

    // ─── Feed Configuration (hardcoded for full isolation) ─────────────────────
    private static final Map<String, List<FeedConfig>> STATE_FEEDS;

    static {
        STATE_FEEDS = new HashMap<>();
        STATE_FEEDS.put("Jharkhand", List.of(
                new FeedConfig("News18", "https://hindi.news18.com/commonfeeds/v1/hin/rss/jharkhand/jharkhand.xml",
                        FeedType.MEDIA_RSS, "https://www.google.com/s2/favicons?domain=hindi.news18.com&sz=128"),
                new FeedConfig("iNext Ranchi", "https://www.inextlive.com/rss/ranchi-top-news.xml",
                        FeedType.INEXT, "https://www.google.com/s2/favicons?domain=inextlive.com&sz=128"),
                new FeedConfig("iNext Jamshedpur", "https://www.inextlive.com/rss/jamshedpur-top-news.xml",
                        FeedType.INEXT, "https://www.google.com/s2/favicons?domain=inextlive.com&sz=128")
        ));
        STATE_FEEDS.put("Bihar", List.of(
                new FeedConfig("News18", "https://hindi.news18.com/commonfeeds/v1/hin/rss/bihar/bihar.xml",
                        FeedType.MEDIA_RSS, "https://www.google.com/s2/favicons?domain=hindi.news18.com&sz=128"),
                new FeedConfig("iNext Patna", "https://www.inextlive.com/rss/patna-top-news.xml",
                        FeedType.INEXT, "https://www.google.com/s2/favicons?domain=inextlive.com&sz=128")
        ));
    }

    // ─── In-memory Cache ──────────────────────────────────────────────────────
    private final ConcurrentHashMap<String, CacheEntry> feedCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

    // ─── Date Parsers ─────────────────────────────────────────────────────────
    // News18 format: "Sun, 3 May 2026 19:13:24 +0530"
    // iNext format:  "29 Apr 2026 16:27:01 GMT"
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            // RFC 2822 with day name (News18)
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("EEE, d MMM yyyy HH:mm:ss Z")
                    .toFormatter(Locale.ENGLISH),
            // RFC 2822 with day name (two-digit day)
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("EEE, dd MMM yyyy HH:mm:ss Z")
                    .toFormatter(Locale.ENGLISH),
            // Without day name (iNext)
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("d MMM yyyy HH:mm:ss z")
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd MMM yyyy HH:mm:ss z")
                    .toFormatter(Locale.ENGLISH),
            // ISO 8601
            DateTimeFormatter.ISO_DATE_TIME
    );

    public RssFeedService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get all RSS news items for a given state.
     * Returns cached data if available and fresh (< 15 min).
     */
    public List<RssNewsItemDto> getNewsForState(String stateName) {
        // Normalize state name
        String normalizedState = normalizeStateName(stateName);

        // Check cache
        CacheEntry cached = feedCache.get(normalizedState);
        if (cached != null && !cached.isExpired()) {
            log.debug("RSS cache hit for state: {} ({} items)", normalizedState, cached.items.size());
            return cached.items;
        }

        log.info("Fetching RSS feeds for state: {}", normalizedState);
        List<FeedConfig> feeds = STATE_FEEDS.get(normalizedState);
        if (feeds == null || feeds.isEmpty()) {
            log.warn("No RSS feeds configured for state: {}", normalizedState);
            return Collections.emptyList();
        }

        List<RssNewsItemDto> allItems = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>(); // Deduplication

        for (FeedConfig feed : feeds) {
            try {
                List<RssNewsItemDto> feedItems = fetchAndParseFeed(feed, normalizedState);
                for (RssNewsItemDto item : feedItems) {
                    // Deduplicate by title (normalized)
                    String normalizedTitle = item.getTitle().trim().toLowerCase();
                    if (seenTitles.add(normalizedTitle)) {
                        allItems.add(item);
                    }
                }
                log.info("Fetched {} items from {}", feedItems.size(), feed.name);
            } catch (Exception e) {
                log.error("Failed to fetch/parse RSS feed: {} ({})", feed.name, feed.url, e);
                // Continue with other feeds — one failure shouldn't break all
            }
        }

        // Sort by date DESC
        allItems.sort(Comparator.comparing(RssNewsItemDto::getPublishedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // Cache the result
        feedCache.put(normalizedState, new CacheEntry(allItems));
        log.info("Cached {} RSS items for state: {}", allItems.size(), normalizedState);

        return allItems;
    }

    /**
     * Force refresh the cache for a state.
     */
    public void evictCache(String stateName) {
        feedCache.remove(normalizeStateName(stateName));
    }

    // ─── Private Methods ──────────────────────────────────────────────────────

    private List<RssNewsItemDto> fetchAndParseFeed(FeedConfig feed, String stateName) throws Exception {
        // Fetch XML with a longer timeout for RSS feeds
        String xml = restTemplate.getForObject(feed.url, String.class);
        if (xml == null || xml.isEmpty()) {
            throw new RuntimeException("Empty RSS response from: " + feed.url);
        }

        // Parse XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // Required for media:content namespace
        // Security: disable external entities
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        NodeList items = doc.getElementsByTagName("item");
        List<RssNewsItemDto> result = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            try {
                Element itemEl = (Element) items.item(i);
                RssNewsItemDto dto = parseItem(itemEl, feed, stateName);
                if (dto != null && dto.getTitle() != null && !dto.getTitle().isBlank()) {
                    result.add(dto);
                }
            } catch (Exception e) {
                log.debug("Failed to parse RSS item #{} from {}: {}", i, feed.name, e.getMessage());
            }
        }

        return result;
    }

    private RssNewsItemDto parseItem(Element itemEl, FeedConfig feed, String stateName) {
        String title = getElementText(itemEl, "title");
        String link = getElementText(itemEl, "link");
        String description = getElementText(itemEl, "description");
        String pubDateStr = getElementText(itemEl, "pubDate");

        // Clean HTML from description
        if (description != null) {
            description = description.replaceAll("<[^>]*>", "").trim();
            if (description.length() > 300) {
                description = description.substring(0, 300) + "...";
            }
        }

        // Parse image based on feed type
        String imageUrl = null;
        String author = null;

        switch (feed.type) {
            case MEDIA_RSS:
                // News18 uses <media:content url="..." />
                imageUrl = getMediaContentUrl(itemEl);
                // Author from <dc:creator>
                author = getElementTextNS(itemEl, "http://purl.org/dc/elements/1.1/", "creator");
                break;
            case INEXT:
                // iNext uses custom <img> tag
                imageUrl = getElementText(itemEl, "img");
                break;
        }

        // Parse date
        LocalDateTime publishedAt = parseDate(pubDateStr);

        // Generate a deterministic ID from the URL
        String id = "rss_" + Math.abs(link != null ? link.hashCode() : title.hashCode());

        return RssNewsItemDto.builder()
                .id(id)
                .title(title)
                .description(description)
                .imageUrl(imageUrl)
                .sourceUrl(link)
                .sourceName(feed.name)
                .sourceLogo(feed.logoUrl)
                .author(author)
                .publishedAt(publishedAt)
                .isExternal(true)
                .stateName(stateName)
                .build();
    }

    // ─── XML Helper Methods ───────────────────────────────────────────────────

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            if (node != null && node.getTextContent() != null) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }

    private String getElementTextNS(Element parent, String namespaceURI, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(namespaceURI, localName);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            if (node != null && node.getTextContent() != null) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }

    private String getMediaContentUrl(Element parent) {
        // Try media:content first
        NodeList mediaNodes = parent.getElementsByTagNameNS("http://search.yahoo.com/mrss/", "content");
        if (mediaNodes.getLength() > 0) {
            Element mediaEl = (Element) mediaNodes.item(0);
            String url = mediaEl.getAttribute("url");
            if (url != null && !url.isEmpty()) {
                return url;
            }
        }
        // Fallback: try <enclosure>
        NodeList enclosureNodes = parent.getElementsByTagName("enclosure");
        if (enclosureNodes.getLength() > 0) {
            Element encEl = (Element) enclosureNodes.item(0);
            String type = encEl.getAttribute("type");
            if (type != null && type.startsWith("image")) {
                return encEl.getAttribute("url");
            }
        }
        return null;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        dateStr = dateStr.trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(dateStr, formatter);
                return zdt.toLocalDateTime();
            } catch (Exception ignored) {
                // Try next formatter
            }
        }

        log.debug("Could not parse RSS date: {}", dateStr);
        return null;
    }

    private String normalizeStateName(String stateName) {
        if (stateName == null) return "";
        String lower = stateName.trim().toLowerCase();
        if (lower.equals("jharkhand")) return "Jharkhand";
        if (lower.equals("bihar")) return "Bihar";
        // Capitalize first letter
        return stateName.substring(0, 1).toUpperCase() + stateName.substring(1).toLowerCase();
    }

    // ─── Inner Classes ────────────────────────────────────────────────────────

    private enum FeedType {
        MEDIA_RSS,  // News18 style (uses media:content for images)
        INEXT       // iNext style (uses custom <img> tag)
    }

    private static class FeedConfig {
        final String name;
        final String url;
        final FeedType type;
        final String logoUrl;

        FeedConfig(String name, String url, FeedType type, String logoUrl) {
            this.name = name;
            this.url = url;
            this.type = type;
            this.logoUrl = logoUrl;
        }
    }

    private static class CacheEntry {
        final List<RssNewsItemDto> items;
        final long timestamp;

        CacheEntry(List<RssNewsItemDto> items) {
            this.items = Collections.unmodifiableList(items);
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
