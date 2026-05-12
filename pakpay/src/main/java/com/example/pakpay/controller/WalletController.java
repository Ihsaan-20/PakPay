package com.example.pakpay.controller;

import com.example.pakpay.dto.TransferRequest;
import com.example.pakpay.dto.UserResponse;
import com.example.pakpay.entity.User;
import com.example.pakpay.entity.Wallet;
import com.example.pakpay.service.QrCodeService;
import com.example.pakpay.service.UserService;
import com.example.pakpay.service.WalletService;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService; 
    private final QrCodeService qrCodeService; 
    
    @GetMapping("/check/{mobileNumber}")
    public ResponseEntity<?> verifyReceiver(@PathVariable String mobileNumber) {
    	String fromMobile = SecurityContextHolder.getContext().getAuthentication().getName();
    	if(fromMobile.equals(mobileNumber)) {
    		Map<String, String> errorResponse = Map.of("error", "You cannot transfer to yourself.");
    		return ResponseEntity.badRequest().body(errorResponse);
    	}
        Optional<User> userOpt = walletService.findByMobileNumber(mobileNumber);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserResponse response = new UserResponse(user.getFullName(), user.getMobileNumber());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }
    
    @GetMapping("/my-qr2")
    public ResponseEntity<Map<String, String>> getMyQrCode(Principal principal) {
        // 1. User dhoondhein
    	String fromMobile = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByMobileNumber(fromMobile);
        // 2. Mobile number ko as a QR content use karein
        String mobileNumber = user.getMobileNumber();
        System.out.println("mobileNumber: " + mobileNumber); // Debugging ke liye print kar rahe hain
        // 3. QR Code generate karein
        String qrBase64 = qrCodeService.generateQrCodeBase64(mobileNumber);
        // 4. Response bhejein
        return ResponseEntity.ok(Map.of(
            "qrCode", "data:image/png;base64," + qrBase64,
            "mobileNumber", mobileNumber
        ));
    }
    
    @PostMapping("/secure-transfer")
    public ResponseEntity<?> transfer(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey, // Header se uthao
            @RequestParam String toMobile, 
            @RequestParam BigDecimal amount, 
            @RequestParam String pin) {
        
        String fromMobile = SecurityContextHolder.getContext().getAuthentication().getName();
        
        try {
            String result = walletService.secureTransfer(fromMobile, toMobile, amount, pin, idempotencyKey);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/set-pin")
    public ResponseEntity<?> setPin(@RequestParam String pin) {
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.setTransactionPin(mobile, pin));
    }
    
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance() {
        try {
        	String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
            BigDecimal balance = walletService.getBalanceByMobile(mobile);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/history") // URL se {mobile} hata diya
    public ResponseEntity<?> getMyHistory() {
        // Token se mobile nikal lo
        String mobile = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(walletService.getTransactionHistory(mobile));
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<String> transferFunds(@RequestBody TransferRequest request) {
        try {
            // Humne Service mein IDs mangi thi, ab mobile number se IDs nikal kar bhejenge
            String trxId = walletService.transferFundsByMobile(
                request.getSenderMobile(), 
                request.getReceiverMobile(), 
                request.getAmount()
            );
            return ResponseEntity.ok("Transfer Successful! Trx ID: " + trxId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Transfer Failed: " + e.getMessage());
        }
    }
}