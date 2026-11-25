package com.libraryhub.api.controller;

import com.libraryhub.api.catalog.StarterCatalog;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Website numbers for the admin dashboard: users, sign-ups, reading and reviews. Admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {
    public record DayCount(String day, int count) {}
    public record BookStat(String id, String title, String author, String coverUrl, int readers, double average, int ratings) {}
    public record Stats(int totalUsers, int members, int staff, int suspended, int newToday, int newThisWeek, int activeThisWeek,
                        List<DayCount> signups, int readers, int booksStarted, int booksSaved,
                        int reviews, double averageRating, int reportedReviews,
                        List<BookStat> mostRead, List<BookStat> topRated) {}

    private final UserAccountRepository users;
    private final ReaderStateRepository states;
    private final ReviewRepository reviews;
    private final ExternalNovelRepository novels;

    public AdminStatsController(UserAccountRepository users, ReaderStateRepository states,
                                ReviewRepository reviews, ExternalNovelRepository novels) {
        this.users = users;
        this.states = states;
        this.reviews = reviews;
        this.novels = novels;
    }

    @GetMapping
    public Stats stats() {
        ZoneId zone = ZoneId.of("Africa/Lagos");
        LocalDate today = LocalDate.now(zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        Instant weekAgo = Instant.now().minus(Duration.ofDays(7));

        // Users
        List<UserAccount> all = users.findAll();
        int members = 0, staff = 0, suspended = 0, newToday = 0, newWeek = 0, active = 0;
        Map<LocalDate, Integer> perDay = new TreeMap<>();
        for (int i = 13; i >= 0; i--) perDay.put(today.minusDays(i), 0);
        for (UserAccount u : all) {
            if (u.getRole() == UserRole.MEMBER) members++; else staff++;
            if (u.getStatus() == UserStatus.SUSPENDED) suspended++;
            Instant created = u.getCreatedAt();
            if (created != null) {
                if (!created.isBefore(startOfToday)) newToday++;
                if (created.isAfter(weekAgo)) newWeek++;
                LocalDate day = created.atZone(zone).toLocalDate();
                perDay.computeIfPresent(day, (d, n) -> n + 1);
            }
            if (u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(weekAgo)) active++;
        }
        List<DayCount> signups = perDay.entrySet().stream().map(e -> new DayCount(e.getKey().toString(), e.getValue())).toList();

        // Reading
        int readers = 0, started = 0, saved = 0;
        Map<String, Integer> readersPerBook = new HashMap<>();
        for (ReaderState s : states.findAll()) {
            boolean reading = false;
            if (s.progress != null) for (var e : s.progress.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) {
                    reading = true;
                    started++;
                    readersPerBook.merge(e.getKey(), 1, Integer::sum);
                }
            }
            if (s.shelves != null) saved += s.shelves.size();
            if (reading) readers++;
        }

        // Reviews
        List<Review> allReviews = reviews.findAll();
        Map<String, int[]> perBook = new HashMap<>();   // [sum, count]
        double total = 0;
        int reported = 0;
        for (Review r : allReviews) {
            if (r.getRating() < 1 || r.getRating() > 5) continue;
            total += r.getRating();
            if (r.isReported()) reported++;
            int[] v = perBook.computeIfAbsent(r.getBookId(), k -> new int[2]);
            v[0] += r.getRating();
            v[1]++;
        }
        int reviewCount = perBook.values().stream().mapToInt(v -> v[1]).sum();
        double avg = reviewCount == 0 ? 0 : round(total / reviewCount);

        List<String> readIds = readersPerBook.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(8).map(Map.Entry::getKey).toList();
        List<String> ratedIds = perBook.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, int[]>>comparingDouble(e -> -((double) e.getValue()[0] / e.getValue()[1]))
                        .thenComparing(e -> -e.getValue()[1]))
                .limit(8).map(Map.Entry::getKey).toList();

        Map<String, ExternalNovel> books = lookup(readIds, ratedIds);
        List<BookStat> mostRead = readIds.stream().map(id -> stat(id, books, readersPerBook, perBook)).toList();
        List<BookStat> topRated = ratedIds.stream().map(id -> stat(id, books, readersPerBook, perBook)).toList();

        return new Stats(all.size(), members, staff, suspended, newToday, newWeek, active, signups,
                readers, started, saved, reviewCount, avg, reported, mostRead, topRated);
    }

    private Map<String, ExternalNovel> lookup(List<String> a, List<String> b) {
        Set<String> ids = new HashSet<>(a);
        ids.addAll(b);
        Map<String, ExternalNovel> map = new HashMap<>();
        for (String id : ids) {
            ExternalNovel starter = StarterCatalog.find(id);
            if (starter != null) map.put(id, starter);
        }
        novels.findAllById(ids.stream().filter(id -> !map.containsKey(id)).toList()).forEach(n -> map.put(n.id, n));
        return map;
    }

    private static BookStat stat(String id, Map<String, ExternalNovel> books, Map<String, Integer> readers, Map<String, int[]> ratings) {
        ExternalNovel n = books.get(id);
        int[] r = ratings.getOrDefault(id, new int[2]);
        return new BookStat(id, n == null ? id : n.title, n == null ? "" : n.author, n == null ? "" : n.coverUrl,
                readers.getOrDefault(id, 0), r[1] == 0 ? 0 : round((double) r[0] / r[1]), r[1]);
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
