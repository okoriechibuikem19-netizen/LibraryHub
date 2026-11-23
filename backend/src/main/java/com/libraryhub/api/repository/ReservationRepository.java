package com.libraryhub.api.repository;
import com.libraryhub.api.model.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface ReservationRepository extends MongoRepository<Reservation,String>{
    List<Reservation> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    Optional<Reservation> findByUserEmailIgnoreCaseAndBookIdAndStatusIn(String email,String bookId,Collection<ReservationStatus> statuses);
    Optional<Reservation> findTopByBookIdAndStatusOrderByCreatedAtAsc(String bookId,ReservationStatus status);
}
