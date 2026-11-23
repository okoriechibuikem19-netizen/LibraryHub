package com.libraryhub.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryhub.api.exception.ResourceNotFoundException;
import com.libraryhub.api.model.ExternalNovel;
import com.libraryhub.api.repository.ExternalNovelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;

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
    private final RestClient gutendex = RestClient.create("https://gutendex.com");
    // Read Gutendex replies as text and parse them ourselves (Jackson 2), so it doesn't depend on
    // which JSON library Spring picks for HTTP clients.
    private final ObjectMapper json = new ObjectMapper();

    public CatalogController(ExternalNovelRepository novels) {
        this.novels = novels;
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
        try {
            String body = gutendex.get().uri(b -> {
                b.path("/books").queryParam("page", page).queryParam("languages", "en");
                if (!term.isEmpty()) b.queryParam("search", term);
                if (!subject.isEmpty()) b.queryParam("topic", subject);
                return b.build();
            }).retrieve().body(String.class);
            JsonNode root = body == null ? null : json.readTree(body);
            List<ExternalNovel> found = new ArrayList<>();
            if (root != null) for (JsonNode n : root.path("results")) {
                ExternalNovel novel = toNovel(n);
                if (novel != null) found.add(novel);
            }
            if (!found.isEmpty()) novels.saveAll(found);
            return found;
        } catch (Exception e) {
            // Gutendex is down or slow: fall back to the books we saved earlier.
            log.warn("Gutendex search failed ({}), using saved books", e.getMessage());
            if (term.isEmpty() && subject.isEmpty()) return novels.findAll().stream().limit(64).toList();
            String like = term.isEmpty() ? subject : term;
            return novels.findTop100ByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(like, like);
        }
    }

    /** One book's details. Works even if nobody has searched for it before. */
    @GetMapping("/novels/{id}")
    public ExternalNovel one(@PathVariable String id) {
        Optional<ExternalNovel> saved = novels.findById(id);
        if (saved.isPresent()) return saved.get();
        String number = id.startsWith(ID_PREFIX) ? id.substring(ID_PREFIX.length()) : id;
        if (!number.matches("\\d{1,7}")) throw new ResourceNotFoundException("Book not found");
        try {
            String body = gutendex.get().uri("/books/{n}", number).retrieve().body(String.class);
            JsonNode n = body == null ? null : json.readTree(body);
            ExternalNovel novel = n == null ? null : toNovel(n);
            if (novel == null) throw new ResourceNotFoundException("Book not found");
            return novels.save(novel);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Book not found");
        }
    }

    /** The full text of the book (signed-in users only). */
    @GetMapping(value = "/novels/{id}/text", produces = "text/plain;charset=UTF-8")
    public String text(@PathVariable String id) {
        ExternalNovel novel = one(id);
        if (novel.textUrl == null || !novel.textUrl.startsWith("https://www.gutenberg.org/"))
            throw new IllegalArgumentException("Text not available for this title");
        String content = RestClient.create().get().uri(novel.textUrl).retrieve().body(String.class);
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
