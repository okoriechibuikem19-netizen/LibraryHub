package com.libraryhub.api.service;

import com.libraryhub.api.dto.BookResponse;
import com.libraryhub.api.dto.LoanResponse;
import com.libraryhub.api.dto.Phase2Dtos.*;
import com.libraryhub.api.exception.*;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class Phase2Service {
    private final ReservationRepository reservations;
    private final ReviewRepository reviews;
    private final ReadingProgressRepository progress;
    private final BookRepository books;
    private final BookService bookService;
    private final ExternalNovelRepository externalNovels;
    private final MemberRepository members;
    private final LoanService loanService;
    public Phase2Service(ReservationRepository reservations,ReviewRepository reviews,ReadingProgressRepository progress,BookRepository books,BookService bookService,MemberRepository members,LoanService loanService,ExternalNovelRepository externalNovels){
        this.reservations=reservations;this.reviews=reviews;this.progress=progress;this.books=books;this.bookService=bookService;this.members=members;this.loanService=loanService;this.externalNovels=externalNovels;
    }
    public List<ReservationResponse> reservations(String email){return reservations.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email).stream().map(this::reservation).toList();}
    public ReservationResponse reserve(String email,ReservationRequest request){
        Book book=books.findById(request.bookId()).orElseThrow(()->new ResourceNotFoundException("Book not found"));
        reservations.findByUserEmailIgnoreCaseAndBookIdAndStatusIn(email,book.getId(),List.of(ReservationStatus.WAITING,ReservationStatus.READY))
            .ifPresent(x->{throw new DuplicateResourceException("You already reserved this book");});
        Reservation value=new Reservation();value.setUserEmail(email);value.setBookId(book.getId());value.setCreatedAt(Instant.now());
        value.setStatus(book.getAvailableCopies()>0?ReservationStatus.READY:ReservationStatus.WAITING);
        if(value.getStatus()==ReservationStatus.READY)value.setExpiresAt(Instant.now().plus(3,ChronoUnit.DAYS));
        return reservation(reservations.save(value));
    }
    public void cancelReservation(String email,String id){Reservation value=reservations.findById(id).orElseThrow(()->new ResourceNotFoundException("Reservation not found"));if(!value.getUserEmail().equalsIgnoreCase(email))throw new IllegalArgumentException("You cannot cancel this reservation");value.setStatus(ReservationStatus.CANCELLED);reservations.save(value);}
    public List<ReviewResponse> reviewsForBook(String bookId){return reviews.findByBookIdOrderByCreatedAtDesc(bookId).stream().map(this::review).toList();}
    public List<ReviewResponse> myReviews(String email){return reviews.findByUserEmailIgnoreCase(email).stream().map(this::review).toList();}
    public ReviewResponse saveReview(String email,ReviewRequest request){
        if(books.findById(request.bookId()).isEmpty()&&externalNovels.findById(request.bookId()).isEmpty())throw new ResourceNotFoundException("Book not found");
        Review value=reviews.findByUserEmailIgnoreCaseAndBookId(email,request.bookId()).orElseGet(Review::new);value.setUserEmail(email);value.setBookId(request.bookId());
        value.setRating(request.rating());value.setComment(request.comment()==null?"":request.comment().trim());value.setCreatedAt(Instant.now());return review(reviews.save(value));
    }
    public List<ProgressResponse> progress(String email){return progress.findByUserEmailIgnoreCaseOrderByUpdatedAtDesc(email).stream().map(this::progress).toList();}
    public ProgressResponse saveProgress(String email,ProgressRequest request){
        books.findById(request.bookId()).orElseThrow(()->new ResourceNotFoundException("Book not found"));
        ReadingProgress value=progress.findByUserEmailIgnoreCaseAndBookId(email,request.bookId()).orElseGet(ReadingProgress::new);
        value.setUserEmail(email);value.setBookId(request.bookId());value.setPercentage(request.percentage());value.setCurrentPage(request.currentPage());value.setUpdatedAt(Instant.now());return progress(progress.save(value));
    }
    public List<BookResponse> recommendations(String email){
        Set<String> likedCategories=new LinkedHashSet<>();
        for(Review review:reviews.findByUserEmailIgnoreCase(email))if(review.getRating()>=4)books.findById(review.getBookId()).ifPresent(b->likedCategories.add(b.getCategory()));
        return bookService.getAll(null,null,Availability.AVAILABLE).stream().filter(b->likedCategories.isEmpty()||likedCategories.contains(b.category())).limit(8).toList();
    }
    public List<LoanResponse> myLoans(String email){Member member=members.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("Member profile not found"));return loanService.getForMember(member.getId());}
    private ReservationResponse reservation(Reservation v){return new ReservationResponse(v.getId(),v.getBookId(),v.getUserEmail(),v.getStatus(),v.getCreatedAt(),v.getExpiresAt());}
    private ReviewResponse review(Review v){return new ReviewResponse(v.getId(),v.getBookId(),v.getUserEmail(),v.getRating(),v.getComment(),v.getCreatedAt());}
    private ProgressResponse progress(ReadingProgress v){return new ProgressResponse(v.getId(),v.getBookId(),v.getPercentage(),v.getCurrentPage(),v.getUpdatedAt());}
}
