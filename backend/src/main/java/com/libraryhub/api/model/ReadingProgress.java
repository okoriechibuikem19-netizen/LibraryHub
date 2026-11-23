package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection="reading_progress")
@CompoundIndex(name="one_progress_per_user_book",def="{'userEmail':1,'bookId':1}",unique=true)
public class ReadingProgress {
    @Id private String id;
    private String userEmail;
    private String bookId;
    private int percentage;
    private int currentPage;
    private Instant updatedAt;
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getUserEmail(){return userEmail;} public void setUserEmail(String userEmail){this.userEmail=userEmail;}
    public String getBookId(){return bookId;} public void setBookId(String bookId){this.bookId=bookId;}
    public int getPercentage(){return percentage;} public void setPercentage(int percentage){this.percentage=percentage;}
    public int getCurrentPage(){return currentPage;} public void setCurrentPage(int currentPage){this.currentPage=currentPage;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant updatedAt){this.updatedAt=updatedAt;}
}
