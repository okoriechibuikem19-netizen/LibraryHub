package com.libraryhub.api.service;

import com.libraryhub.api.dto.AuthDtos.*;
import com.libraryhub.api.exception.*;
import com.libraryhub.api.model.*;
import com.libraryhub.api.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final OtpCodeRepository otps;
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final SecureRandom random=new SecureRandom();
    private final long expiryMinutes;
    private final boolean showDevOtp;
    public AuthService(UserAccountRepository users,OtpCodeRepository otps,MemberRepository members,PasswordEncoder encoder,EmailService emailService,
                       @Value("${app.otp.expiry-minutes:5}") long expiryMinutes,@Value("${app.otp.show-in-response:false}") boolean showDevOtp){
        this.users=users;this.otps=otps;this.members=members;this.encoder=encoder;this.emailService=emailService;this.expiryMinutes=expiryMinutes;this.showDevOtp=showDevOtp;
    }
    public MessageResponse register(RegisterRequest request){
        emailService.requireOtpDelivery();
        String address=normalize(request.email());
        if(users.findByEmailIgnoreCase(address).isPresent())throw new DuplicateResourceException("An account already exists for this email");
        UserAccount user=new UserAccount();user.setName(request.name().trim());user.setEmail(address);user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(UserRole.MEMBER);user.setStatus(UserStatus.PENDING);user.setEmailVerified(false);user.setCreatedAt(Instant.now());users.save(user);
        try{return issue(address,"REGISTER","Account created. Enter the verification code sent to your email.");}
        catch(org.springframework.mail.MailException ex){users.delete(user);throw ex;}
    }
    public MessageResponse login(LoginRequest request){
        emailService.requireOtpDelivery();
        UserAccount user=find(request.email());
        if(!encoder.matches(request.password(),user.getPasswordHash()))throw new IllegalArgumentException("Incorrect email or password");
        if(user.getStatus()==UserStatus.SUSPENDED)throw new IllegalArgumentException("This account is suspended");
        if(!user.isEmailVerified())return issue(user.getEmail(),"REGISTER","Verify your email before signing in.");
        return issue(user.getEmail(),"LOGIN","A login code has been sent to your email.");
    }
    public UserAccount verify(VerifyOtpRequest request){
        UserAccount user=find(request.email());verifyCode(user.getEmail(),request.purpose().toUpperCase(),request.code());
        if("REGISTER".equalsIgnoreCase(request.purpose())){
            user.setEmailVerified(true);user.setStatus(UserStatus.ACTIVE);
            if(members.findByEmailIgnoreCase(user.getEmail()).isEmpty()){
                Member member=new Member();member.setMemberId("M"+System.currentTimeMillis());member.setName(user.getName());member.setEmail(user.getEmail());
                member.setPhone("Not provided");member.setJoined(java.time.LocalDate.now());member.setStatus(MemberStatus.ACTIVE);members.save(member);
            }
        }
        user.setLastLoginAt(Instant.now());return users.save(user);
    }
    public MessageResponse resend(ResendOtpRequest request){find(request.email());return issue(normalize(request.email()),request.purpose().toUpperCase(),"A new verification code has been sent.");}
    public MessageResponse forgot(ForgotPasswordRequest request){find(request.email());return issue(normalize(request.email()),"RESET","A password reset code has been sent.");}
    public void reset(ResetPasswordRequest request){UserAccount user=find(request.email());verifyCode(user.getEmail(),"RESET",request.code());user.setPasswordHash(encoder.encode(request.newPassword()));users.save(user);}
    public UserAccount find(String email){return users.findByEmailIgnoreCase(normalize(email)).orElseThrow(()->new ResourceNotFoundException("Account not found"));}
    public UserResponse updateProfile(String email,String name){
        if(name==null||name.isBlank()||name.length()>120)throw new IllegalArgumentException("Enter a valid name");
        UserAccount user=find(email);user.setName(name.trim());
        members.findByEmailIgnoreCase(user.getEmail()).ifPresent(m->{m.setName(user.getName());members.save(m);});
        return response(users.save(user));
    }
    public UserResponse response(UserAccount user){return new UserResponse(user.getId(),user.getName(),user.getEmail(),user.getRole(),user.getStatus(),user.isEmailVerified(),user.isVoiceEnabled());}
    public UserResponse createStaff(StaffRequest request){
        if(request.role()==UserRole.MEMBER)throw new IllegalArgumentException("Use registration to create member accounts");
        String address=normalize(request.email());if(users.findByEmailIgnoreCase(address).isPresent())throw new DuplicateResourceException("Email already exists");
        UserAccount user=new UserAccount();user.setName(request.name().trim());user.setEmail(address);user.setPasswordHash(encoder.encode(request.password()));
        user.setRole(request.role());user.setStatus(UserStatus.ACTIVE);user.setEmailVerified(true);user.setCreatedAt(Instant.now());return response(users.save(user));
    }
    private MessageResponse issue(String address,String purpose,String message){
        otps.deleteByEmailIgnoreCaseAndPurpose(address,purpose);String code=String.format("%06d",random.nextInt(1_000_000));
        OtpCode otp=new OtpCode();otp.setEmail(address);otp.setPurpose(purpose);otp.setCodeHash(encoder.encode(code));otp.setExpiresAt(Instant.now().plusSeconds(expiryMinutes*60));otps.save(otp);
        try{emailService.sendOtp(address,code,purpose);}
        catch(RuntimeException ex){otps.delete(otp);throw ex;}
        return new MessageResponse(message,showDevOtp?code:null);
    }
    private void verifyCode(String address,String purpose,String code){
        OtpCode otp=otps.findTopByEmailIgnoreCaseAndPurposeOrderByExpiresAtDesc(address,purpose).orElseThrow(()->new IllegalArgumentException("Verification code not found"));
        if(otp.getExpiresAt().isBefore(Instant.now()))throw new IllegalArgumentException("Verification code has expired");
        if(otp.getAttempts()>=5)throw new IllegalArgumentException("Too many incorrect attempts. Request a new code.");
        if(!encoder.matches(code,otp.getCodeHash())){otp.setAttempts(otp.getAttempts()+1);otps.save(otp);throw new IllegalArgumentException("Incorrect verification code");}
        otps.delete(otp);
    }
    private String normalize(String value){return value.trim().toLowerCase();}
}
