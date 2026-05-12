package com.example.pakpay.helper;

public class EMVQRCodeGenerator {

    public static String generatePakPayQR(String mobileNumber, String amount, String fullName) {
        StringBuilder qr = new StringBuilder();

        // 00: Payload Format Indicator
        qr.append("000201");
        // 01: Point of Initiation (11 = Static QR)
        qr.append("010211");
        
        // 26: Merchant Account Information (Custom for PakPay)
        // Tag 26 has sub-tags: 00 (Application ID), 01 (Mobile Number)
        String merchantData = "0006PAKPAY" + "0111" + mobileNumber;
        qr.append("26").append(String.format("%02d", merchantData.length())).append(merchantData);

        // 52: Merchant Category Code
        qr.append("52040000");
        // 53: Currency (586 for PKR)
        qr.append("5303586");
        
        if (amount != null && !amount.isEmpty()) {
            qr.append("54").append(String.format("%02d", amount.length())).append(amount);
        }

        // 58: Country Code (PK)
        qr.append("5802PK");
        // 59: Merchant Name
        qr.append("59").append(String.format("%02d", fullName.length())).append(fullName);
        // 60: Merchant City
        qr.append("6007Karachi");

        // 63: CRC (Checksum) - Pehle tag 63 likhte hain, phir 4 digits ka CRC calculate hota hai
        qr.append("6304");
        String finalQR = qr.toString() + calculateCRC(qr.toString());
        
        return finalQR;
    }

    private static String calculateCRC(String data) {
        // CRC-CCITT (FFFF) algorithm implementation needed here
        // Filhal placeholder (Testing ke liye)
        return "ABCD"; 
    }
}