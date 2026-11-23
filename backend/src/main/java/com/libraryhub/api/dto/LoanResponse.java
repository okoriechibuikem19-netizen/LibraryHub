package com.libraryhub.api.dto;
import com.libraryhub.api.model.LoanStatus;
import java.time.LocalDate;
import java.math.BigDecimal;
public record LoanResponse(String id,String bookId,String memberId,LocalDate borrowedOn,LocalDate dueDate,LocalDate returnedOn,LoanStatus status,BigDecimal fineAmount,boolean finePaid) {}
