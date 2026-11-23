package com.libraryhub.api.dto;

import com.libraryhub.api.model.Availability;
import java.math.BigDecimal;

public record BookResponse(
    String id, String bookId, String title, String author, String category, String isbn,
    String publisher, Integer yearPublished, BigDecimal price, Availability availability,
    String contentType, String sourceUrl, String license,String coverUrl,String barcode,Integer totalCopies,Integer availableCopies
) {}
