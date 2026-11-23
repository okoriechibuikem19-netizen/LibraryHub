package com.libraryhub.api.config;

import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;

@Configuration
public class AdminSeeder {
    @Bean CommandLineRunner seedAdmin(UserAccountRepository users,PasswordEncoder encoder,
        @Value("${app.bootstrap.admin-name}") String name,@Value("${app.bootstrap.admin-email:}") String email,
        @Value("${app.bootstrap.admin-password:}") String password){
        return args->{if(email.isBlank()||password.isBlank()||users.findByEmailIgnoreCase(email).isPresent())return;
            UserAccount user=new UserAccount();user.setName(name);user.setEmail(email.trim().toLowerCase());user.setPasswordHash(encoder.encode(password));
            user.setRole(UserRole.ADMIN);user.setStatus(UserStatus.ACTIVE);user.setEmailVerified(true);user.setCreatedAt(Instant.now());users.save(user);
        };
    }
}
