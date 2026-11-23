package com.libraryhub.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection="users")
public class UserAccount {
    @Id private String id;
    private String name;
    @Indexed(unique=true) private String email;
    private String passwordHash;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private boolean voiceEnabled=true;
    private Instant createdAt;
    private Instant lastLoginAt;
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;}
    public UserRole getRole(){return role;} public void setRole(UserRole role){this.role=role;}
    public UserStatus getStatus(){return status;} public void setStatus(UserStatus status){this.status=status;}
    public boolean isEmailVerified(){return emailVerified;} public void setEmailVerified(boolean emailVerified){this.emailVerified=emailVerified;}
    public boolean isVoiceEnabled(){return voiceEnabled;} public void setVoiceEnabled(boolean voiceEnabled){this.voiceEnabled=voiceEnabled;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getLastLoginAt(){return lastLoginAt;} public void setLastLoginAt(Instant lastLoginAt){this.lastLoginAt=lastLoginAt;}
}
