package com.example.pakpay.service;

import com.example.pakpay.dto.*;
import com.example.pakpay.entity.*;
import com.example.pakpay.repository.BankLinkOtpRepository;
import com.example.pakpay.repository.LinkedBankAccountRepository;
import com.example.pakpay.repository.TransactionRepository;
import com.example.pakpay.repository.UserRepository;
import com.example.pakpay.repository.WalletRepository;
import com.example.pakpay.util.PakistaniBanks;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddMoneyService {

    private final BankLinkOtpRepository otpRepo;
    private final LinkedBankAccountRepository linkedRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final TransactionRepository transactionRepo;
    private final SystemWalletService systemWalletService;
    private final WhatsAppNotificationService whatsappService;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.features.demo-otp-exposed:true}")
    private boolean demoOtpExposed;

    public List<BankDto> listBanks() {
        return PakistaniBanks.ALL;
    }

    public String getFullName(String mobile) {
        return userRepo.findByMobileNumber(mobile)
                .map(User::getFullName)
                .orElse("User");
    }

    public List<LinkedBankAccountDto> listLinkedAccounts(String mobile) {
        return linkedRepo.findByMobileNumberAndActiveTrueOrderByLinkedAtDesc(mobile).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SendOtpResponse sendLinkOtp(String mobile, LinkBankSendOtpRequest request) {
        BankDto bank = PakistaniBanks.findByCode(request.bankCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid bank selected."));

        String accountNumber = request.accountNumber().trim();
        if (linkedRepo.existsByMobileNumberAndBankCodeAndAccountNumberAndActiveTrue(
                mobile, bank.code(), accountNumber)) {
            throw new IllegalArgumentException("Ye bank account pehle se linked hai.");
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));

        BankLinkOtp record = new BankLinkOtp();
        record.setMobileNumber(mobile);
        record.setBankCode(bank.code());
        record.setAccountNumber(accountNumber);
        record.setAccountTitle(request.accountTitle().trim());
        record.setOtpCode(otp);
        record.setVerified(false);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        BankLinkOtp saved = otpRepo.save(record);

        try {
            whatsappService.sendOTPNotification(mobile, otp);
        } catch (Exception e) {
            System.err.println("WhatsApp notification failed: " + e.getMessage());
        }

        String message = "OTP aap ke registered mobile par bhej diya gaya hai.";
        String demoOtp = demoOtpExposed ? otp : null;
        return new SendOtpResponse(saved.getId(), message, demoOtp);
    }

    @Transactional
    public LinkedBankAccountDto verifyLinkOtp(String mobile, LinkBankVerifyOtpRequest request) {
        BankLinkOtp record = otpRepo.findByIdAndMobileNumber(request.otpRequestId(), mobile)
                .orElseThrow(() -> new IllegalArgumentException("OTP request not found."));

        if (record.isVerified()) {
            throw new IllegalArgumentException("OTP already used. Naya OTP mangwaein.");
        }
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP expire ho chuka hai. Dobara try karein.");
        }
        if (!record.getOtpCode().equals(request.otp().trim())) {
            throw new IllegalArgumentException("Ghalat OTP. Dobara try karein.");
        }

        record.setVerified(true);
        otpRepo.save(record);

        BankDto bank = PakistaniBanks.findByCode(record.getBankCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid bank."));

        if (linkedRepo.existsByMobileNumberAndBankCodeAndAccountNumberAndActiveTrue(
                mobile, bank.code(), record.getAccountNumber())) {
            return linkedRepo
                    .findByMobileNumberAndActiveTrueOrderByLinkedAtDesc(mobile).stream()
                    .filter(a -> a.getBankCode().equals(bank.code())
                            && a.getAccountNumber().equals(record.getAccountNumber()))
                    .findFirst()
                    .map(this::toDto)
                    .orElseThrow();
        }

        LinkedBankAccount linked = new LinkedBankAccount();
        linked.setMobileNumber(mobile);
        linked.setBankCode(bank.code());
        linked.setBankName(bank.name());
        linked.setLogoKey(bank.logoKey());
        linked.setAccountNumber(record.getAccountNumber());
        linked.setAccountTitle(record.getAccountTitle());
        linked.setActive(true);
        linked = linkedRepo.save(linked);

        return toDto(linked);
    }

    @Transactional
    public DepositResult deposit(String mobile, DepositRequest request) {
        LinkedBankAccount linked = linkedRepo
                .findByIdAndMobileNumberAndActiveTrue(request.linkedAccountId(), mobile)
                .orElseThrow(() -> new IllegalArgumentException("Linked bank account not found."));

        User user = userRepo.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Wallet wallet = walletRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal amount = request.amount();
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepo.save(wallet);

        Wallet gatewayWallet = systemWalletService.getBankGatewayWallet();

        String trxId = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String depositDescription = linked.getBankName() + " (" + maskAccount(linked.getAccountNumber()) + ")";

        Transaction txn = new Transaction();
        txn.setTrxId(trxId);
        txn.setSenderWalletId(gatewayWallet.getId());
        txn.setReceiverWalletId(wallet.getId());
        txn.setAmount(amount);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setType("BANK_DEPOSIT");
        txn.setDescription(depositDescription);
        txn.setIdempotencyKey("dep-" + trxId);
        txn.setCreatedAt(LocalDateTime.now());
        transactionRepo.save(txn);

        return new DepositResult(
                trxId,
                amount,
                wallet.getBalance(),
                "Rs. " + amount + " aap ke PakPay wallet mein add ho gaye (dummy transfer).",
                linked.getBankName(),
                maskAccount(linked.getAccountNumber()));
    }

    private LinkedBankAccountDto toDto(LinkedBankAccount account) {
        return new LinkedBankAccountDto(
                account.getId(),
                account.getBankCode(),
                account.getBankName(),
                account.getLogoKey(),
                maskAccount(account.getAccountNumber()),
                account.getAccountTitle());
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    public record DepositResult(
            String trxId,
            BigDecimal amount,
            BigDecimal newBalance,
            String message,
            String bankName,
            String accountMasked) {}
}
