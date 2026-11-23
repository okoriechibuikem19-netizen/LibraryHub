package com.libraryhub.api.service;

import com.libraryhub.api.dto.AuthDtos.*;
import com.libraryhub.api.exception.*;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Accounts: register, sign in with email + password, profile, staff accounts.
 * No email codes: the controller sets the login cookie as soon as the password is correct.
 */
@Service
public class AuthService {
    private static final String WRONG_LOGIN = "Incorrect email or password";

    private final UserAccountRepository users;
    private final MemberRepository members;
    private final PasswordEncoder encoder;

    public AuthService(UserAccountRepository users, MemberRepository members, PasswordEncoder encoder) {
        this.users = users;
        this.members = members;
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
