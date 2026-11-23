package com.libraryhub.api.service;
import com.libraryhub.api.dto.*;
import com.libraryhub.api.exception.*;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.List;

@Service
public class LoanService {
    private final LoanRepository loans;
    private final BookRepository books;
    private final MemberRepository members;
    private final ReservationRepository reservations;
    public LoanService(LoanRepository loans,BookRepository books,MemberRepository members,ReservationRepository reservations){this.loans=loans;this.books=books;this.members=members;this.reservations=reservations;}
    public List<LoanResponse> getAll(){
        return loans.findAll(Sort.by(Sort.Direction.DESC,"borrowedOn")).stream().map(this::refreshStatus).map(this::response).toList();
    }
    public LoanResponse getById(String id){return response(refreshStatus(find(id)));}
    public List<LoanResponse> getForMember(String memberId){return loans.findByMemberIdOrderByBorrowedOnDesc(memberId).stream().map(this::refreshStatus).map(this::response).toList();}
    public LoanResponse create(LoanRequest request){
        Book book=books.findById(request.bookId()).orElseThrow(()->new ResourceNotFoundException("Book not found: "+request.bookId()));
        Member member=members.findById(request.memberId()).orElseThrow(()->new ResourceNotFoundException("Member not found: "+request.memberId()));
        if(book.getAvailableCopies()<=0) throw new DuplicateResourceException("Book is not available");
        if(member.getStatus()!=MemberStatus.ACTIVE) throw new DuplicateResourceException("Member is not active");
        Loan loan=new Loan();loan.setBookId(book.getId());loan.setMemberId(member.getId());loan.setBorrowedOn(LocalDate.now());loan.setDueDate(request.dueDate());loan.setStatus(LoanStatus.BORROWED);
        book.setAvailableCopies(book.getAvailableCopies()-1);book.setAvailability(book.getAvailableCopies()>0?Availability.AVAILABLE:Availability.ON_LOAN);books.save(book);
        return response(loans.save(loan));
    }
    public LoanResponse returnBook(String id){
        Loan loan=refreshStatus(find(id));
        if(loan.getStatus()==LoanStatus.RETURNED) throw new DuplicateResourceException("Book has already been returned");
        Book book=books.findById(loan.getBookId()).orElseThrow(()->new ResourceNotFoundException("Book not found for this loan"));
        loan.setStatus(LoanStatus.RETURNED);loan.setReturnedOn(LocalDate.now());book.setAvailableCopies(Math.min(book.getTotalCopies(),book.getAvailableCopies()+1));book.setAvailability(Availability.AVAILABLE);books.save(book);
        reservations.findTopByBookIdAndStatusOrderByCreatedAtAsc(book.getId(),ReservationStatus.WAITING).ifPresent(value->{value.setStatus(ReservationStatus.READY);value.setExpiresAt(java.time.Instant.now().plus(3,java.time.temporal.ChronoUnit.DAYS));reservations.save(value);});
        return response(loans.save(loan));
    }
    public void delete(String id){
        Loan loan=find(id);
        if(loan.getStatus()!=LoanStatus.RETURNED) books.findById(loan.getBookId()).ifPresent(book->{book.setAvailableCopies(Math.min(book.getTotalCopies(),book.getAvailableCopies()+1));book.setAvailability(Availability.AVAILABLE);books.save(book);});
        loans.delete(loan);
    }
    public LoanResponse payFine(String id){Loan loan=refreshStatus(find(id));if(loan.getFineAmount().signum()<=0)throw new IllegalArgumentException("This loan has no outstanding fine");loan.setFinePaid(true);return response(loans.save(loan));}
    private Loan find(String id){return loans.findById(id).orElseThrow(()->new ResourceNotFoundException("Loan not found: "+id));}
    private Loan refreshStatus(Loan loan){LocalDate end=loan.getReturnedOn()==null?LocalDate.now():loan.getReturnedOn();if(loan.getDueDate().isBefore(end)){if(loan.getStatus()!=LoanStatus.RETURNED)loan.setStatus(LoanStatus.OVERDUE);loan.setFineAmount(BigDecimal.valueOf(ChronoUnit.DAYS.between(loan.getDueDate(),end)));return loans.save(loan);}return loan;}
    private LoanResponse response(Loan l){return new LoanResponse(l.getId(),l.getBookId(),l.getMemberId(),l.getBorrowedOn(),l.getDueDate(),l.getReturnedOn(),l.getStatus(),l.getFineAmount(),l.isFinePaid());}
}
