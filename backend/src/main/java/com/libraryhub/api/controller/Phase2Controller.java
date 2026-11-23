package com.libraryhub.api.controller;

import com.libraryhub.api.dto.BookResponse;
import com.libraryhub.api.dto.LoanResponse;
import com.libraryhub.api.dto.Phase2Dtos.*;
import com.libraryhub.api.service.Phase2Service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/my-library")
public class Phase2Controller {
    private final Phase2Service service;
    public Phase2Controller(Phase2Service service){this.service=service;}
    @GetMapping("/reservations") public List<ReservationResponse> reservations(Authentication auth){return service.reservations(auth.getName());}
    @PostMapping("/reservations") @ResponseStatus(HttpStatus.CREATED) public ReservationResponse reserve(Authentication auth,@Valid @RequestBody ReservationRequest request){return service.reserve(auth.getName(),request);}
    @DeleteMapping("/reservations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void cancel(Authentication auth,@PathVariable String id){service.cancelReservation(auth.getName(),id);}
    @GetMapping("/reviews") public List<ReviewResponse> myReviews(Authentication auth){return service.myReviews(auth.getName());}
    @GetMapping("/reviews/book/{bookId}") public List<ReviewResponse> reviews(@PathVariable String bookId){return service.reviewsForBook(bookId);}
    @PostMapping("/reviews") public ReviewResponse review(Authentication auth,@Valid @RequestBody ReviewRequest request){return service.saveReview(auth.getName(),request);}
    @GetMapping("/progress") public List<ProgressResponse> progress(Authentication auth){return service.progress(auth.getName());}
    @PutMapping("/progress") public ProgressResponse progress(Authentication auth,@Valid @RequestBody ProgressRequest request){return service.saveProgress(auth.getName(),request);}
    @GetMapping("/recommendations") public List<BookResponse> recommendations(Authentication auth){return service.recommendations(auth.getName());}
    @GetMapping("/loans") public List<LoanResponse> loans(Authentication auth){return service.myLoans(auth.getName());}
}
