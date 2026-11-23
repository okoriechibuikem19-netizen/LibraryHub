package com.libraryhub.api.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown=true)
public record LoanRequest(
    @NotBlank String bookId,
    @NotBlank String memberId,
    @NotNull @FutureOrPresent LocalDate dueDate
) {}
