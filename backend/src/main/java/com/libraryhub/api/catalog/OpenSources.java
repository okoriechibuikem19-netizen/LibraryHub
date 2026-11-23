package com.libraryhub.api.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryhub.api.model.ExternalNovel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Two more free, legal sources:
 *  - OpenStax (openstax.org): modern university textbooks under an open licence. Read on openstax.org or as a free PDF.
 *  - Open Library (openlibrary.org): millions of books. Public-domain scans are read here (text from archive.org);
 *    in-copyright books are borrowed free on Open Library's own site, which handles the lending.
 */
public class OpenSources {
    private static final Logger log = LoggerFactory.getLogger(OpenSources.class);
    private final RestClient openStax;
    private final RestClient openLibrary;
    private final ObjectMapper json = new ObjectMapper();

    private volatile List<ExternalNovel> openStaxCache = List.of();
    private volatile Instant openStaxLoaded = Instant.EPOCH;

    /** Which OpenStax titles go under each Textbooks subject (matched on the title). */
    private static final Map<String, List<String>> SUBJECT_WORDS = Map.of(
        "mathematics", List.of("algebra", "calculus", "statistic", "trigonometry", "math", "precalculus", "prealgebra"),
        "physics", List.of("physics", "astronomy"),
        "chemistry", List.of("chemistry"),
        "biology", List.of("biology", "anatomy", "microbiology", "genetics"),
        "economics", List.of("economics", "accounting", "finance", "business", "entrepreneurship", "marketing", "management"),
        "psychology", List.of("psychology", "lifespan", "sociology"),
        "history", List.of("history", "government"),
        "philosophy", List.of("philosophy", "ethics"),
        "grammar", List.of("writing", "college success"));

    public OpenSources(RestClient.Builder http) {
        this.openStax = http.clone().baseUrl("https://openstax.org").build();
        this.openLibrary = http.clone().baseUrl("https://openlibrary.org").build();
    }

    // ---------------- OpenStax ----------------

    public List<ExternalNovel> openStaxBooks() {
        if (!openStaxCache.isEmpty() && openStaxLoaded.isAfter(Instant.now().minus(Duration.ofHours(12)))) return openStaxCache;
        try {
            String body = openStax.get().uri(b -> b.path("/apps/cms/api/v2/pages/")
                    .queryParam("type", "books.Book")
                    .queryParam("fields", "title,cover_url,book_state,high_resolution_pdf_url,webview_rex_link,book_subjects")
                    .queryParam("limit", 250).build()).retrieve().body(String.class);
            List<ExternalNovel> out = new ArrayList<>();
            for (JsonNode n : json.readTree(body).path("items")) {
                if (!"live".equals(n.path("book_state").asText())) continue;
                String slug = n.path("meta").path("slug").asText("");
                String read = n.path("webview_rex_link").asText("");
                if (slug.isEmpty() || read.isEmpty()) continue;
                ExternalNovel v = new ExternalNovel();
                v.id = "openstax-" + slug;
                v.title = n.path("title").asText();
                v.author = "OpenStax";
                v.coverUrl = n.path("cover_url").asText("");
                v.sourceUrl = "https://openstax.org/details/books/" + slug;
                v.readUrl = read;
                String pdf = n.path("high_resolution_pdf_url").asText("");
                v.pdfUrl = pdf.isEmpty() || "null".equals(pdf) ? null : pdf;
                v.language = "en";
                v.source = "OpenStax";
                v.summary = "A free, peer-reviewed university textbook from OpenStax (Rice University), openly licensed.";
                String lower = v.title.toLowerCase(Locale.ROOT);
                SUBJECT_WORDS.forEach((subject, words) -> {
                    if (words.stream().anyMatch(lower::contains)) v.genres.add(cap(subject));
                });
                for (JsonNode s : n.path("book_subjects")) {
                    String name = s.path("subject_name").asText("");
                    if (!name.isEmpty() && !v.genres.contains(name)) v.genres.add(name);
                }
                v.genres.add("Textbook");
                out.add(v);
            }
            if (!out.isEmpty()) { openStaxCache = List.copyOf(out); openStaxLoaded = Instant.now(); }
        } catch (Exception e) {
            log.warn("OpenStax list failed ({}: {})", e.getClass().getSimpleName(), e.getMessage());
        }
        return openStaxCache;
    }

