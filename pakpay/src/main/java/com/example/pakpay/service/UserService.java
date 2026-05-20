package com.example.pakpay.service;

import com.example.pakpay.dto.AuthResponse;
import com.example.pakpay.dto.ForgotPasswordResetRequest;
import com.example.pakpay.dto.ForgotPasswordResponse;
import com.example.pakpay.dto.ForgotPasswordSendOtpRequest;
import com.example.pakpay.dto.ForgotPasswordVerifyOtpRequest;
import com.example.pakpay.dto.LoginRequest;
import com.example.pakpay.dto.RegisterRequest;
import com.example.pakpay.dto.SignupResponse;
import com.example.pakpay.dto.UserWalletDTO;
import com.example.pakpay.entity.PasswordResetOtp;
import com.example.pakpay.entity.User;
import com.example.pakpay.exception.ConflictException;
import com.example.pakpay.util.RegistrationValidator;
import com.example.pakpay.entity.UserToken;
import com.example.pakpay.entity.Wallet;
import com.example.pakpay.repository.PasswordResetOtpRepository;
import com.example.pakpay.repository.UserRepository;
import com.example.pakpay.repository.UserTokenRepository;
import com.example.pakpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserTokenRepository userTokenRepo;
    private final PasswordResetOtpRepository passwordResetOtpRepo;
    private final WhatsAppNotificationService whatsappService;
    private final AuthenticationManager authenticationManager;
    
    
    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with email " + email + " not found"));
    }

    public User findByMobileNumber(String mobileNumber) {
        return userRepo.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User with mobile number " + mobileNumber + " not found"));
    }

    public String setTransactionPin(String mobile, String rawPin) {
        if (rawPin.length() != 4) {
            throw new RuntimeException("PIN must be exactly 4 digits!");
        }
        
        User user = userRepo.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        user.setTransactionPin(passwordEncoder.encode(rawPin)); // PIN ko bhi encrypt rakhna hai
        userRepo.save(user);
        
        return "Transaction PIN set successfully!";
    }
    
    public Map<String, String> refreshToken(String refreshToken) {
        // 1. DB se refresh token dhundo
        UserToken storedToken = userTokenRepo.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid Refresh Token"));

        // 2. Check karo revoked to nahi
        if (storedToken.isRevoked() || storedToken.isExpired()) {
            throw new RuntimeException("Token is no longer valid!");
        }

        // 3. Token se mobile nikal kar naya Access Token banao
        String mobile = jwtService.extractMobileNumber(refreshToken);
        
        // Optional: Refresh token ki validity bhi check karlo (signature wise)
        if (!jwtService.isTokenValid(refreshToken, mobile)) {
            throw new RuntimeException("Refresh token expired!");
        }

        String newAccessToken = jwtService.generateToken(mobile);

        // 4. DB mein update karo
        storedToken.setAccessToken(newAccessToken);
        userTokenRepo.save(storedToken);

        Map<String, String> response = new HashMap<>();
        response.put("access_token", newAccessToken);
        response.put("refresh_token", refreshToken); // Wahi refresh token wapis bhej dete hain
        
        return response;
    }
    
