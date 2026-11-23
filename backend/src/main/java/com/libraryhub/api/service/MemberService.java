package com.libraryhub.api.service;
import com.libraryhub.api.dto.*;
import com.libraryhub.api.exception.*;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MemberService {
    private final MemberRepository members;
    private final LoanRepository loans;
    public MemberService(MemberRepository members,LoanRepository loans){this.members=members;this.loans=loans;}
    public List<MemberResponse> getAll(){return members.findAll(Sort.by("name")).stream().map(this::response).toList();}
    public MemberResponse getById(String id){return response(find(id));}
    public MemberResponse create(MemberRequest request){unique(request.memberId(),request.email(),null);return response(members.save(apply(new Member(),request)));}
    public MemberResponse update(String id,MemberRequest request){Member member=find(id);unique(request.memberId(),request.email(),id);return response(members.save(apply(member,request)));}
    public void delete(String id){
        Member member=find(id);
        if(!loans.findByMemberIdAndStatusIn(id,List.of(LoanStatus.BORROWED,LoanStatus.OVERDUE)).isEmpty())
            throw new DuplicateResourceException("Return this member's active loans before deleting the member");
        members.delete(member);
    }
    private Member find(String id){return members.findById(id).orElseThrow(()->new ResourceNotFoundException("Member not found: "+id));}
    private void unique(String memberId,String email,String current){
        members.findByMemberIdIgnoreCase(memberId).filter(x->!x.getId().equals(current)).ifPresent(x->{throw new DuplicateResourceException("Member ID already exists: "+memberId);});
        members.findByEmailIgnoreCase(email).filter(x->!x.getId().equals(current)).ifPresent(x->{throw new DuplicateResourceException("Email already exists: "+email);});
    }
    private Member apply(Member m,MemberRequest r){m.setMemberId(r.memberId().trim());m.setName(r.name().trim());m.setEmail(r.email().trim().toLowerCase());m.setPhone(r.phone().trim());m.setJoined(r.joined());m.setStatus(r.status());return m;}
    private MemberResponse response(Member m){return new MemberResponse(m.getId(),m.getMemberId(),m.getName(),m.getEmail(),m.getPhone(),m.getJoined(),m.getStatus());}
}
