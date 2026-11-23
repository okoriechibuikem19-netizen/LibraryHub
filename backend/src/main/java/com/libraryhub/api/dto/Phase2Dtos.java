package com.libraryhub.api.dto;
import com.libraryhub.api.model.ReservationStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Phase2Dtos {
    private Phase2Dtos(){}
    public record ReservationRequest(@NotBlank String bookId){}
    public record ReservationResponse(String id,String bookId,String userEmail,ReservationStatus status,Instant createdAt,Instant expiresAt){}
    public record ReviewRequest(@NotBlank String bookId,@Min(1) @Max(5) int rating,@Size(max=1000) String comment){}
    public record ReviewResponse(String id,String bookId,String userEmail,int rating,String comment,Instant createdAt){}
    public record ProgressRequest(@NotBlank String bookId,@Min(0) @Max(100) int percentage,@Min(0) int currentPage){}
    public record ProgressResponse(String id,String bookId,int percentage,int currentPage,Instant updatedAt){}
}
