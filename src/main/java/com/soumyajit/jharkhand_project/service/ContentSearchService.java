package com.soumyajit.jharkhand_project.service;

import com.soumyajit.jharkhand_project.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Production-grade content search.
 * Location-aware, time-filtered, intent-detected, flexible matching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final int MAX_RESULTS = 6;
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─── STOP WORDS: generic words that add no search value ─────────────
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            // Hindi / Hinglish filler
            "kya", "kay", "hai", "hain", "mein", "main", "ka", "ke", "ki", "se", "ko",
            "haan", "nahi", "nhi", "bhai", "yaar", "bro", "hey", "hello", "hi",
            "abhi", "naya", "tell", "me", "mere", "mera", "meri", "tera", "apna",
            "bata", "batao", "dikhao", "dikha", "show", "give", "de", "dedo", "please",
            "karo", "kar", "karna", "karke", "chahiye", "chahte", "want", "need",
            "koi", "kuch", "sab", "sabhi", "sara", "sari", "bahut", "bht",
            "acha", "achha", "accha", "aisa", "aise", "jaise", "waise",
            "bolo", "bol", "sun", "suno", "dekh", "dekho",
            "hota", "hoti", "hote", "hua", "hui", "hue", "tha", "thi", "the",
            "raha", "rahi", "rahe", "wala", "wali", "wale",
            "aur", "par", "lekin", "toh", "phir", "fir",
            // English filler
            "is", "are", "was", "were", "a", "an", "in", "on", "at",
            "for", "to", "of", "and", "or", "but", "with", "this", "that",
            "what", "how", "where", "when", "which", "who", "whom",
            "do", "does", "did", "can", "could", "should", "would", "will",
            "shall", "may", "might", "any", "some", "all", "about",
            "there", "here", "also", "just", "only", "very", "much",
            "find", "get", "list"
    ));

    // ─── INTENT WORDS: trigger category search ─────────────────────────
    private static final Set<String> NEWS_INTENTS = Set.of(
            "news", "khabar", "khabr", "taza", "taja", "samachar",
            "headline", "breaking", "update", "updates"
    );
    private static final Set<String> JOB_INTENTS = Set.of(
            "job", "jobs", "naukri", "vacancy", "vacancies",
            "hiring", "recruitment", "career", "rojgar", "employment",
            "bharti", "sarkari"
    );
    private static final Set<String> EVENT_INTENTS = Set.of(
            "event", "events", "program", "festival", "mela",
            "function", "ceremony", "workshop", "upcoming",
            "aayojan", "karyakram", "mahotsav", "utsav"
    );
    private static final Set<String> PROPERTY_INTENTS = Set.of(
            "property", "properties", "flat", "flats", "house", "rent", "sale",
            "apartment", "apartments", "plot", "plots", "land",
            "ghar", "makaan", "kiraya", "villa", "villas",
            "commercial", "bhk", "floor"
    );
    private static final Set<String> COMMUNITY_INTENTS = Set.of(
            "community", "post", "posts", "discussion", "samaj", "local"
    );

    // ─── LOCATION KEYWORDS: always preserved for search ────────────────
    private static final Set<String> LOCATION_KEYWORDS = Set.of(
            "jharkhand", "bihar",
            "ranchi", "patna", "jamshedpur", "bokaro", "dhanbad",
            "hazaribagh", "deoghar", "dumka", "giridih", "ramgarh",
            "gaya", "muzaffarpur", "bhagalpur", "purnia", "purnea",
            "darbhanga", "begusarai", "munger", "chapra", "saharsa",
            "motihari", "sasaram", "bettiah", "sitamarhi", "madhubani",
            "arrah", "lohardaga", "gumla", "simdega", "khunti",
            "jamtara", "godda", "pakur", "sahebganj", "palamu",
            "latehar", "chatra", "koderma", "chaibasa", "seraikela",
            "madhepura", "kishanganj", "katihar", "nawada", "aurangabad",
            "banka", "arwal", "jehanabad", "rohtas", "lakhisarai"
    );

    // ─── MAIN ENTRY POINT ──────────────────────────────────────────────

    public List<SearchResult> searchAll(String question) {
        String q = question != null ? question.toLowerCase().trim() : "";
        List<String> allWords = extractAllWords(q);
        Set<String> intents = detectIntents(allWords, q);
        List<String> locations = allWords.stream().filter(LOCATION_KEYWORDS::contains).collect(Collectors.toList());
        List<String> keywords = extractKeywords(allWords);

        log.info("AI Search — intents:{} keywords:{} locations:{}", intents, keywords, locations);

        List<SearchResult> results = new ArrayList<>();

        // CASE 1: No intent, no keywords, no location → show recent mix
        if (intents.isEmpty() && keywords.isEmpty() && locations.isEmpty()) {
            return getRecentContent();
        }

        // CASE 2: Intent detected (with or without keywords/locations)
        // Always fetch by intent; then additionally filter by keywords/locations if present
        boolean hasSearchTerms = !keywords.isEmpty() || !locations.isEmpty();
        List<String> searchTerms = new ArrayList<>();
        searchTerms.addAll(keywords);
        searchTerms.addAll(locations);

        if (intents.contains("NEWS") || intents.contains("COMMUNITY") ||
                (intents.isEmpty() && !hasSearchTerms)) {
            results.addAll(fetchNews(hasSearchTerms ? searchTerms : null));
            results.addAll(fetchCommunity(hasSearchTerms ? searchTerms : null));
        }
        if (intents.contains("JOB") || (intents.isEmpty() && hasSearchTerms)) {
            results.addAll(fetchJobs(hasSearchTerms ? searchTerms : null));
        }
        if (intents.contains("EVENT") || (intents.isEmpty() && hasSearchTerms)) {
            results.addAll(fetchEvents(hasSearchTerms ? searchTerms : null));
        }
        if (intents.contains("PROPERTY") || (intents.isEmpty() && hasSearchTerms)) {
            results.addAll(fetchProperties(searchTerms, keywords, locations));
        }

        // If intent was detected but no search terms → and we got 0 results from intent tables,
        // do a broader search across all tables
        if (results.isEmpty() && !intents.isEmpty() && hasSearchTerms) {
            results.addAll(fetchNews(searchTerms));
            results.addAll(fetchJobs(searchTerms));
            results.addAll(fetchEvents(searchTerms));
            results.addAll(fetchCommunity(searchTerms));
        }

        // If still nothing with keywords, try without keywords (just show by intent)
        if (results.isEmpty() && !intents.isEmpty()) {
            if (intents.contains("NEWS")) results.addAll(fetchNews(null));
            if (intents.contains("JOB")) results.addAll(fetchJobs(null));
            if (intents.contains("EVENT")) results.addAll(fetchEvents(null));
            if (intents.contains("COMMUNITY")) results.addAll(fetchCommunity(null));
            if (intents.contains("PROPERTY")) results.addAll(fetchProperties(null, null, null));
        }

        // Sort by recency
        results.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return results.stream().limit(12).collect(Collectors.toList());
    }

    // ─── INTENT DETECTION ──────────────────────────────────────────────

    private Set<String> detectIntents(List<String> words, String fullText) {
        Set<String> intents = new HashSet<>();
        for (String w : words) {
            if (NEWS_INTENTS.contains(w)) intents.add("NEWS");
            if (JOB_INTENTS.contains(w)) intents.add("JOB");
            if (EVENT_INTENTS.contains(w)) intents.add("EVENT");
            if (PROPERTY_INTENTS.contains(w)) intents.add("PROPERTY");
            if (COMMUNITY_INTENTS.contains(w)) intents.add("COMMUNITY");
        }
        // Catch short/variant Hindi keywords
        if (fullText.contains("aaj") || fullText.contains("aj ") || fullText.startsWith("aj")
                || fullText.contains("latest") || fullText.contains("recent")
                || fullText.contains("taza") || fullText.contains("taja")
                || fullText.contains("nayi") || fullText.contains("nai ")) {
            intents.add("NEWS");
        }
        return intents;
    }

    // ─── KEYWORD & WORD EXTRACTION ─────────────────────────────────────

    private List<String> extractAllWords(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.replaceAll("[^a-zA-Z0-9\\s\\u0900-\\u097F]", " ")
                        .split("\\s+"))
                .filter(w -> w.length() > 1)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> extractKeywords(List<String> words) {
        return words.stream()
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .filter(w -> !NEWS_INTENTS.contains(w) && !JOB_INTENTS.contains(w)
                        && !EVENT_INTENTS.contains(w) && !PROPERTY_INTENTS.contains(w)
                        && !COMMUNITY_INTENTS.contains(w))
                .filter(w -> !LOCATION_KEYWORDS.contains(w))
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    // ─── RECENT CONTENT (no input) ─────────────────────────────────────

    private List<SearchResult> getRecentContent() {
        List<SearchResult> r = new ArrayList<>();
        r.addAll(fetchNews(null));
        r.addAll(fetchJobs(null));
        r.addAll(fetchEvents(null));
        r.addAll(fetchCommunity(null));
        return r;
    }

    // ─── FETCH METHODS (null terms = no keyword filter) ────────────────

    private List<SearchResult> fetchNews(List<String> terms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT sn.id, sn.title, sn.content, sn.created_at, s.name ");
        sql.append("FROM state_news sn LEFT JOIN states s ON sn.state_id = s.id ");
        sql.append("WHERE sn.published = true ");
        sql.append("AND sn.created_at >= NOW() - INTERVAL '7 days' ");
        if (terms != null && !terms.isEmpty()) {
            appendKeywordFilter(sql, terms, "sn.title", "sn.content", "s.name");
        }
        sql.append(" ORDER BY sn.created_at DESC LIMIT ").append(MAX_RESULTS);
        return queryNews(sql.toString());
    }

    private List<SearchResult> fetchCommunity(List<String> terms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, title, content, created_at, location ");
        sql.append("FROM community_posts WHERE status = 'APPROVED' ");
        sql.append("AND created_at >= NOW() - INTERVAL '7 days' ");
        if (terms != null && !terms.isEmpty()) {
            appendKeywordFilter(sql, terms, "title", "content", "location");
        }
        sql.append(" ORDER BY created_at DESC LIMIT 3");
        return queryCommunity(sql.toString());
    }

    private List<SearchResult> fetchJobs(List<String> terms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, title, description, created_at, company, location, salary_range, application_deadline ");
        sql.append("FROM jobs WHERE status = 'APPROVED' ");
        sql.append("AND (application_deadline IS NULL OR application_deadline >= NOW()) ");
        if (terms != null && !terms.isEmpty()) {
            appendKeywordFilter(sql, terms, "title", "description", "location");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ").append(MAX_RESULTS);
        return queryJobs(sql.toString());
    }

    private List<SearchResult> fetchEvents(List<String> terms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, title, description, created_at, location, event_date, end_date ");
        sql.append("FROM events WHERE status = 'APPROVED' ");
        // CRITICAL: proper expiry check using COALESCE
        // If end_date exists → use it; else if event_date exists → use that; else show it
        sql.append("AND COALESCE(end_date, CAST(event_date AS date), CURRENT_DATE) >= CURRENT_DATE ");
        if (terms != null && !terms.isEmpty()) {
            appendKeywordFilter(sql, terms, "title", "description", "location");
        }
        sql.append(" ORDER BY event_date ASC NULLS LAST LIMIT ").append(MAX_RESULTS);
        return queryEvents(sql.toString());
    }

    private List<SearchResult> fetchProperties(List<String> allTerms,
                                                List<String> keywords, List<String> locations) {
        // If we have locations, try location-first approach
        if (locations != null && !locations.isEmpty()) {
            // First: try with all terms
            if (allTerms != null && !allTerms.isEmpty()) {
                List<SearchResult> r = queryPropertiesWithTerms(allTerms);
                if (!r.isEmpty()) return r;
            }
            // Fallback: just location (e.g., "flat in ranchi" → no flat, show ranchi props)
            List<SearchResult> r = queryPropertiesWithTerms(locations);
            if (!r.isEmpty()) return r;
        }
        // No location or nothing found: general search
        if (allTerms != null && !allTerms.isEmpty()) {
            return queryPropertiesWithTerms(allTerms);
        }
        // No terms at all: show recent available
        return queryPropertiesWithTerms(null);
    }

    private List<SearchResult> queryPropertiesWithTerms(List<String> terms) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, title, description, created_at, city, district, price, property_type, property_status ");
        sql.append("FROM properties WHERE status = 'APPROVED' ");
        sql.append("AND property_status IN ('FOR_SALE', 'FOR_RENT') ");
        if (terms != null && !terms.isEmpty()) {
            appendKeywordFilter(sql, terms, "title", "description", "city");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ").append(MAX_RESULTS);
        return queryProperties(sql.toString());
    }

    // ─── QUERY EXECUTORS ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<SearchResult> queryNews(String sql) {
        List<SearchResult> results = new ArrayList<>();
        try {
            List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
            for (Object[] row : rows) {
                results.add(SearchResult.builder()
                        .type("StateNews").id(toLong(row[0]))
                        .title(toStr(row[1])).content(truncate(toStr(row[2])))
                        .createdAt(toLocalDateTime(row[3]))
                        .stateName(toStr(row[4])).build());
            }
        } catch (Exception e) { log.error("News query error: {}", e.getMessage()); }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> queryCommunity(String sql) {
        List<SearchResult> results = new ArrayList<>();
        try {
            List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
            for (Object[] row : rows) {
                results.add(SearchResult.builder()
                        .type("CommunityPost").id(toLong(row[0]))
                        .title(toStr(row[1])).content(truncate(toStr(row[2])))
                        .createdAt(toLocalDateTime(row[3]))
                        .location(toStr(row[4])).build());
            }
        } catch (Exception e) { log.error("Community query error: {}", e.getMessage()); }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> queryJobs(String sql) {
        List<SearchResult> results = new ArrayList<>();
        try {
            List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
            for (Object[] row : rows) {
                LocalDateTime dl = toLocalDateTime(row[7]);
                results.add(SearchResult.builder()
                        .type("Job").id(toLong(row[0]))
                        .title(toStr(row[1])).content(truncate(toStr(row[2])))
                        .createdAt(toLocalDateTime(row[3]))
                        .company(toStr(row[4])).location(toStr(row[5]))
                        .salaryRange(toStr(row[6]))
                        .deadline(dl != null ? dl.format(DATE_FMT) : null)
                        .build());
            }
        } catch (Exception e) { log.error("Jobs query error: {}", e.getMessage()); }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> queryEvents(String sql) {
        List<SearchResult> results = new ArrayList<>();
        try {
            List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
            for (Object[] row : rows) {
                LocalDateTime evtDate = toLocalDateTime(row[5]);
                results.add(SearchResult.builder()
                        .type("Event").id(toLong(row[0]))
                        .title(toStr(row[1])).content(truncate(toStr(row[2])))
                        .createdAt(toLocalDateTime(row[3]))
                        .location(toStr(row[4]))
                        .eventDate(evtDate != null ? evtDate.format(DATETIME_FMT) : null)
                        .deadline(toStr(row[6]))
                        .build());
            }
        } catch (Exception e) { log.error("Events query error: {}", e.getMessage()); }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<SearchResult> queryProperties(String sql) {
        List<SearchResult> results = new ArrayList<>();
        try {
            List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
            for (Object[] row : rows) {
                String city = toStr(row[4]);
                String district = toStr(row[5]);
                String loc = city != null ? city : "";
                if (district != null && !district.isBlank() && !district.equals(city))
                    loc = loc.isEmpty() ? district : loc + ", " + district;

                String price = null;
                if (row[6] != null) {
                    BigDecimal p = row[6] instanceof BigDecimal ? (BigDecimal) row[6] : new BigDecimal(row[6].toString());
                    if (p.compareTo(BigDecimal.valueOf(10000000)) >= 0)
                        price = "₹" + p.divide(BigDecimal.valueOf(10000000), 1, java.math.RoundingMode.HALF_UP) + " Cr";
                    else if (p.compareTo(BigDecimal.valueOf(100000)) >= 0)
                        price = "₹" + p.divide(BigDecimal.valueOf(100000), 1, java.math.RoundingMode.HALF_UP) + " L";
                    else price = "₹" + p.toPlainString();
                }

                results.add(SearchResult.builder()
                        .type("Property").id(toLong(row[0]))
                        .title(toStr(row[1])).content(truncate(toStr(row[2])))
                        .createdAt(toLocalDateTime(row[3]))
                        .location(loc).price(price)
                        .propertyType(toStr(row[7])).build());
            }
        } catch (Exception e) { log.error("Properties query error: {}", e.getMessage()); }
        return results;
    }

    // ─── HELPERS ──────────────────────────────────────────────────────

    private void appendKeywordFilter(StringBuilder sql, List<String> terms,
                                      String col1, String col2, String col3) {
        if (terms == null || terms.isEmpty()) return;
        List<String> conds = new ArrayList<>();
        for (String t : terms) {
            String escaped = escapeSQL(t);
            StringBuilder c = new StringBuilder("(");
            c.append(col1).append(" ILIKE '%").append(escaped).append("%'");
            c.append(" OR ").append(col2).append(" ILIKE '%").append(escaped).append("%'");
            if (col3 != null) c.append(" OR ").append(col3).append(" ILIKE '%").append(escaped).append("%'");
            c.append(")");
            conds.add(c.toString());
        }
        sql.append(" AND (").append(String.join(" OR ", conds)).append(")");
    }

    private String truncate(String t) {
        if (t == null) return "";
        t = t.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return t.length() > MAX_CONTENT_LENGTH ? t.substring(0, MAX_CONTENT_LENGTH) + "..." : t;
    }

    private Long toLong(Object o) { return o != null ? ((Number) o).longValue() : null; }
    private String toStr(Object o) { return o != null ? o.toString() : null; }

    private LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (o instanceof LocalDateTime ldt) return ldt;
        if (o instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        if (o instanceof LocalDate ld) return ld.atStartOfDay();
        return null;
    }

    private String escapeSQL(String s) {
        return s.replace("'", "''").replace("\\", "\\\\");
    }
}
