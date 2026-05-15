package com.example.pakpay.service;

import com.example.pakpay.dto.TransactionHistoryDTO;
import com.example.pakpay.entity.Transaction;
import com.example.pakpay.entity.TransactionStatus;
import com.example.pakpay.entity.User;
import com.example.pakpay.entity.UserLimit;
import com.example.pakpay.entity.Wallet;
import com.example.pakpay.repository.TransactionRepository;
import com.example.pakpay.repository.UserLimitRepository;
import com.example.pakpay.repository.UserRepository;
import com.example.pakpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;



@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepo;
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;
    private final UserLimitRepository limitRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public Optional<User> findByMobileNumber(String mobileNumber) {
        return userRepo.findByMobileNumber(mobileNumber);
    }
    
    @Transactional
    public String secureTransfer(String senderMobile, String receiverMobile, BigDecimal amount, String rawPin, String idempotencyKey) {
        
    	// . IDEMPOTENCY CHECK (Sab se pehle)
        Optional<Transaction> existingTxn = transactionRepo.findByIdempotencyKey(idempotencyKey);
        if (existingTxn.isPresent()) {
            return "Duplicate Request! Transaction already successful. Trx ID: " + existingTxn.get().getTrxId();
        }
        
        // . PIN Check & Sender Validation
        User sender = userRepo.findByMobileNumber(senderMobile)
                .orElseThrow(() -> new RuntimeException("Sender account nahi mila!"));

        if (sender.getTransactionPin() == null) {
            throw new RuntimeException("Pehle Transaction PIN set karein!");
        }

        if (!passwordEncoder.matches(rawPin, sender.getTransactionPin())) {
            throw new RuntimeException("Ghalat Transaction PIN dala hai!");
        }

        // . Wallet IDs nikalna (Using custom query in WalletRepository)
        Wallet senderWallet = walletRepo.findByMobile(senderMobile)
                .orElseThrow(() -> new RuntimeException("Sender ka wallet nahi mila!"));
        
        Wallet receiverWallet = walletRepo.findByMobile(receiverMobile)
                .orElseThrow(() -> new RuntimeException("Receiver ka wallet nahi mila!"));

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new RuntimeException("Apne hi wallet mein paise nahi bhej sakte!");
        }

        // . Balance Check (BigDecimal)
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Bhai balance kam hai! Current: " + senderWallet.getBalance());
        }

        // . Daily Limit Check (50,000 PKR)
        UserLimit limit = limitRepo.findByMobileNumber(senderMobile)
                .orElse(UserLimit.builder()
                        .mobileNumber(senderMobile)
                        .dailySpent(BigDecimal.ZERO)
                        .lastTransactionDate(LocalDate.now())
                        .build());

        // Date check: Agar naya din hai to limit reset karo
        if (!limit.getLastTransactionDate().equals(LocalDate.now())) {
            limit.setDailySpent(BigDecimal.ZERO);
            limit.setLastTransactionDate(LocalDate.now());
        }

        BigDecimal totalSpentToday = limit.getDailySpent().add(amount);
        BigDecimal maxLimit = new BigDecimal("50000.00");

        if (totalSpentToday.compareTo(maxLimit) > 0) {
            BigDecimal remaining = maxLimit.subtract(limit.getDailySpent());
            throw new RuntimeException("Daily limit cross ho gayi! Baqi limit: " + remaining);
        }

        // . Money Transfer (Actual Balance Updates)
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        // . Transaction Table Entry (Aapki actual Entity ke mutabiq)
        Transaction txn = new Transaction();
        String uniqueTrxId = "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        txn.setTrxId(uniqueTrxId);
        txn.setIdempotencyKey(idempotencyKey);
        txn.setSenderWalletId(senderWallet.getId());
        txn.setReceiverWalletId(receiverWallet.getId());
        txn.setAmount(amount);
        txn.setStatus(TransactionStatus.SUCCESS); // ENUM: SUCCESS
        txn.setType("WALLET_TRANSFER");
        txn.setDescription("Transfer to " + receiverMobile);
        txn.setCreatedAt(LocalDateTime.now());

        // . Limit Update
        limit.setDailySpent(totalSpentToday);

        // . DB Saves
        walletRepo.save(senderWallet);
        walletRepo.save(receiverWallet);
        limitRepo.save(limit);
        transactionRepo.save(txn);
        
        // Sender ko alert bhejna
        if (sender.getEmail() != null) {
            emailService.sendTransactionAlert(sender.getEmail(), "DEBIT", amount, uniqueTrxId);
        }

        // Receiver ko alert bhejna
        User receiver = userRepo.findByMobileNumber(receiverMobile).orElse(null);
        if (receiver != null && receiver.getEmail() != null) {
            emailService.sendTransactionAlert(receiver.getEmail(), "CREDIT", amount, uniqueTrxId);
        }

        return "Mubarak ho! " + amount + " PKR bhej diye gaye. Trx ID: " + uniqueTrxId;
    }
    
    public BigDecimal getBalanceByMobile(String mobile) {
        User user = userRepo.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found with mobile: " + mobile));
        
        Wallet wallet = walletRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for this user"));
        
        return wallet.getBalance();
    }
    
    public List<TransactionHistoryDTO> getTransactionHistory(String mobileNumber) {
        User user = userRepo.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Long walletId = wallet.getId();

        List<Transaction> transactions = transactionRepo
                .findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(walletId, walletId);

        return transactions.stream()
                .map(trx -> toHistoryDto(trx, walletId, mobileNumber))
                .collect(Collectors.toList());
    }

    private TransactionHistoryDTO toHistoryDto(Transaction trx, Long userWalletId, String userMobile) {
        String trxType = trx.getType() != null ? trx.getType() : "WALLET_TRANSFER";
        boolean isBankDeposit = "BANK_DEPOSIT".equalsIgnoreCase(trxType);
        boolean isReceiver = userWalletId.equals(trx.getReceiverWalletId());
        boolean isSender = trx.getSenderWalletId() != null && userWalletId.equals(trx.getSenderWalletId());

        String displayType;
        String otherParty;

        if (isBankDeposit && isReceiver) {
            displayType = "ADD_MONEY";
            otherParty = trx.getDescription() != null && !trx.getDescription().isBlank()
                    ? trx.getDescription()
                    : "Bank deposit";
        } else if (isSender) {
            displayType = "SENT";
            otherParty = resolveCounterpartyMobile(trx.getReceiverWalletId(), "Receiver");
        } else if (isReceiver) {
            displayType = "RECEIVED";
            otherParty = resolveCounterpartyMobile(trx.getSenderWalletId(), "Sender");
        } else {
            displayType = "RECEIVED";
            otherParty = trx.getDescription() != null ? trx.getDescription() : "Transaction";
        }

        return TransactionHistoryDTO.builder()
                .trxId(trx.getTrxId())
                .type(displayType)
                .amount(trx.getAmount())
                .status(trx.getStatus().toString())
                .date(trx.getCreatedAt())
                .otherPartyMobile(otherParty)
                .build();
    }

    private String resolveCounterpartyMobile(Long walletId, String fallback) {
        if (walletId == null) {
            return fallback;
        }
        return walletRepo.findById(walletId)
                .flatMap(w -> userRepo.findById(w.getUserId()))
                .map(User::getMobileNumber)
                .filter(m -> !SystemWalletService.GATEWAY_MOBILE.equals(m))
                .orElseGet(() -> walletRepo.findById(walletId)
                        .map(Wallet::getWalletAccountNumber)
                        .orElse(fallback));
    }
    
    public String transferFundsByMobile(String senderMobile, String receiverMobile, BigDecimal amount) {
        User sender = userRepo.findByMobileNumber(senderMobile)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepo.findByMobileNumber(receiverMobile)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Wallet senderWallet = walletRepo.findByUserId(sender.getId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
        Wallet receiverWallet = walletRepo.findByUserId(receiver.getId())
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        return transferFunds(senderWallet.getId(), receiverWallet.getId(), amount);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public String transferFunds(Long senderWalletId, Long receiverWalletId, BigDecimal amount) {
        log.info("[START] Transfer: From Wallet {} to Wallet {} | Amount: {}", senderWalletId, receiverWalletId, amount);

        Wallet sender = walletRepo.findById(senderWalletId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
        Wallet receiver = walletRepo.findById(receiverWalletId)
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        if (sender.getBalance().compareTo(amount) < 0) {
            log.error("[FAILED] Low balance for wallet: {}", senderWalletId);
            throw new RuntimeException("Incomplete transaction: Low Balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        walletRepo.save(sender);
        walletRepo.save(receiver);

        String uniqueTrxId = "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction trx = new Transaction();
        trx.setTrxId(uniqueTrxId);
        trx.setSenderWalletId(senderWalletId);
        trx.setReceiverWalletId(receiverWalletId);
        trx.setAmount(amount);
        trx.setStatus(TransactionStatus.SUCCESS);
        trx.setType("WALLET_TRANSFER");
        trx.setCreatedAt(LocalDateTime.now());
        transactionRepo.save(trx);

        log.info("[SUCCESS] Trx ID: {}", trx.getTrxId());
        return trx.getTrxId();
    }
}
