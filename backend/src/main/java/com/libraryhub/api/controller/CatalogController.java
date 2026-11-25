package com.libraryhub.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryhub.api.catalog.OpenSources;
import com.libraryhub.api.catalog.StarterCatalog;
import com.libraryhub.api.exception.ResourceNotFoundException;
import com.libraryhub.api.model.ExternalNovel;
import com.libraryhub.api.repository.ExternalNovelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Free public-domain books from Project Gutenberg, through the Gutendex API (https://gutendex.com).
 * Browsing is public; reading the full text (/text) needs a signed-in user (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);
    private static final String ID_PREFIX = "gutenberg-";

    private final ExternalNovelRepository novels;
    // Follows redirects (Gutendex sends /books -> /books/, Gutenberg moves text files) and
    // gives up quickly, so a slow service falls back to the built-in shelf instead of hanging.
    private static final RestClient.Builder HTTP = RestClient.builder()
            .defaultHeader("User-Agent", "LibraryHub/1.0")
            .requestFactory(factory());
    private final RestClient gutendex = HTTP.clone().baseUrl("https://gutendex.com").build();
    private final RestClient web = HTTP.clone().build();
    private final OpenSources sources = new OpenSources(HTTP);

    /** Recent search results, so pages open instantly for the next visitor (kept 30 minutes). */
    private record Cached(List<ExternalNovel> books, long at) {}
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private static final long CACHE_MS = 30 * 60 * 1000L;

    private static JdkClientHttpRequestFactory factory() {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(client);
        f.setReadTimeout(Duration.ofSeconds(20));
        return f;
    }
    // Read Gutendex replies as text and parse them ourselves (Jackson 2), so it doesn't depend on
    // which JSON library Spring picks for HTTP clients.
    private final ObjectMapper json = new ObjectMapper();

    public CatalogController(ExternalNovelRepository novels) {
        this.novels = novels;
        Thread t = new Thread(() -> sources.enrichClassics(StarterCatalog.all()), "classics-covers");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Search or browse.
     *   q      words in the title or author name
     *   topic  a genre or subject, e.g. "romance", "adventure", "mathematics"
     *   page   1, 2, 3 ... (32 books per page)
     */
    @GetMapping("/novels")
    public List<ExternalNovel> search(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "") String topic,
                                      @RequestParam(defaultValue = "1") int page) {
        String term = q.trim();
        String subject = topic.trim();
        if (page < 1 || page > 1000) throw new IllegalArgumentException("Invalid page");
        if (term.length() > 100 || subject.length() > 60) throw new IllegalArgumentException("Search is too long");
        String key = term.toLowerCase(Locale.ROOT) + "|" + subject.toLowerCase(Locale.ROOT) + "|" + page;
        Cached hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.at() < CACHE_MS) return hit.books();
        if (cache.size() > 500) cache.clear();

        // Ask the three sources at the same time, but never wait more than 8 seconds for a slow one.
        CompletableFuture<List<ExternalNovel>> gutenberg = CompletableFuture.supplyAsync(() -> gutenberg(term, subject, page))
                .completeOnTimeout(page == 1 ? fallback(term, subject) : List.of(), 8, TimeUnit.SECONDS);
        CompletableFuture<List<ExternalNovel>> library = CompletableFuture.supplyAsync(() -> sources.openLibrary(term, subject, page))
                .completeOnTimeout(List.of(), 8, TimeUnit.SECONDS);
        List<ExternalNovel> textbooks = page == 1 ? sources.openStax(term, subject) : List.of();

        Map<String, ExternalNovel> out = new LinkedHashMap<>();
        for (ExternalNovel v : textbooks) out.putIfAbsent(v.id, v);
        for (ExternalNovel v : gutenberg.join()) out.putIfAbsent(v.id, v);
        List<ExternalNovel> borrowed = library.join();
        for (ExternalNovel v : borrowed) out.putIfAbsent(v.id, v);
        try {
            List<ExternalNovel> keep = new ArrayList<>(textbooks);
            keep.addAll(borrowed);
            if (!keep.isEmpty()) novels.saveAll(keep);   // so the book page can find them later
        } catch (Exception e) {
            log.warn("Could not save catalogue books: {}", e.getMessage());
        }
        List<ExternalNovel> result = new ArrayList<>(out.values());
        if (!result.isEmpty()) cache.put(key, new Cached(result, System.currentTimeMillis()));
        return result;
    }

    /** Project Gutenberg books through Gutendex, or the built-in shelf when it is unavailable. */
    private List<ExternalNovel> gutenberg(String term, String subject, int page) {
        try {
            String body = gutendex.get().uri(b -> {
                b.path("/books/").queryParam("page", page).queryParam("languages", "en");
                if (!term.isEmpty()) b.queryParam("search", term);
                if (!subject.isEmpty()) b.queryParam("topic", subject);
                return b.build();
            }).retrieve().body(String.class);
            JsonNode root = body == null ? null : json.readTree(body);
            List<ExternalNovel> found = new ArrayList<>();
            if (root != null) for (JsonNode n : root.path("results")) {
                ExternalNovel novel = toNovel(n);
                if (novel == null) continue;
                ExternalNovel classic = StarterCatalog.find(novel.id);
                if (classic != null) {          // reuse the Open Library cover and backup text we found
                    if (classic.coverUrl != null && classic.coverUrl.contains("openlibrary")) novel.coverUrl = classic.coverUrl;
                    novel.backupTextUrl = classic.backupTextUrl;
                }
                found.add(novel);
            }
            if (!found.isEmpty()) novels.saveAll(found);
            if (found.isEmpty() && page == 1) return fallback(term, subject);
            return found;
        } catch (Exception e) {
            // Gutendex is down or slow: fall back to the books we saved earlier.
            log.warn("Gutendex search failed ({}: {}), using the built-in shelf", e.getClass().getSimpleName(), e.getMessage());
            return page == 1 ? fallback(term, subject) : List.of();
        }
    }

    /** Built-in classics first, then any matching books saved from earlier searches. */
    private List<ExternalNovel> fallback(String term, String subject) {
        Map<String, ExternalNovel> out = new LinkedHashMap<>();
        for (ExternalNovel v : StarterCatalog.search(term, subject)) out.put(v.id, v);
        try {
            List<ExternalNovel> saved;
            if (term.isEmpty() && subject.isEmpty()) saved = novels.findAll().stream().limit(64).toList();
            else {
                String like = term.isEmpty() ? subject : term;
                saved = novels.findTop100ByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(like, like);
            }
            for (ExternalNovel v : saved) out.putIfAbsent(v.id, v);
        } catch (Exception e) {
            log.warn("Could not read saved books: {}", e.getMessage());
        }
        return new ArrayList<>(out.values());
    }

    /** One book's details. Works even if nobody has searched for it before. */
    @GetMapping("/novels/{id}")
    public ExternalNovel one(@PathVariable String id) {
        ExternalNovel starter = StarterCatalog.find(id);
        if (starter != null) return starter;
        Optional<ExternalNovel> saved = novels.findById(id);
        if (saved.isPresent()) return saved.get();
        if (id.startsWith("openstax-")) {
            ExternalNovel book = sources.openStaxById(id);
            if (book != null) return book;
            throw new ResourceNotFoundException("Book not found");
        }
        String number = id.startsWith(ID_PREFIX) ? id.substring(ID_PREFIX.length()) : id;
        if (!number.matches("\\d{1,7}")) throw new ResourceNotFoundException("Book not found");
        try {
            String body = gutendex.get().uri("/books/{n}/", number).retrieve().body(String.class);
            JsonNode n = body == null ? null : json.readTree(body);
            ExternalNovel novel = n == null ? null : toNovel(n);
            if (novel == null) throw new ResourceNotFoundException("Book not found");
            return novels.save(novel);
        } catch (ResourceNotFoundException e) {
            if (starter != null) return starter;
            throw e;
        } catch (Exception e) {
            if (starter != null) return starter;
            throw new ResourceNotFoundException("Book not found");
        }
    }

    /** A Project Gutenberg cover picture, fetched by the server and cached by the browser for a week. */
    @GetMapping("/cover/{number}")
    public ResponseEntity<byte[]> cover(@PathVariable String number) {
        if (!number.matches("\\d{1,7}")) return ResponseEntity.notFound().build();
        try {
            byte[] image = web.get().uri("https://www.gutenberg.org/cache/epub/{n}/pg{n}.cover.medium.jpg", number, number)
                    .retrieve().body(byte[].class);
            if (image == null || image.length == 0) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "public, max-age=604800").body(image);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** The full text of the book (signed-in users only). */
    @GetMapping(value = "/novels/{id}/text", produces = "text/plain;charset=UTF-8")
    public String text(@PathVariable String id) {
        ExternalNovel novel = one(id);
        if (novel.textUrl == null || !(novel.textUrl.startsWith("https://www.gutenberg.org/")
                || novel.textUrl.startsWith("https://archive.org/download/")))
            throw new IllegalArgumentException("This book is read on " + novel.source + "'s own website");
        String content;
        try {
            content = web.get().uri(novel.textUrl).retrieve().body(String.class);
        } catch (Exception e) {
            if (novel.backupTextUrl == null) {
                // gutenberg.org can't be reached: look for the same book's scan on Open Library / archive.org.
                sources.enrichClassics(List.of(novel));
                if (novel.backupTextUrl == null) throw new IllegalArgumentException("This book can't be opened right now. Please try another one.");
                try { novels.save(novel); } catch (Exception ignored) { }
            }
            log.warn("Main text failed for {} ({}), using the archive.org copy", id, e.getMessage());
            content = web.get().uri(novel.backupTextUrl).retrieve().body(String.class);
        }
        if (content == null) throw new IllegalArgumentException("Text not available");
        return content.length() > 1_500_000 ? content.substring(0, 1_500_000) : content;
    }

    /** Turns one Gutendex result into our ExternalNovel, or null if it can't be read online. */
    private ExternalNovel toNovel(JsonNode n) {
        if (n.path("copyright").asBoolean(false)) return null;
        JsonNode formats = n.path("formats");
        String text = "";
        Iterator<String> keys = formats.fieldNames();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith("text/plain")) { text = formats.path(key).asText(""); break; }
        }
        if (text.isBlank()) return null;
        ExternalNovel v = new ExternalNovel();
        v.id = ID_PREFIX + n.path("id").asText();
        v.title = n.path("title").asText();
        v.author = cleanAuthor(n.path("authors").path(0).path("name").asText("Unknown"));
        // If a network blocks gutenberg.org pictures, the website shows a coloured cover instead.
        v.coverUrl = formats.path("image/jpeg").asText("");
        v.sourceUrl = "https://www.gutenberg.org/ebooks/" + n.path("id").asText();
        v.textUrl = text;
        v.language = n.path("languages").path(0).asText("en");
        v.downloads = n.path("download_count").asLong();
        v.summary = n.path("summaries").path(0).asText("");
        for (JsonNode shelf : n.path("bookshelves")) {
            String name = shelf.asText().replace("Browsing: ", "").replace("Category: ", "");
            if (v.genres.size() < 5 && !v.genres.contains(name)) v.genres.add(name);
        }
        for (JsonNode subject : n.path("subjects")) {
            String name = subject.asText().split(" -- ")[0];
            if (v.genres.size() < 5 && !v.genres.contains(name)) v.genres.add(name);
        }
        return v;
    }

    /** Gutenberg writes "Austen, Jane": show it as "Jane Austen". */
    private static String cleanAuthor(String name) {
        int comma = name.indexOf(", ");
        return comma > 0 ? name.substring(comma + 2) + " " + name.substring(0, comma) : name;
    }
}
