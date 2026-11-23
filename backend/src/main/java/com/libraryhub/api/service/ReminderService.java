package com.libraryhub.api.service;
import com.libraryhub.api.model.LoanStatus;
import com.libraryhub.api.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.temporal.ChronoUnit;
@Service public class ReminderService {
 private final LoanRepository loans;private final MemberRepository members;private final BookRepository books;private final EmailService email;
 public ReminderService(LoanRepository loans,MemberRepository members,BookRepository books,EmailService email){this.loans=loans;this.members=members;this.books=books;this.email=email;}
 @Scheduled(cron="0 0 9 * * *",zone="UTC") public void dueSoon(){
  LocalDate today=LocalDate.now(ZoneOffset.UTC);
  for(var loan:loans.findAll()){
   if(loan.getStatus()==LoanStatus.RETURNED||loan.getDueDate()==null)continue;
   long days=ChronoUnit.DAYS.between(today,loan.getDueDate());if(days!=3&&days!=1&&days!=0)continue;
   var member=members.findById(loan.getMemberId());var book=books.findById(loan.getBookId());
   if(member.isPresent()&&book.isPresent())email.sendNotice(member.get().getEmail(),"LibraryHub loan reminder",
    "Hello "+member.get().getName()+", your loan for "+book.get().getTitle()+" is due on "+loan.getDueDate()+". Open LibraryHub to review your loan.");
  }
 }
}
