package com.libraryhub.api.repository;
import com.libraryhub.api.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface ReviewRepository extends MongoRepository<Review,String>{
    List<Review> findByBookIdOrderByCreatedAtDesc(String bookId);
    List<Review> findByUserEmailIgnoreCase(String email);
    Optional<Review> findByUserEmailIgnoreCaseAndBookId(String email,String bookId);
}
