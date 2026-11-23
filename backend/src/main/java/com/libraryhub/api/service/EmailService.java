package com.libraryhub.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log=LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender sender;
    private final boolean enabled;
    private final boolean developmentOtp;
    private final String from;
    private final String host;

    public EmailService(JavaMailSender sender,
        @Value("${app.mail.enabled:false}") boolean enabled,
        @Value("${app.otp.show-in-response:false}") boolean developmentOtp,
        @Value("${app.mail.from:}") String from,
        @Value("${spring.mail.host:}") String host,
        @Value("${spring.mail.username:}") String username) {
        this.sender=sender;this.enabled=enabled;this.developmentOtp=developmentOtp;
        this.from=from.isBlank()?username:from;this.host=host;
        if(enabled&&(this.from.isBlank()||host.isBlank()))
            throw new IllegalStateException("MAIL_HOST and a sender (MAIL_USERNAME or MAIL_FROM) are required when MAIL_ENABLED=true");
    }

    public void requireOtpDelivery() {
        if(!enabled&&!developmentOtp)
            throw new IllegalStateException("Email is not configured. Set SMTP variables and MAIL_ENABLED=true, or enable SHOW_DEV_OTP for local development only.");
    }

    public void sendOtp(String to,String code,String purpose) {
        requireOtpDelivery();
        if(!enabled){log.info("Development OTP requested for {} ({})",to,purpose);return;}
        String subject=switch(purpose){
            case "REGISTER" -> "Verify your LibraryHub email";
            case "LOGIN" -> "Your LibraryHub sign-in code";
            case "RESET" -> "Reset your LibraryHub password";
            default -> "Your LibraryHub verification code";
        };
        send(to,subject,"Hello,\n\nYour LibraryHub code is "+code+". It expires in 5 minutes.\n\nIf you did not request this code, you can ignore this message. Never share it with anyone.\n\nLibraryHub");
    }

    public void sendNotice(String to,String subject,String body){
        if(!enabled)return;
        send(to,subject,body+"\n\nLibraryHub");
    }

    private void send(String to,String subject,String body){
        SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(to);
        message.setSubject(subject);message.setText(body);sender.send(message);
    }
}
