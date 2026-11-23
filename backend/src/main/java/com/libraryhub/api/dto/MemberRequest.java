package com.libraryhub.api.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.libraryhub.api.model.MemberStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown=true)
public record MemberRequest(
    @NotBlank @Size(max=30) String memberId,
    @NotBlank @Size(max=120) String name,
    @NotBlank @Email String email,
    @NotBlank @Size(max=30) String phone,
    @NotNull @PastOrPresent LocalDate joined,
    @NotNull MemberStatus status
) {}
