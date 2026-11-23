package com.libraryhub.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;

@Document(collection = "books")
public class Book {
    @Id private String id;
    @Indexed(unique = true) private String bookId;
    private String title;
    private String author;
    private String category;
    @Indexed(unique = true) private String isbn;
    private String publisher;
    private Integer yearPublished;
    private BigDecimal price;
    private Availability availability;
    private String contentType;
    private String sourceUrl;
    private String license;
    private String coverUrl;
    private String barcode;
    private Integer totalCopies=1;
    private Integer availableCopies=1;

    public Book() {}
    public Book(String id, String bookId, String title, String author, String category, String isbn,
                String publisher, Integer yearPublished, BigDecimal price, Availability availability) {
        this.id=id; this.bookId=bookId; this.title=title; this.author=author; this.category=category;
        this.isbn=isbn; this.publisher=publisher; this.yearPublished=yearPublished; this.price=price;
        this.availability=availability;
        this.barcode=bookId;this.totalCopies=1;this.availableCopies=availability==Availability.AVAILABLE?1:0;
    }
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getBookId(){return bookId;} public void setBookId(String bookId){this.bookId=bookId;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getAuthor(){return author;} public void setAuthor(String author){this.author=author;}
    public String getCategory(){return category;} public void setCategory(String category){this.category=category;}
    public String getIsbn(){return isbn;} public void setIsbn(String isbn){this.isbn=isbn;}
    public String getPublisher(){return publisher;} public void setPublisher(String publisher){this.publisher=publisher;}
    public Integer getYearPublished(){return yearPublished;} public void setYearPublished(Integer yearPublished){this.yearPublished=yearPublished;}
    public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal price){this.price=price;}
    public Availability getAvailability(){return availability;} public void setAvailability(Availability availability){this.availability=availability;}
    public String getContentType(){return contentType;} public void setContentType(String contentType){this.contentType=contentType;}
    public String getSourceUrl(){return sourceUrl;} public void setSourceUrl(String sourceUrl){this.sourceUrl=sourceUrl;}
    public String getLicense(){return license;} public void setLicense(String license){this.license=license;}
    public String getCoverUrl(){return coverUrl;} public void setCoverUrl(String coverUrl){this.coverUrl=coverUrl;}
    public String getBarcode(){return barcode;} public void setBarcode(String barcode){this.barcode=barcode;}
    public Integer getTotalCopies(){return totalCopies==null?1:totalCopies;} public void setTotalCopies(Integer totalCopies){this.totalCopies=totalCopies;}
    public Integer getAvailableCopies(){return availableCopies==null?(availability==Availability.AVAILABLE?1:0):availableCopies;} public void setAvailableCopies(Integer availableCopies){this.availableCopies=availableCopies;}
}
