package com.libraryhub.api.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stops password guessing: too many failed sign-ins for one email (or from one internet address)
 * blocks further tries for 15 minutes. Also limits how many accounts one address can create.
 * Kept in memory, so a restart clears it - fine for a single server.
 */
@Component
public class LoginLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILS_PER_EMAIL = 5;
    private static final int MAX_FAILS_PER_ADDRESS = 20;
    private static final int MAX_SIGNUPS_PER_ADDRESS = 5;   // per hour

    private record Counter(int count, Instant since) {}
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public static class TooManyAttemptsException extends RuntimeException {
        public TooManyAttemptsException(String message) { super(message); }
    }

    /** Call before checking the password. */
    public void checkLogin(String email, String address) {
        if (count("email:" + key(email), WINDOW) >= MAX_FAILS_PER_EMAIL
                || count("ip:" + address, WINDOW) >= MAX_FAILS_PER_ADDRESS)
            throw new TooManyAttemptsException("Too many failed sign-in attempts. Please wait 15 minutes and try again, or reset your password.");
    }

    public void loginFailed(String email, String address) {
        add("email:" + key(email), WINDOW);
        add("ip:" + address, WINDOW);
    }

    public void loginSucceeded(String email) {
        counters.remove("email:" + key(email));
    }

    /** Call before creating an account. */
    public void checkSignup(String address) {
        if (count("signup:" + address, Duration.ofHours(1)) >= MAX_SIGNUPS_PER_ADDRESS)
            throw new TooManyAttemptsException("Too many new accounts from your network. Please try again in an hour.");
        add("signup:" + address, Duration.ofHours(1));
    }

    private int count(String k, Duration window) {
        Counter c = counters.get(k);
        if (c == null) return 0;
        if (c.since().plus(window).isBefore(Instant.now())) { counters.remove(k); return 0; }
        return c.count();
    }

    private void add(String k, Duration window) {
        if (counters.size() > 50_000) counters.clear();   // simple memory guard
        counters.compute(k, (key, c) -> c == null || c.since().plus(window).isBefore(Instant.now())
                ? new Counter(1, Instant.now()) : new Counter(c.count() + 1, c.since()));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
