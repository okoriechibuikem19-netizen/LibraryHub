package com.libraryhub.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection="otp_codes")
public class OtpCode {
    @Id private String id;
    @Indexed private String email;
    private String codeHash;
    private String purpose;
    @Indexed(expireAfter="0s") private Instant expiresAt;
    private int attempts;
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getCodeHash(){return codeHash;} public void setCodeHash(String codeHash){this.codeHash=codeHash;}
    public String getPurpose(){return purpose;} public void setPurpose(String purpose){this.purpose=purpose;}
    public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant expiresAt){this.expiresAt=expiresAt;}
    public int getAttempts(){return attempts;} public void setAttempts(int attempts){this.attempts=attempts;}
}