//    public Map<String, String> login(String mobile, String password) {
//        User user = userRepo.findByMobileNumber(mobile)
//                .orElseThrow(() -> new RuntimeException("User not found!"));
//
//        if (passwordEncoder.matches(password, user.getPassword())) {
//            String accessToken = jwtService.generateToken(mobile);
//            String refreshToken = jwtService.generateRefreshToken(mobile);
//
//            // DB mein save karo
//            UserToken userToken = UserToken.builder()
//                    .accessToken(accessToken)
//                    .refreshToken(refreshToken)
//                    .mobileNumber(mobile)
//                    .expired(false)
//                    .revoked(false)
//                    .build();
//            userTokenRepo.save(userToken);
//
//            Map<String, String> tokens = new HashMap<>();
//            tokens.put("access_token", accessToken);
//            tokens.put("refresh_token", refreshToken);
//            return tokens;
//        } else {
//            throw new RuntimeException("Ghalat password!");
//        }
//    }
    
    public UserWalletDTO findWalletDetailsByMobile(String mobileNumber) {
        return userRepo.findUserAndWalletDetails(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User or Wallet not found for: " + mobileNumber));
    }

    // Login mein use:
    public AuthResponse loginUser(LoginRequest loginRequest) {

    	System.out.println("1. Entering loginUser method...");
        
        // Step A: Find User
        String loginMobile = RegistrationValidator.normalizeMobile(loginRequest.mobileNumber());
        User user = userRepo.findByMobileNumber(loginMobile)
                .orElseGet(() -> {
                    System.out.println("DEBUG >> User NOT FOUND in DB for: " + loginMobile);
                    throw new RuntimeException("User not found!");
                });

        System.out.println("2. User found in DB: " + user.getMobileNumber());
        System.out.println("3. DB Hash: " + user.getPassword());

        // Step B: Manual Match
        boolean manualMatch = passwordEncoder.matches(loginRequest.password(), user.getPassword());
        System.out.println("4. Manual Match Result: " + manualMatch);

        if (!manualMatch) {
            throw new RuntimeException("Invalid Password!");
        }
        
        try {
            System.out.println("Attempting login for: " + loginMobile);
            System.out.println("Raw Password from Request: " + loginRequest.password());
            // Step 1: Authentication
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginMobile, loginRequest.password())
            );
            System.out.println("Authentication successful!");

            // Step 2: Fetch Details
            UserWalletDTO details = findWalletDetailsByMobile(loginMobile);
            System.out.println("User details found: " + details.fullName());

            // Step 3: Tokens
            String jwtToken = jwtService.generateToken(details.mobileNumber());
            String jwtRefreshToken = jwtService.generateRefreshToken(details.mobileNumber());

            // Step 4: Save Token
            UserToken userToken = UserToken.builder()
                    .accessToken(jwtToken)
                    .refreshToken(jwtRefreshToken)
                    .mobileNumber(details.mobileNumber())
                    .expired(false)
                    .revoked(false)
                    .build();
            userTokenRepo.save(userToken);

            String isPinSet = "no";
            System.out.println("details:"+details);
            if (details.transactionPin() != null && !details.transactionPin().isEmpty()) {
                isPinSet = "yes";
            }else {
            	isPinSet = "no";
            }
            
            return new AuthResponse(
            	    jwtToken,
            	    jwtRefreshToken,
            	    details.email(),
            	    details.fullName(),
            	    details.mobileNumber(),
            	    details.balance(),
            	    details.walletAccountNumber(),
            	    isPinSet
            	);
        } catch (Exception e) {
            System.out.println("LOGIN FAILED: " + e.getMessage());
            throw new RuntimeException("Login Error: " + e.getMessage());
        }
    }
    
    @Transactional
    public SignupResponse registerUser(RegisterRequest request) {
        String normalizedMobile = RegistrationValidator.normalizeMobile(request.mobileNumber());
        String normalizedCnic = RegistrationValidator.normalizeCnic(request.cnic());

        if (!RegistrationValidator.isValidMobile(normalizedMobile)) {
            throw new IllegalArgumentException("Mobile number 03XXXXXXXXX format mein hona chahiye.");
        }
        if (!RegistrationValidator.isValidCnic(normalizedCnic)) {
            throw new IllegalArgumentException("CNIC 13 digits ka hona chahiye.");
        }

        if (userRepo.existsByMobileNumber(normalizedMobile)) {
            throw new ConflictException("mobileNumber", "Ye mobile number pehle se registered hai.");
        }
        if (userRepo.existsByCnicEncrypted(normalizedCnic)) {
            throw new ConflictException("cnic", "Ye CNIC pehle se registered hai. Ek CNIC sirf ek account ke liye.");
        }

        String virtualEmail = RegistrationValidator.buildVirtualEmail(normalizedMobile);
        if (userRepo.findByEmail(virtualEmail).isPresent()) {
            throw new ConflictException("mobileNumber", "Ye mobile number pehle se registered hai.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setMobileNumber(normalizedMobile);
        user.setEmail(virtualEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setCnicEncrypted(normalizedCnic);
        User savedUser = userRepo.save(user);

        String randomAccountNumber = "PK-PAY-" + (int) (Math.random() * 900000 + 100000);

        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setWalletAccountNumber(randomAccountNumber);
        walletRepo.save(wallet);

        return new SignupResponse("Registration successful!", normalizedMobile);
    }

    @Transactional
    public ForgotPasswordResponse sendPasswordResetOtp(ForgotPasswordSendOtpRequest request) {
        String mobile = RegistrationValidator.normalizeMobile(request.mobileNumber());

        User user = userRepo.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("Is mobile number se koi account nahi mila."));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        int todayCount = passwordResetOtpRepo.countByMobileNumberAndCreatedAtAfter(mobile, todayStart);

        if (todayCount >= 3) {
            throw new RuntimeException("Aap din mein sirf 3 baar OTP bhej sakte hain. Kal dobara try karein.");
        }

        String otpCode = String.valueOf((int) ((Math.random() * 900000) + 100000));

        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setMobileNumber(mobile);
        otp.setOtpCode(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        passwordResetOtpRepo.save(otp);

        try {
            whatsappService.sendOTPNotification(mobile, otpCode);
        } catch (Exception e) {
            System.err.println("WhatsApp notification failed: " + e.getMessage());
        }

        return new ForgotPasswordResponse("OTP aapke mobile number par bhej diya gaya hai.");
    }

    public ForgotPasswordResponse verifyPasswordResetOtp(ForgotPasswordVerifyOtpRequest request) {
        String mobile = RegistrationValidator.normalizeMobile(request.mobileNumber());

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        int invalidToday = passwordResetOtpRepo.findTopByMobileNumberOrderByCreatedAtDesc(mobile)
                .map(PasswordResetOtp::getInvalidAttempts)
                .orElse(0);

        if (invalidToday >= 3) {
            throw new RuntimeException("Bohat zyada galat attempts. Kal dobara try karein.");
        }

        PasswordResetOtp otp = passwordResetOtpRepo.findTopByMobileNumberOrderByCreatedAtDesc(mobile)
                .orElseThrow(() -> new RuntimeException("Pehle OTP bhejein."));

        if (otp.isVerified()) {
            throw new RuntimeException("Ye OTP pehle hi verify ho chuka hai.");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expire ho chuka hai. Naya OTP bhejein.");
        }

        if (!otp.getOtpCode().equals(request.otpCode())) {
            otp.setInvalidAttempts(otp.getInvalidAttempts() + 1);
            passwordResetOtpRepo.save(otp);

            int remaining = 3 - otp.getInvalidAttempts();
            if (remaining <= 0) {
                throw new RuntimeException("Bohat zyada galat attempts. Kal dobara try karein.");
            }
            throw new RuntimeException("Galat OTP. Aapke paas " + remaining + " attempt(s) baaqi hain.");
        }

        otp.setVerified(true);
        passwordResetOtpRepo.save(otp);

        return new ForgotPasswordResponse("OTP verify ho gaya hai. Ab naya password set karein.");
    }

    @Transactional
    public ForgotPasswordResponse resetPassword(ForgotPasswordResetRequest request) {
        String mobile = RegistrationValidator.normalizeMobile(request.mobileNumber());

        PasswordResetOtp otp = passwordResetOtpRepo.findTopByMobileNumberAndVerifiedTrueOrderByCreatedAtDesc(mobile)
                .orElseThrow(() -> new RuntimeException("Pehle OTP verify karein."));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expire ho chuka hai. Dobara OTP bhejein.");
        }

        User user = userRepo.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User nahi mila."));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);

        return new ForgotPasswordResponse("Password successfully change ho gaya hai.");
    }
}
