package com.libraryhub.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends LibraryHub emails (login codes, reservation notices).
 *
 * Two ways to send, chosen with MAIL_PROVIDER:
 *  - "smtp"  (default): a normal mail server such as Gmail. Good on your own laptop.
 *  - "brevo": Brevo's HTTPS API. Use this on Render's free plan, which blocks SMTP ports 25/465/587.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender sender;
    private final RestClient http = RestClient.create();
    private final boolean enabled;
    private final boolean developmentOtp;
    private final String provider;
    private final String brevoApiKey;
    private final String from;
    private final String fromName;

    public EmailService(JavaMailSender sender,
        @Value("${app.mail.enabled:false}") boolean enabled,
        @Value("${app.otp.show-in-response:false}") boolean developmentOtp,
        @Value("${app.mail.provider:smtp}") String provider,
        @Value("${app.mail.brevo-api-key:}") String brevoApiKey,
        @Value("${app.mail.from:}") String from,
        @Value("${app.mail.from-name:LibraryHub}") String fromName,
        @Value("${spring.mail.host:}") String host,
        @Value("${spring.mail.username:}") String username) {
        this.sender = sender;
        this.enabled = enabled;
        this.developmentOtp = developmentOtp;
        this.provider = provider.trim().toLowerCase();
        this.brevoApiKey = brevoApiKey.trim();
        this.from = from.isBlank() ? username : from;
        this.fromName = fromName;

        if (!enabled) return;
        if (this.from.isBlank())
            throw new IllegalStateException("Set MAIL_FROM (the address emails are sent from) when MAIL_ENABLED=true");
        if (isBrevo() && this.brevoApiKey.isBlank())
            throw new IllegalStateException("Set BREVO_API_KEY when MAIL_PROVIDER=brevo");
        if (!isBrevo() && host.isBlank())
            throw new IllegalStateException("Set MAIL_HOST when MAIL_PROVIDER=smtp");
        log.info("Email sending enabled via {} from {}", isBrevo() ? "Brevo API" : "SMTP " + host, this.from);
    }

    private boolean isBrevo() {
        return "brevo".equals(provider);
    }

    public void requireOtpDelivery() {
        if (!enabled && !developmentOtp)
            throw new IllegalStateException("Email is not configured. Set MAIL_ENABLED=true with SMTP or Brevo settings, or enable SHOW_DEV_OTP for local development only.");
    }

    public void sendOtp(String to, String code, String purpose) {
        requireOtpDelivery();
        if (!enabled) {
            log.info("Development OTP requested for {} ({})", to, purpose);
            return;
        }
        String subject = switch (purpose) {
            case "REGISTER" -> "Verify your LibraryHub email";
            case "LOGIN" -> "Your LibraryHub sign-in code";
            case "RESET" -> "Reset your LibraryHub password";
            default -> "Your LibraryHub verification code";
        };
        send(to, subject, "Hello,\n\nYour LibraryHub code is " + code + ". It expires in 5 minutes.\n\n"
            + "If you did not request this code, you can ignore this message. Never share it with anyone.\n\nLibraryHub");
    }

    /** Reservation and loan reminders. Best effort: a mail problem never breaks the action itself. */
    public void sendNotice(String to, String subject, String body) {
        if (!enabled) return;
        try {
            send(to, subject, body + "\n\nLibraryHub");
        } catch (RuntimeException ex) {
            log.warn("Could not send notice to {}: {}", to, ex.getMessage());
        }
    }

    private void send(String to, String subject, String body) {
        if (isBrevo()) sendWithBrevo(to, subject, body);
        else sendWithSmtp(to, subject, body);
    }

    private void sendWithSmtp(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }

    private void sendWithBrevo(String to, String subject, String body) {
        Map<String, Object> payload = Map.of(
            "sender", Map.of("email", from, "name", fromName),
            "to", List.of(Map.of("email", to)),
            "subject", subject,
            "textContent", body,
            "htmlContent", "<html><body><p style=\"font-family:Arial,sans-serif;white-space:pre-line\">"
                + escapeHtml(body) + "</p></body></html>");
        try {
            http.post()
                .uri(BREVO_URL)
                .header("api-key", brevoApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException ex) {
            // Don't log the API key or the code; the message from Brevo is enough to debug.
            log.error("Brevo could not send email to {}: {}", to, ex.getMessage());
            // MailSendException is handled by GlobalExceptionHandler ("Email delivery failed").
            throw new MailSendException("Email delivery failed", ex);
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
