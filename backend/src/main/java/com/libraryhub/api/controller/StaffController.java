package com.libraryhub.api.controller;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import com.libraryhub.api.service.EmailService;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
@RestController @RequestMapping("/api/staff")
public class StaffController {
 private final ReservationRepository reservations;private final ReviewRepository reviews;private final EmailService email;
 public StaffController(ReservationRepository reservations,ReviewRepository reviews,EmailService email){this.reservations=reservations;this.reviews=reviews;this.email=email;}
 @GetMapping("/reservations") public List<Reservation> reservations(){return reservations.findAll();}
 @PutMapping("/reservations/{id}/ready") public Reservation ready(@PathVariable String id){Reservation r=reservations.findById(id).orElseThrow();r.setStatus(ReservationStatus.READY);r.setExpiresAt(Instant.now().plus(3,ChronoUnit.DAYS));r=reservations.save(r);email.sendNotice(r.getUserEmail(),"Your LibraryHub reservation is ready","Your reserved book is ready. Sign in to LibraryHub to see the details.");return r;}
 @GetMapping("/reviews") public List<Review> reviews(){return reviews.findAll();}
 @DeleteMapping("/reviews/{id}") public void remove(@PathVariable String id){reviews.deleteById(id);}
}
