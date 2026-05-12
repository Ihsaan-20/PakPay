package com.example.pakpay.controller;

import com.example.pakpay.service.QRService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true", allowedHeaders = "*")
public class QRController {

    private final QRService qrService;

    @GetMapping("/my-qr")
    public ResponseEntity<?> getMyQRCode(@RequestParam String mobile, @RequestParam String name) {
        String qrString = qrService.generateEMVQRCode(mobile, name);
//        String qrString = qrService.generateEMVQRCodeWithIBAN(mobile, name);
        return ResponseEntity.ok(Map.of("qrString", qrString));
    }
    
}