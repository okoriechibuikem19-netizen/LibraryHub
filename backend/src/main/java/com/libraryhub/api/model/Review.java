package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection="reviews")
@CompoundIndex(name="one_review_per_user_book",def="{'userEmail':1,'bookId':1}",unique=true)
public class Review {
    @Id private String id;
    @Indexed private String bookId;
    private String userEmail;
    private int rating;
    private String comment;
    private Instant createdAt;
    private boolean reported;
    public boolean isReported(){return reported;} public void setReported(boolean reported){this.reported=reported;}
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getBookId(){return bookId;} public void setBookId(String bookId){this.bookId=bookId;}
    public String getUserEmail(){return userEmail;} public void setUserEmail(String userEmail){this.userEmail=userEmail;}
    public int getRating(){return rating;} public void setRating(int rating){this.rating=rating;}
    public String getComment(){return comment;} public void setComment(String comment){this.comment=comment;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}
