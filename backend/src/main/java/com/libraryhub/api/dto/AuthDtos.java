package com.libraryhub.api.dto;

import com.libraryhub.api.model.UserRole;
import com.libraryhub.api.model.UserStatus;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos(){}
    public record RegisterRequest(@NotBlank @Size(max=120) String name,@NotBlank @Email String email,@NotBlank @Size(min=8,max=72) String password){}
    public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
    public record VerifyOtpRequest(@NotBlank @Email String email,@NotBlank @Pattern(regexp="\\d{6}") String code,@NotBlank String purpose){}
    public record ResendOtpRequest(@NotBlank @Email String email,@NotBlank String purpose){}
    public record ForgotPasswordRequest(@NotBlank @Email String email){}
    public record ResetPasswordRequest(@NotBlank @Email String email,@NotBlank @Pattern(regexp="\\d{6}") String code,
                                       @NotBlank @Size(min=8,max=72) String newPassword){}
    public record MessageResponse(String message,String developmentOtp){}
    public record UserResponse(String id,String name,String email,UserRole role,UserStatus status,boolean emailVerified,boolean voiceEnabled){}
    public record ChangePasswordRequest(@NotBlank String currentPassword,@NotBlank @Size(min=8,max=72) String newPassword){}
    public record ResetWithTokenRequest(@NotBlank String token,@NotBlank @Size(min=8,max=72) String newPassword){}
    public record ResetLinkResponse(String token,java.time.Instant expiresAt){}
    public record StaffRequest(@NotBlank @Size(max=120) String name,@NotBlank @Email String email,@NotBlank @Size(min=8,max=72) String password,@NotNull UserRole role){}
}
