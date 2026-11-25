package com.libraryhub.api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Set;

/**
 * Checks a "Sign in with Google" token with Google itself and returns the verified email and name.
 * The token must be made for our Client ID, by Google, not expired, with a verified email.
 */
@Component
public class GoogleTokenVerifier {
    public record GoogleUser(String email, String name) {}

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private final String clientId;
    private final RestClient google = RestClient.create("https://oauth2.googleapis.com");
    private final ObjectMapper json = new ObjectMapper();

    public GoogleTokenVerifier(@Value("${app.auth.google-client-id:}") String clientId) {
        this.clientId = clientId;
    }

    public String clientId() { return clientId; }

    public GoogleUser verify(String idToken) {
        if (clientId.isBlank()) throw new IllegalArgumentException("Google sign-in is not set up");
        if (idToken == null || idToken.isBlank() || idToken.length() > 4096)
            throw new IllegalArgumentException("Google sign-in failed. Please try again.");
        try {
            String body = google.get().uri(b -> b.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve().body(String.class);
            JsonNode t = json.readTree(body);
            boolean ok = clientId.equals(t.path("aud").asText())
                    && ISSUERS.contains(t.path("iss").asText())
                    && "true".equals(t.path("email_verified").asText())
                    && t.path("exp").asLong(0) > Instant.now().getEpochSecond()
                    && !t.path("email").asText("").isBlank();
            if (!ok) throw new IllegalArgumentException("Google sign-in failed. Please try again.");
            String name = t.path("name").asText("");
            return new GoogleUser(t.path("email").asText(), name.isBlank() ? t.path("email").asText().split("@")[0] : name);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Google sign-in failed. Please try again.");
        }
    }
}
