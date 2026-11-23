package com.libraryhub.api.config;

import com.libraryhub.api.security.JwtCookieFilter;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean SecurityFilterChain security(HttpSecurity http,JwtCookieFilter filter)throws Exception{
        return http.csrf(csrf->csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())).cors(cors->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers("/api/auth/csrf","/api/auth/register","/api/auth/login","/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/books/**").authenticated()
                .requestMatchers("/api/catalog/**","/api/reader/**").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/books/**").hasAnyRole("ADMIN","LIBRARIAN")
                .requestMatchers(HttpMethod.PUT,"/api/books/**").hasAnyRole("ADMIN","LIBRARIAN")
                .requestMatchers(HttpMethod.DELETE,"/api/books/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/staff/**").hasAnyRole("ADMIN","LIBRARIAN")
                .requestMatchers("/api/members/**","/api/loans/**").hasAnyRole("ADMIN","LIBRARIAN")
                .anyRequest().authenticated())
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }
}
