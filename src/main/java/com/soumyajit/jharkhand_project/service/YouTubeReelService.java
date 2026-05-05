package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.ReelItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated YouTube RSS feed parser for Reels.
 * Fetches YouTube Atom feeds and converts entries to ReelItemDto.
 * Completely independent of RssFeedService.
 */
@Service
@Slf4j
public class YouTubeReelService {

    private final RestTemplate restTemplate;

    // ─── Channel Configuration (state-wise) ───────────────────────────────────
    private static final Map<String, List<YtChannelConfig>> STATE_YT_CHANNELS;

    static {
        STATE_YT_CHANNELS = new HashMap<>();
        STATE_YT_CHANNELS.put("Jharkhand", List.of(
                new YtChannelConfig("JH Update", "UCUNqDpf0V7FPKl17qOUPyEw",
                        "https://www.google.com/s2/favicons?domain=youtube.com&sz=128")
        ));
        STATE_YT_CHANNELS.put("Bihar", List.of(
                new YtChannelConfig("News18 Bihar Jharkhand", "UC531MlZA5LUbeGwEN_zcppw",
                        "https://www.google.com/s2/favicons?domain=youtube.com&sz=128")
        ));
        STATE_YT_CHANNELS.put("West Bengal", List.of(
                new YtChannelConfig("ABP Ananda", "UCRWFSbif-RFENbBrSiez1DA",
                        "https://www.google.com/s2/favicons?domain=youtube.com&sz=128"),
                new YtChannelConfig("Zee 24 Ghanta", "UCIvaYmXn910QMdemBG3v1pQ",
                        "https://www.google.com/s2/favicons?domain=youtube.com&sz=128")
        ));
    }

    // ─── Cache ────────────────────────────────────────────────────────────────
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

    public YouTubeReelService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get YouTube reels for a given state. Results are cached for 15 minutes.
     */
    public List<ReelItemDto> getReelsForState(String stateName) {
        String normalizedState = normalizeStateName(stateName);
        CacheEntry entry = cache.get(normalizedState);
        if (entry != null && !entry.isExpired()) {
            log.debug("YouTube reel cache HIT for state: {}", normalizedState);
            return entry.items;
        }

        log.info("YouTube reel cache MISS for state: {} — fetching fresh", normalizedState);
        List<YtChannelConfig> channels = STATE_YT_CHANNELS.getOrDefault(normalizedState, Collections.emptyList());
        List<ReelItemDto> allReels = new ArrayList<>();

        for (YtChannelConfig channel : channels) {
            try {
                String feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + channel.channelId;
                String xml = restTemplate.getForObject(feedUrl, String.class);
                if (xml != null) {
                    List<ReelItemDto> channelReels = parseAtomFeed(xml, channel, normalizedState);
                    allReels.addAll(channelReels);
                }
            } catch (Exception e) {
                log.error("Failed to fetch YouTube feed for channel: {} ({})", channel.name, channel.channelId, e);
            }
        }

        // Sort by publishedAt DESC
        allReels.sort(Comparator.comparing(ReelItemDto::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        cache.put(normalizedState, new CacheEntry(allReels));
        return allReels;
    }

    /**
     * Parse YouTube Atom XML feed into ReelItemDto list.
     * YouTube Atom format:
     *   <entry>
     *     <yt:videoId>ABC123</yt:videoId>
     *     <title>Video Title</title>
     *     <published>2026-05-04T12:00:00+00:00</published>
     *     <media:group>
     *       <media:thumbnail url="https://i.ytimg.com/vi/ABC123/hqdefault.jpg" />
     *     </media:group>
     *   </entry>
     */
    private List<ReelItemDto> parseAtomFeed(String xml, YtChannelConfig channel, String stateName) {
        List<ReelItemDto> reels = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList entries = doc.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                try {
                    Element entry = (Element) entries.item(i);
                    ReelItemDto reel = parseEntry(entry, channel, stateName);
                    if (reel != null) {
                        reels.add(reel);
                    }
                } catch (Exception e) {
                    log.debug("Skipping YouTube entry due to parse error", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse YouTube Atom feed for channel: {}", channel.name, e);
        }
        return reels;
    }

    private ReelItemDto parseEntry(Element entry, YtChannelConfig channel, String stateName) {
        // Extract videoId from <yt:videoId>
        String videoId = getElementTextNS(entry, "http://www.youtube.com/xml/schemas/2015", "videoId");
        if (videoId == null || videoId.isEmpty()) return null;

        String title = getElementText(entry, "title");
        String publishedStr = getElementText(entry, "published");

        // Get thumbnail from <media:group><media:thumbnail url="..." />
        String thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
        NodeList thumbNodes = entry.getElementsByTagNameNS("http://search.yahoo.com/mrss/", "thumbnail");
        if (thumbNodes.getLength() > 0) {
            Element thumbEl = (Element) thumbNodes.item(0);
            String url = thumbEl.getAttribute("url");
            if (url != null && !url.isEmpty()) {
                thumbnailUrl = url;
            }
        }

        // Parse ISO date
        LocalDateTime publishedAt = null;
        if (publishedStr != null) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(publishedStr, DateTimeFormatter.ISO_DATE_TIME);
                // Convert UTC YouTube time to IST (Asia/Kolkata) to perfectly sync with Admin DB timestamps
                publishedAt = zdt.withZoneSameInstant(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
            } catch (Exception e) {
                log.debug("Could not parse YouTube date: {}", publishedStr);
            }
        }

        return ReelItemDto.builder()
                .id("yt_" + videoId)
                .title(title)
                .youtubeVideoId(videoId)
                .thumbnailUrl(thumbnailUrl)
                .author(channel.name)
                .sourceName(channel.name)
                .sourceLogo(channel.logoUrl)
                .publishedAt(publishedAt)
                .stateName(stateName)
                .type("YOUTUBE")
                .build();
    }

    // ─── XML Helpers ──────────────────────────────────────────────────────────

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

    private String getElementTextNS(Element parent, String ns, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(ns, localName);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            if (node != null && node.getTextContent() != null) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }

    private String normalizeStateName(String stateName) {
        if (stateName == null) return "";
        String lower = stateName.trim().toLowerCase();
        if (lower.equals("jharkhand")) return "Jharkhand";
        if (lower.equals("bihar")) return "Bihar";
        if (lower.equals("west bengal")) return "West Bengal";
        return stateName.trim();
    }

    // ─── Inner Classes ────────────────────────────────────────────────────────

    private static class YtChannelConfig {
        final String name;
        final String channelId;
        final String logoUrl;

        YtChannelConfig(String name, String channelId, String logoUrl) {
            this.name = name;
            this.channelId = channelId;
            this.logoUrl = logoUrl;
        }
    }

    private static class CacheEntry {
        final List<ReelItemDto> items;
        final long timestamp;

        CacheEntry(List<ReelItemDto> items) {
            this.items = items;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