    /** OpenStax books for a Textbooks subject or a search. Empty for novel genres. */
    public List<ExternalNovel> openStax(String term, String subject) {
        String t = term.toLowerCase(Locale.ROOT), s = subject.toLowerCase(Locale.ROOT);
        if (t.isEmpty() && !SUBJECT_WORDS.containsKey(s)) return List.of();
        return openStaxBooks().stream().filter(v -> {
            String tags = String.join(",", v.genres).toLowerCase(Locale.ROOT);
            boolean subjectOk = s.isEmpty() || tags.contains(s);
            boolean termOk = t.isEmpty() || v.title.toLowerCase(Locale.ROOT).contains(t) || tags.contains(t);
            return subjectOk && termOk;
        }).toList();
    }

    public ExternalNovel openStaxById(String id) {
        return openStaxBooks().stream().filter(v -> v.id.equals(id)).findFirst().orElse(null);
    }

    // ---------------- Open Library ----------------

    /** Books that can be read or borrowed free, with a cover. */
    public List<ExternalNovel> openLibrary(String term, String subject, int page) {
        if (term.isEmpty() && subject.isEmpty()) return List.of();
        try {
            String body = openLibrary.get().uri(b -> {
                b.path("/search.json")
                        .queryParam("fields", "key,title,author_name,cover_i,ia,ebook_access,first_publish_year,subject")
                        .queryParam("has_fulltext", "true")
                        .queryParam("language", "eng")
                        .queryParam("limit", 24)
                        .queryParam("page", page);
                if (!term.isEmpty()) b.queryParam("q", term);
                if (!subject.isEmpty()) b.queryParam("subject", subject);
                return b.build();
            }).retrieve().body(String.class);
            List<ExternalNovel> out = new ArrayList<>();
            for (JsonNode d : json.readTree(body).path("docs")) {
                String access = d.path("ebook_access").asText("");
                boolean free = "public".equals(access);
                if (!free && !"borrowable".equals(access)) continue;
                long cover = d.path("cover_i").asLong(0);
                String key = d.path("key").asText("");          // "/works/OL123W"
                if (cover <= 0 || !key.startsWith("/works/")) continue;
                ExternalNovel v = new ExternalNovel();
                v.id = "ol-" + key.substring("/works/".length());
                v.title = d.path("title").asText();
                v.author = d.path("author_name").path(0).asText("Unknown");
                v.coverUrl = "https://covers.openlibrary.org/b/id/" + cover + "-M.jpg";
                v.sourceUrl = "https://openlibrary.org" + key;
                v.readUrl = v.sourceUrl;
                v.source = "Open Library";
                v.language = "en";
                String ia = d.path("ia").path(0).asText("");
                if (free && ia.matches("[A-Za-z0-9._-]{1,100}"))
                    v.textUrl = "https://archive.org/download/" + ia + "/" + ia + "_djvu.txt";
                int year = d.path("first_publish_year").asInt(0);
                v.summary = year > 0 ? "First published in " + year + "." : "";
                if (!subject.isEmpty()) v.genres.add(cap(subject));
                for (JsonNode s : d.path("subject")) {
                    String name = s.asText();
                    if (v.genres.size() >= 4) break;
                    if (name.length() <= 30 && !v.genres.contains(name)) v.genres.add(name);
                }
                out.add(v);
            }
            return out;
        } catch (Exception e) {
            log.warn("Open Library search failed ({}: {})", e.getClass().getSimpleName(), e.getMessage());
            return List.of();
        }
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
