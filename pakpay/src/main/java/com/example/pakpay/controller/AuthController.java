package com.example.pakpay.controller;

import com.example.pakpay.dto.AuthResponse;
import com.example.pakpay.dto.LoginRequest;
import com.example.pakpay.dto.OTPRequestDTO;
import com.example.pakpay.dto.RegisterRequest;
import com.example.pakpay.dto.SignupResponse;
import com.example.pakpay.service.UserService;
import com.example.pakpay.service.WhatsAppNotificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final WhatsAppNotificationService whatsappService;
    
    public AuthController(UserService userService,
    		WhatsAppNotificationService whatsappService) {
        this.userService = userService;
        this.whatsappService = whatsappService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody RegisterRequest request) {
        SignupResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = userService.loginUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Header missing!");
        }

        String refreshToken = authHeader.substring(7);

        try {
            Map<String, String> tokens = userService.refreshToken(refreshToken);
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }


    @PostMapping("/send-otp")
    public ResponseEntity<String> testOtp(@RequestBody OTPRequestDTO requestDTO) {
        String phone = requestDTO.phone();

        // Basic Null/Empty Check
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body("Error: Phone number cannot be empty.");
        }

        // Spaces aur dashes saf karlein pehle validation ke liye
        phone = phone.trim().replaceAll("\\s+", "");

        // Validation: Agar 03 se start ho aur length exact 11 ho
        if (phone.startsWith("03") && phone.length() == 11) {
            phone = phone.substring(1); // Pehla character (0) remove krdo -> 3157073692
        }

        // Demo ke liye ek random 6-digit OTP generate karte hain
        String mockOtp = String.valueOf((int)((Math.random() * 900000) + 100000));
        
        // Notification service ko call karein (Service khud iske agay 92 lagaye gi ya handle kregi)
        whatsappService.sendOTPNotification(phone, mockOtp);
        
        return ResponseEntity.ok("OTP Request Triggered for: " + phone + " with OTP: " + mockOtp);
    }
}
