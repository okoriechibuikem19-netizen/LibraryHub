package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.math.BigDecimal;

@Document(collection="loans")
public class Loan {
    @Id private String id;
    @Indexed private String bookId;
    @Indexed private String memberId;
    private LocalDate borrowedOn;
    private LocalDate dueDate;
    private LocalDate returnedOn;
    private LoanStatus status;
    private BigDecimal fineAmount=BigDecimal.ZERO;
    private boolean finePaid;
    public Loan(){}
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getBookId(){return bookId;} public void setBookId(String bookId){this.bookId=bookId;}
    public String getMemberId(){return memberId;} public void setMemberId(String memberId){this.memberId=memberId;}
    public LocalDate getBorrowedOn(){return borrowedOn;} public void setBorrowedOn(LocalDate borrowedOn){this.borrowedOn=borrowedOn;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate dueDate){this.dueDate=dueDate;}
    public LocalDate getReturnedOn(){return returnedOn;} public void setReturnedOn(LocalDate returnedOn){this.returnedOn=returnedOn;}
    public LoanStatus getStatus(){return status;} public void setStatus(LoanStatus status){this.status=status;}
    public BigDecimal getFineAmount(){return fineAmount==null?BigDecimal.ZERO:fineAmount;} public void setFineAmount(BigDecimal fineAmount){this.fineAmount=fineAmount;}
    public boolean isFinePaid(){return finePaid;} public void setFinePaid(boolean finePaid){this.finePaid=finePaid;}
}
