package com.libraryhub.api.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection="members")
public class Member {
    @Id private String id;
    @Indexed(unique=true) private String memberId;
    private String name;
    @Indexed(unique=true) private String email;
    private String phone;
    private LocalDate joined;
    private MemberStatus status;
    public Member(){}
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getMemberId(){return memberId;} public void setMemberId(String memberId){this.memberId=memberId;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getEmail(){return email;} public void setEmail(String email){this.email=email;}
    public String getPhone(){return phone;} public void setPhone(String phone){this.phone=phone;}
    public LocalDate getJoined(){return joined;} public void setJoined(LocalDate joined){this.joined=joined;}
    public MemberStatus getStatus(){return status;} public void setStatus(MemberStatus status){this.status=status;}
}
