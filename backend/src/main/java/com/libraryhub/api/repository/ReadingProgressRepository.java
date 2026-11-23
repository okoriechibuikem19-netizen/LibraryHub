package com.libraryhub.api.repository;
import com.libraryhub.api.model.ReadingProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface ReadingProgressRepository extends MongoRepository<ReadingProgress,String>{
    List<ReadingProgress> findByUserEmailIgnoreCaseOrderByUpdatedAtDesc(String email);
    Optional<ReadingProgress> findByUserEmailIgnoreCaseAndBookId(String email,String bookId);
}
