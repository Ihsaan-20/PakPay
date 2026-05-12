package com.example.pakpay.service;

import org.springframework.stereotype.Service;

@Service
public class QRService {
	
	public String generateEMVQRCode(String mobileNumber, String fullName) {
        StringBuilder sb = new StringBuilder();
        
        // 1. IBAN Generate karna (Standard NayaPay Format)
        String cleanMobile = mobileNumber.startsWith("0") ? mobileNumber.substring(1) : mobileNumber;
        String iban = "PK24NAYA12345" + cleanMobile;

        // --- EMV TAGS START ---
        sb.append("000201"); // Payload Format Indicator
        sb.append("010211"); // Point of Initiation (Static)
        
        // Tag 04: IBAN Number (Scanning apps yahan se IBAN uthati hain)
        sb.append("04").append(String.format("%02d", iban.length())).append(iban);

        // Tag 26: Merchant Account Information
        String merchantInfo = "0006PAKPAY" + "0111" + mobileNumber;
        sb.append("26").append(String.format("%02d", merchantInfo.length())).append(merchantInfo);

        sb.append("52040000"); // Category Code
        sb.append("5303586");  // Currency (PKR)
        sb.append("5802PK");   // Country Code
        
        // Tag 59: Merchant Name
        String name = fullName.length() > 20 ? fullName.substring(0, 20) : fullName;
        sb.append("59").append(String.format("%02d", name.length())).append(name);
        
        sb.append("6007Karachi"); // City
        sb.append("6304");        // CRC Tag Placeholder
        // --- EMV TAGS END ---

        // Final String with Checksum
        return sb.toString() + calculateCRC(sb.toString());
    }
	
	// QRService.java ke andar ye function replace karein
//	public String generateEMVQRCodeWithIBAN(String mobileNumber, String fullName) {
//	    StringBuilder sb = new StringBuilder();
//	    
//	    // NayaPay IBAN Format (Hardcoded static part + mobile)
//	    String iban = "PK24NAYA12345" + (mobileNumber.startsWith("0") ? mobileNumber.substring(1) : mobileNumber);
//
//	    sb.append("000201"); // Payload Indicator
//	    sb.append("010211"); // Static QR Method
//	    
//	    // Tag 04: IBAN Number
//	    sb.append("04").append(String.format("%02d", iban.length())).append(iban);
//
//	    // Tag 26: Merchant Account Info
//	    String merchantInfo = "0006PAKPAY" + "0111" + mobileNumber;
//	    sb.append("26").append(String.format("%02d", merchantInfo.length())).append(merchantInfo);
//
//	    sb.append("52040000"); // Category
//	    sb.append("5303586");  // PKR
//	    
//	    String name = fullName.length() > 20 ? fullName.substring(0, 20) : fullName;
//	    sb.append("59").append(String.format("%02d", name.length())).append(name);
//	    
//	    sb.append("6007Karachi");
//	    sb.append("6304"); // CRC Tag
//
//	    return sb.toString() + calculateCRC(sb.toString());
//	}
	
//    public String generateEMVQRCode(String mobileNumber, String fullName) {
//        StringBuilder sb = new StringBuilder();
//
//        // EMV Tags
//        sb.append("000201"); // Payload Format Indicator
//        sb.append("010211"); // Point of Initiation (Static)
//        
//        // Tag 26: Merchant Account Info (PakPay Application)
//        String subTags = "0006PAKPAY" + "0111" + mobileNumber;
//        sb.append("26").append(String.format("%02d", subTags.length())).append(subTags);
//
//        sb.append("52040000"); // Category Code
//        sb.append("5303586"); // Currency (PKR)
//        sb.append("5802PK");   // Country Code
//        
//        // Tag 59: Name (Max 25 chars for safety)
//        String name = fullName.length() > 25 ? fullName.substring(0, 25) : fullName;
//        sb.append("59").append(String.format("%02d", name.length())).append(name);
//        
//        sb.append("6007Karachi"); // City
//        sb.append("6304");        // CRC Tag + Length (Next 4 chars will be CRC)
//
//        String crc = calculateCRC(sb.toString());
//        return sb.toString() + crc;
//    }

    private String calculateCRC(String data) {
        int crc = 0xFFFF;          // Initial Value
        int polynomial = 0x1021;   // CCITT Polynomial

        byte[] bytes = data.getBytes();
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }
        crc &= 0xFFFF;
        return String.format("%04X", crc);
    }
    
 // QRService.java ke andar update karein
//    public String generateEMVQRCode(String mobileNumber, String fullName) {
//        StringBuilder sb = new StringBuilder();
//        String iban = generateNayaPayIBAN(mobileNumber);
//
//        sb.append("000201"); 
//        sb.append("010211");
//        
//        // Tag 04: Merchant IBAN / Account Information
//        sb.append("04").append(String.format("%02d", iban.length())).append(iban);
//
//        // Baki tags (26, 52, 53, 58, 59, 60, 63) wese hi rahenge...
//        // ...
//        
//        return sb.toString() + calculateCRC(sb.toString());
//    }
}