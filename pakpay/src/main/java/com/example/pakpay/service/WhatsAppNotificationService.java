package com.example.pakpay.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.Map;

@Service
public class WhatsAppNotificationService {

    private final RestClient restClient;
    private final String sessionId;
    private final boolean isEnabled;

    public WhatsAppNotificationService(
            @Value("${pakpay.whatsapp.base-url}") String baseURL,
            @Value("${pakpay.whatsapp.api-key}") String apiKey,
            @Value("${pakpay.whatsapp.session-id}") String sessionId,
            @Value("${pakpay.whatsapp.enabled:false}") boolean isEnabled) {
        this.sessionId = sessionId;
        this.isEnabled = isEnabled;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseURL)
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendOTPNotification(String phoneNumber, String otpCode) {
        if (!isEnabled) {
            System.out.println("WhatsApp OTP disabled. Skipping send for: " + phoneNumber);
            return;
        }

        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        if (cleanNumber.startsWith("0")) {
            cleanNumber = cleanNumber.substring(1);
        } else if (cleanNumber.startsWith("92")) {
            cleanNumber = cleanNumber.substring(2);
        }
        String chatId = "92" + cleanNumber + "@c.us";
        System.out.println("Formatted Chat ID: " + chatId);

        String messageText = "🏦 *PakPay* - Security Code\n\n" +
                             "Your verification code is:\n" +
                             "*" + otpCode + "*\n\n" +
                             "This code is valid for 5 minutes.\n" +
                             "Do not share it with anyone.\n\n" +
                             "🙏 *PakPay Team*";

        Map<String, String> payload = Map.of(
            "chatId", chatId,
            "text", messageText
        );

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("/sessions/{sessionId}/messages/send-text", sessionId)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            System.out.println("Response from OpenWA: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Failed to send OTP via OpenWA: " + e.getMessage());
        }
    }
}
