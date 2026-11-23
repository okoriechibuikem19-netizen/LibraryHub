package com.libraryhub.api.service;

import com.libraryhub.api.dto.AuthDtos.*;
import com.libraryhub.api.exception.*;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Accounts: register, sign in with email + password, profile, staff accounts.
 * No email codes: the controller sets the login cookie as soon as the password is correct.
 */
@Service
public class AuthService {
    private static final String WRONG_LOGIN = "Incorrect email or password";
    private static final String RESET_PURPOSE = "RESET_LINK";
    private static final Duration RESET_LINK_LIFETIME = Duration.ofHours(24);

    private final UserAccountRepository users;
    private final MemberRepository members;
    private final OtpCodeRepository tokens;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserAccountRepository users, MemberRepository members, OtpCodeRepository tokens, PasswordEncoder encoder) {
        this.users = users;
        this.members = members;
        this.tokens = tokens;
        this.encoder = encoder;
    }

    /** Creates an active member account and its library member record. */
    public UserAccount register(RegisterRequest request) {
        String address = normalize(request.email());
        if (users.findByEmailIgnoreCase(address).isPresent())
            throw new DuplicateResourceException("An account already exists for this email");
        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setEmail(address);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setCreatedAt(Instant.now());
        user.setLastLoginAt(Instant.now());
        users.save(user);
        ensureMemberRecord(user);
        return user;
    }

    /** Checks email + password. The same message is used for "no account" and "wrong password". */
    public UserAccount login(LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(normalize(request.email()))
            .orElseThrow(() -> new IllegalArgumentException(WRONG_LOGIN));
        if (!encoder.matches(request.password(), user.getPasswordHash()))
            throw new IllegalArgumentException(WRONG_LOGIN);
        if (user.getStatus() == UserStatus.SUSPENDED)
            throw new IllegalArgumentException("This account is suspended. Please contact the library.");
        if (user.getStatus() == UserStatus.PENDING) {
            // Accounts created under the old email-code system: activate them now.
            user.setStatus(UserStatus.ACTIVE);
            if (user.getRole() == UserRole.MEMBER) ensureMemberRecord(user);
        }
        user.setLastLoginAt(Instant.now());
        return users.save(user);
    }

    /** Admin sets a new password for someone who forgot theirs. */
    public UserResponse adminResetPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72)
            throw new IllegalArgumentException("Password must be 8 to 72 characters");
        UserAccount user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        user.setPasswordHash(encoder.encode(newPassword));
        return response(users.save(user));
    }

    /** A signed-in user changes their own password. */
    public void changePassword(String email, ChangePasswordRequest request) {
        UserAccount user = find(email);
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash()))
            throw new IllegalArgumentException("Your current password is incorrect");
        user.setPasswordHash(encoder.encode(request.newPassword()));
        users.save(user);
    }

    /**
     * Admin creates a one-time reset link for someone who forgot their password.
     * Only a hash of the token is stored, so a database leak can't be used to reset passwords.
     */
    public ResetLinkResponse createResetLink(String userId) {
        UserAccount user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        tokens.deleteByEmailIgnoreCaseAndPurpose(user.getEmail(), RESET_PURPOSE);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        OtpCode record = new OtpCode();
        record.setEmail(user.getEmail());
        record.setPurpose(RESET_PURPOSE);
        record.setCodeHash(sha256(token));
        record.setExpiresAt(Instant.now().plus(RESET_LINK_LIFETIME));
        tokens.save(record);
        return new ResetLinkResponse(token, record.getExpiresAt());
    }

    /** Anyone holding a valid reset link sets a new password. The link works once. */
    public void resetWithToken(ResetWithTokenRequest request) {
        OtpCode record = tokens.findByCodeHashAndPurpose(sha256(request.token().trim()), RESET_PURPOSE)
            .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has already been used"));
        tokens.delete(record);
        if (record.getExpiresAt().isBefore(Instant.now()))
            throw new IllegalArgumentException("This reset link has expired. Ask a librarian for a new one.");
        UserAccount user = find(record.getEmail());
        user.setPasswordHash(encoder.encode(request.newPassword()));
        if (user.getStatus() == UserStatus.PENDING) user.setStatus(UserStatus.ACTIVE);
        users.save(user);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public UserAccount find(String email) {
        return users.findByEmailIgnoreCase(normalize(email)).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    public UserResponse updateProfile(String email, String name) {
        if (name == null || name.isBlank() || name.length() > 120) throw new IllegalArgumentException("Enter a valid name");
        UserAccount user = find(email);
        user.setName(name.trim());
        members.findByEmailIgnoreCase(user.getEmail()).ifPresent(m -> { m.setName(user.getName()); members.save(m); });
        return response(users.save(user));
    }

    public UserResponse response(UserAccount user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(), user.isEmailVerified(), user.isVoiceEnabled());
    }

    public UserResponse createStaff(StaffRequest request) {
        if (request.role() == UserRole.MEMBER) throw new IllegalArgumentException("Use registration to create member accounts");
        String address = normalize(request.email());
        if (users.findByEmailIgnoreCase(address).isPresent()) throw new DuplicateResourceException("Email already exists");
        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setEmail(address);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.now());
        return response(users.save(user));
    }

    private void ensureMemberRecord(UserAccount user) {
        if (members.findByEmailIgnoreCase(user.getEmail()).isPresent()) return;
        Member member = new Member();
        member.setMemberId("M" + System.currentTimeMillis());
        member.setName(user.getName());
        member.setEmail(user.getEmail());
        member.setPhone("Not provided");
        member.setJoined(LocalDate.now());
        member.setStatus(MemberStatus.ACTIVE);
        members.save(member);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }
}
