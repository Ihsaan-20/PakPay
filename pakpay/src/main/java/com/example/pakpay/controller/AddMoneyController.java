package com.example.pakpay.controller;

import com.example.pakpay.dto.*;
import com.example.pakpay.service.AddMoneyService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/add-money")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*")
public class AddMoneyController {

    private final AddMoneyService addMoneyService;

    private String currentMobile() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/banks")
    public ResponseEntity<List<BankDto>> listBanks() {
        return ResponseEntity.ok(addMoneyService.listBanks());
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> profile() {
        String mobile = currentMobile();
        return ResponseEntity.ok(Map.of(
                "fullName", addMoneyService.getFullName(mobile),
                "mobileNumber", mobile));
    }

    @GetMapping("/linked-accounts")
    public ResponseEntity<List<LinkedBankAccountDto>> linkedAccounts() {
        return ResponseEntity.ok(addMoneyService.listLinkedAccounts(currentMobile()));
    }

    @PostMapping("/link/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody LinkBankSendOtpRequest request) {
        return ResponseEntity.ok(addMoneyService.sendLinkOtp(currentMobile(), request));
    }

    @PostMapping("/link/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody LinkBankVerifyOtpRequest request) {
        LinkedBankAccountDto linked = addMoneyService.verifyLinkOtp(currentMobile(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Bank account successfully linked!",
                "account", linked));
    }

    @PostMapping("/deposit")
    public ResponseEntity<AddMoneyService.DepositResult> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(addMoneyService.deposit(currentMobile(), request));
    }
}
