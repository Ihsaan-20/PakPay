package com.example.pakpay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class EmailService {

    // SLF4J Logger use karein System.err ki jagah
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.features.email-alerts-enabled:false}")
    private boolean isEmailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendTransactionAlert(String toEmail, String type, BigDecimal amount, String trxId) {
        // Log lagayen taake pata chale function call hua bhi hai ya nahi
        log.info("Attempting to send {} email to {}", type, toEmail);

        if (!isEmailEnabled) {
            log.warn("Email alerts are DISABLED in config. Skipping email to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("PakPay Alert: " + type);
            message.setText("Transaction successful!\nAmount: PKR " + amount + "\nTrx ID: " + trxId);
            
            mailSender.send(message);
            log.info("Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}. Error: {}", toEmail, e.getMessage());
        }
    }
}