package com.libraryhub.api.security;

import com.libraryhub.api.model.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {
    private final byte[] secret;
    private final long sessionHours;
    public JwtService(@Value("${app.auth.jwt-secret}") String secret,@Value("${app.auth.session-hours:8}") long sessionHours){
        this.secret=secret.getBytes(StandardCharsets.UTF_8);this.sessionHours=sessionHours;
    }
    public String create(UserAccount user){
        long expires=Instant.now().plusSeconds(sessionHours*3600).getEpochSecond();
        String payload=encode(user.getId()+"|"+user.getEmail()+"|"+user.getRole().name()+"|"+expires);
        return payload+"."+sign(payload);
    }
    public Session parse(String token){
        try{
            if(token==null||!token.contains("."))return null;
            String[] parts=token.split("\\.",2);
            if(!constantEquals(sign(parts[0]),parts[1]))return null;
            String[] values=new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8).split("\\|",4);
            if(values.length!=4||Long.parseLong(values[3])<Instant.now().getEpochSecond())return null;
            return new Session(values[0],values[1],values[2]);
        }catch(Exception ignored){return null;}
    }
    private String sign(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String encode(String value){return encode(value.getBytes(StandardCharsets.UTF_8));}
    private String encode(byte[] value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    private boolean constantEquals(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    public record Session(String userId,String email,String role){}
}
