package com.libraryhub.api.controller;

import com.libraryhub.api.dto.AuthDtos.*;
import com.libraryhub.api.model.UserAccount;
import com.libraryhub.api.security.*;
import com.libraryhub.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final JwtService jwt;
    private final boolean secure;
    public AuthController(AuthService auth,JwtService jwt,@Value("${app.auth.cookie-secure:false}") boolean secure){this.auth=auth;this.jwt=jwt;this.secure=secure;}
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public MessageResponse register(@Valid @RequestBody RegisterRequest request){return auth.register(request);}
    @PostMapping("/login") public MessageResponse login(@Valid @RequestBody LoginRequest request){return auth.login(request);}
    @PostMapping("/resend-otp") public MessageResponse resend(@Valid @RequestBody ResendOtpRequest request){return auth.resend(request);}
    @PostMapping("/forgot-password") public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest request){return auth.forgot(request);}
    @PostMapping("/reset-password") public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request){auth.reset(request);return new MessageResponse("Password reset successfully",null);}
    @PostMapping("/verify-otp") public ResponseEntity<UserResponse> verify(@Valid @RequestBody VerifyOtpRequest request){
        UserAccount user=auth.verify(request);ResponseCookie cookie=sessionCookie(jwt.create(user),Duration.ofHours(8));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie.toString()).body(auth.response(user));
    }
    public record ProfileRequest(String name){}
    @PutMapping("/profile") public UserResponse profile(Authentication authentication,@RequestBody ProfileRequest request){return auth.updateProfile(authentication.getName(),request.name());}
    @GetMapping("/me") public UserResponse me(Authentication authentication){return auth.response(auth.find(authentication.getName()));}
    @PostMapping("/logout") public ResponseEntity<MessageResponse> logout(){
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,sessionCookie("",Duration.ZERO).toString()).body(new MessageResponse("Logged out",null));
    }
    private ResponseCookie sessionCookie(String value,Duration age){return ResponseCookie.from(JwtCookieFilter.COOKIE,value).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(age).build();}
}
