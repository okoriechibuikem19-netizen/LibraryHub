package com.libraryhub.api.dto;
import com.libraryhub.api.model.MemberStatus;
import java.time.LocalDate;
public record MemberResponse(String id,String memberId,String name,String email,String phone,LocalDate joined,MemberStatus status) {}
