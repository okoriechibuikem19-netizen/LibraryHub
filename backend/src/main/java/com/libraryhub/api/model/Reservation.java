package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection="reservations")
@CompoundIndex(name="user_book_active",def="{'userEmail':1,'bookId':1,'status':1}")
public class Reservation {
    @Id private String id;
    @Indexed private String userEmail;
    @Indexed private String bookId;
    private ReservationStatus status;
    private Instant createdAt;
    private Instant expiresAt;
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getUserEmail(){return userEmail;} public void setUserEmail(String userEmail){this.userEmail=userEmail;}
    public String getBookId(){return bookId;} public void setBookId(String bookId){this.bookId=bookId;}
    public ReservationStatus getStatus(){return status;} public void setStatus(ReservationStatus status){this.status=status;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
    public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant expiresAt){this.expiresAt=expiresAt;}
}
