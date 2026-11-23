package com.libraryhub.api.repository;
import com.libraryhub.api.model.Loan;
import com.libraryhub.api.model.LoanStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Collection;
import java.util.List;
public interface LoanRepository extends MongoRepository<Loan,String> {
    List<Loan> findByMemberIdAndStatusIn(String memberId, Collection<LoanStatus> statuses);
    List<Loan> findByBookIdAndStatusIn(String bookId, Collection<LoanStatus> statuses);
    List<Loan> findByMemberIdOrderByBorrowedOnDesc(String memberId);
}
