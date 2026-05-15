package com.example.pakpay.service;

import com.example.pakpay.entity.User;
import com.example.pakpay.entity.Wallet;
import com.example.pakpay.entity.WalletStatus;
import com.example.pakpay.repository.UserRepository;
import com.example.pakpay.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

import com.example.pakpay.entity.Transaction;
import com.example.pakpay.repository.TransactionRepository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dummy external bank gateway — all Add Money deposits originate from this wallet.
 */
@Service
@RequiredArgsConstructor
public class SystemWalletService {

    public static final String GATEWAY_MOBILE = "92000000000";
    public static final String GATEWAY_WALLET_ACCOUNT = "PK-PAY-BANK-IN";
    public static final String GATEWAY_FULL_NAME = "PakPay Bank Gateway";

    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final TransactionRepository transactionRepo;

    @Transactional
    public Wallet getBankGatewayWallet() {
        Wallet existing = walletRepo.findByWalletAccountNumber(GATEWAY_WALLET_ACCOUNT).orElse(null);
        if (existing != null) {
            return existing;
        }

        User systemUser = userRepo.findByMobileNumber(GATEWAY_MOBILE).orElseGet(() -> {
            User user = new User();
            user.setMobileNumber(GATEWAY_MOBILE);
            user.setFullName(GATEWAY_FULL_NAME);
            user.setEmail("gateway@pakpay.com");
            user.setPassword("$2a$10$SYSTEM.NO.LOGIN.ACCOUNT.PLACEHOLDER.HASH");
            user.setCnicEncrypted("0000000000000");
            return userRepo.save(user);
        });

        Wallet wallet = new Wallet();
        wallet.setUserId(systemUser.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setWalletAccountNumber(GATEWAY_WALLET_ACCOUNT);
        wallet.setStatus(WalletStatus.ACTIVE);
        return walletRepo.save(wallet);
    }

    /** Fix legacy BANK_DEPOSIT rows that had null sender_wallet_id */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillBankDepositSenders() {
        Wallet gateway = getBankGatewayWallet();
        List<Transaction> legacy = transactionRepo.findByTypeAndSenderWalletIdIsNull("BANK_DEPOSIT");
        for (Transaction txn : legacy) {
            txn.setSenderWalletId(gateway.getId());
            if (txn.getDescription() == null || txn.getDescription().isBlank()) {
                txn.setDescription("Bank deposit");
            }
            transactionRepo.save(txn);
        }
    }
}
