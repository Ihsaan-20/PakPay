package com.example.pakpay.dto;

import java.util.Map;

public record ApiErrorResponse(
    String error,
    String message,
    String field,
    Map<String, String> errors
) {
    public static ApiErrorResponse of(String error, String message, String field) {
        return new ApiErrorResponse(error, message, field, null);
    }

    public static ApiErrorResponse validation(Map<String, String> errors) {
        return new ApiErrorResponse("Validation Error", "Invalid request data", null, errors);
    }
}
