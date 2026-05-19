package com.example.pakpay.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.Map;

@Service
public class WhatsAppNotificationService {

    private final RestClient restClient;
    
    // Aapki actual active session ID Jo Postman mein chal rhi hai
    private final String sessionId = ""; 

    public WhatsAppNotificationService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("http://127.0.0.1:2785/api") // Gateway Base URL
                .defaultHeader("X-API-Key", "")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendOTPNotification(String phoneNumber, String otpCode) {
        // Regex sirf numbers rakhega (+ ya spaces sab automatically remove ho jayenge)
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        String chatId = "92"+cleanNumber + "@c.us";
        System.out.println("Formatted Chat ID: " + chatId);
        String messageText = "🔒 *Your Security OTP*\n\n" +
                             "Your one-time password is: *" + otpCode + "*\n" +
                             "This code is valid for 5 minutes.";

        // Exact Request Body matching OpenWA schema
        Map<String, String> payload = Map.of(
            "chatId", chatId,
            "text", messageText
        );

        try {
            // Target API Endpoint: /sessions/{sessionId}/messages/send-text
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
