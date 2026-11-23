package com.libraryhub.api.controller;

import com.libraryhub.api.dto.AuthDtos.*;
import com.libraryhub.api.model.UserAccount;
import com.libraryhub.api.security.*;
import com.libraryhub.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Duration SESSION_LENGTH = Duration.ofHours(8);

    private final AuthService auth;
    private final JwtService jwt;
    private final boolean secure;

    public AuthController(AuthService auth, JwtService jwt, @Value("${app.auth.cookie-secure:false}") boolean secure) {
        this.auth = auth;
        this.jwt = jwt;
        this.secure = secure;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    /** Creates a member account and signs it in straight away. */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return signedIn(HttpStatus.CREATED, auth.register(request));
    }

    /** Email + password. Sets the login cookie when they are correct. */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return signedIn(HttpStatus.OK, auth.login(request));
    }

    /** Signed-in user changes their own password. */
    @PutMapping("/password")
    public MessageResponse changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(authentication.getName(), request);
        return new MessageResponse("Password changed", null);
    }

    /** Uses a reset link created by an admin. */
    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetWithTokenRequest request) {
        auth.resetWithToken(request);
        return new MessageResponse("Password changed. You can sign in now.", null);
    }

    public record ProfileRequest(String name) {}

    @PutMapping("/profile")
    public UserResponse profile(Authentication authentication, @RequestBody ProfileRequest request) {
        return auth.updateProfile(authentication.getName(), request.name());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return auth.response(auth.find(authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO).toString())
            .body(new MessageResponse("Logged out", null));
    }

    private ResponseEntity<UserResponse> signedIn(HttpStatus status, UserAccount user) {
        return ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, sessionCookie(jwt.create(user), SESSION_LENGTH).toString())
            .body(auth.response(user));
    }

    private ResponseCookie sessionCookie(String value, Duration age) {
        return ResponseCookie.from(JwtCookieFilter.COOKIE, value)
            .httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(age).build();
    }
}
