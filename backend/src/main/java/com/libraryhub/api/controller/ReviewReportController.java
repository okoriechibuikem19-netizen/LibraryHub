package com.libraryhub.api.controller;
import com.libraryhub.api.repository.ReviewRepository;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reader/reviews")
public class ReviewReportController {
 private final ReviewRepository reviews;
 public ReviewReportController(ReviewRepository reviews){this.reviews=reviews;}
 @PostMapping("/{id}/report") public void report(@PathVariable String id){var review=reviews.findById(id).orElseThrow();review.setReported(true);reviews.save(review);}
}
