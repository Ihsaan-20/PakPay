package com.example.pakpay.dto;

public class UserResponse {
    private String fullName;
    private String mobileNumber;

    public UserResponse(String fullName, String mobileNumber) {
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
    }

    // Getters and Setters
    public String getFullName() { return fullName; }
    public String getMobileNumber() { return mobileNumber; }
}
