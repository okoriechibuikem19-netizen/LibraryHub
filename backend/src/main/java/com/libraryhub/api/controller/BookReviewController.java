package com.libraryhub.api.controller;

import com.libraryhub.api.exception.ResourceNotFoundException;
import com.libraryhub.api.model.Review;
import com.libraryhub.api.model.UserAccount;
import com.libraryhub.api.repository.ReviewRepository;
import com.libraryhub.api.repository.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Ratings and reviews on catalogue books.
 * Anyone can read them (only the reviewer's first name is shown, never the email).
 * Signed-in readers can write one review per book, change or delete it, and report others.
 */
@RestController
@RequestMapping("/api/catalog/novels/{bookId}/reviews")
public class BookReviewController {
    public record ReviewView(String id, String name, int rating, String comment, Instant createdAt, boolean mine) {}
    public record ReviewSummary(double average, int count, int[] stars, List<ReviewView> reviews) {}
    public record ReviewInput(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}

    private final ReviewRepository reviews;
    private final UserAccountRepository users;

    public BookReviewController(ReviewRepository reviews, UserAccountRepository users) {
        this.reviews = reviews;
        this.users = users;
    }

    @GetMapping
    public ReviewSummary list(@PathVariable String bookId, Authentication auth) {
        String me = signedIn(auth) ? auth.getName() : null;
        List<Review> all = reviews.findByBookIdOrderByCreatedAtDesc(bookId);
        int[] stars = new int[5];
        double total = 0;
        Map<String, String> names = new HashMap<>();
        List<ReviewView> views = new ArrayList<>();
        for (Review r : all) {
            if (r.getRating() < 1 || r.getRating() > 5) continue;
            stars[r.getRating() - 1]++;
            total += r.getRating();
            String email = r.getUserEmail() == null ? "" : r.getUserEmail();
            String name = names.computeIfAbsent(email.toLowerCase(Locale.ROOT), e ->
                    users.findByEmailIgnoreCase(e).map(UserAccount::getName).map(BookReviewController::firstName).orElse("Reader"));
            views.add(new ReviewView(r.getId(), name, r.getRating(), r.isReported() ? "" : r.getComment(),
                    r.getCreatedAt(), me != null && me.equalsIgnoreCase(email)));
        }
        int count = views.size();
        double average = count == 0 ? 0 : Math.round(total / count * 10) / 10.0;
        if (views.size() > 100) views = views.subList(0, 100);
        return new ReviewSummary(average, count, stars, views);
    }

    /** Write or change my review of this book. */
    @PutMapping
    public ReviewView save(@PathVariable String bookId, Authentication auth, @Valid @RequestBody ReviewInput input) {
        if (bookId.length() > 80) throw new IllegalArgumentException("Unknown book");
        Review r = reviews.findByUserEmailIgnoreCaseAndBookId(auth.getName(), bookId).orElseGet(Review::new);
        r.setBookId(bookId);
        r.setUserEmail(auth.getName());
        r.setRating(input.rating());
        r.setComment(input.comment() == null ? "" : input.comment().trim());
        r.setReported(false);
        r.setCreatedAt(Instant.now());
        reviews.save(r);
        String name = users.findByEmailIgnoreCase(auth.getName()).map(UserAccount::getName).map(BookReviewController::firstName).orElse("Reader");
        return new ReviewView(r.getId(), name, r.getRating(), r.getComment(), r.getCreatedAt(), true);
    }

    @DeleteMapping
    public void delete(@PathVariable String bookId, Authentication auth) {
        reviews.findByUserEmailIgnoreCaseAndBookId(auth.getName(), bookId).ifPresent(reviews::delete);
    }

    /** Hides a review's text until staff look at it (staff see reported reviews in the admin area). */
    @PostMapping("/{reviewId}/report")
    public void report(@PathVariable String bookId, @PathVariable String reviewId) {
        Review r = reviews.findById(reviewId).filter(v -> bookId.equals(v.getBookId()))
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        r.setReported(true);
        reviews.save(r);
    }

    private static boolean signedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }

    private static String firstName(String full) {
        if (full == null || full.isBlank()) return "Reader";
        return full.trim().split("\\s+")[0];
    }
}
