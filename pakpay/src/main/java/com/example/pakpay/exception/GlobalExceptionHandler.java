package com.example.pakpay.exception;

import com.example.pakpay.dto.ApiErrorResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern DUPLICATE_VALUE_PATTERN =
            Pattern.compile("Duplicate entry '([^']+)' for key");

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("Conflict", ex.getMessage(), ex.getField()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.validation(errors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String root = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        ApiErrorResponse body = mapDuplicateConstraint(root);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("Validation Error", ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of("Error", ex.getMessage(), null));
    }

    private ApiErrorResponse mapDuplicateConstraint(String message) {
        if (message == null) {
            return ApiErrorResponse.of("Conflict", "Record already exists", null);
        }
        String lower = message.toLowerCase();
        if (lower.contains("mobile_number") || lower.contains("mobilenumber")) {
            return ApiErrorResponse.of("Conflict", "Ye mobile number pehle se registered hai.", "mobileNumber");
        }
        if (lower.contains("cnic")) {
            return ApiErrorResponse.of("Conflict", "Ye CNIC pehle se registered hai.", "cnic");
        }
        if (lower.contains("email")) {
            return ApiErrorResponse.of("Conflict", "Account already exists for this user.", "email");
        }

        Matcher matcher = DUPLICATE_VALUE_PATTERN.matcher(message);
        if (matcher.find() && message.toLowerCase().contains("email")) {
            return ApiErrorResponse.of("Conflict", "Account already exists for this user.", "email");
        }

        return ApiErrorResponse.of("Conflict", "Record already exists. Mobile aur CNIC unique hone chahiye.", null);
    }
}
