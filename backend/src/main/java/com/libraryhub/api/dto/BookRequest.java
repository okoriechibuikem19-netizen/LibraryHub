package com.libraryhub.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.libraryhub.api.model.Availability;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookRequest(
    @NotBlank @Size(max=30) String bookId,
    @NotBlank @Size(max=200) String title,
    @NotBlank @Size(max=120) String author,
    @NotBlank @Size(max=80) String category,
    @NotBlank @Size(max=30) String isbn,
    @NotBlank @Size(max=120) String publisher,
    @NotNull @Min(1000) @Max(2100) Integer yearPublished,
    @NotNull @DecimalMin(value="0.0", inclusive=true) BigDecimal price,
    @NotNull Availability availability,
    @NotBlank String contentType,
    @NotBlank @Pattern(regexp="https?://.*", message="must be a valid HTTP or HTTPS URL") String sourceUrl,
    @NotBlank String license,
    @Pattern(regexp="^$|https?://.*", message="must be a valid HTTP or HTTPS URL") String coverUrl,
    @Size(max=80) String barcode,
    @Min(1) Integer totalCopies
) {}
