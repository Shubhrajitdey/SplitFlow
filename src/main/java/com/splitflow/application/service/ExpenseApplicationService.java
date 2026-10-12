package com.splitflow.application.service;

import com.splitflow.domain.model.PairwiseLedger;
import com.splitflow.domain.model.ParsedExpense;
import com.splitflow.domain.model.Split;
import com.splitflow.domain.model.SplitType;
import com.splitflow.domain.model.User;
import com.splitflow.domain.port.in.ClaimPaymentUseCase;
import com.splitflow.domain.port.in.ConfirmSettlementUseCase;
import com.splitflow.domain.port.in.DisputeSplitUseCase;
import com.splitflow.domain.port.in.LogExpenseUseCase;
import com.splitflow.domain.port.in.ResolveDisputeUseCase;
import com.splitflow.domain.port.in.SettleBalanceUseCase;
import com.splitflow.domain.port.out.IdempotencyLockPort;
import com.splitflow.domain.port.out.MessagingGatewayPort;
import com.splitflow.domain.port.out.PairwiseLedgerRepositoryPort;
import com.splitflow.domain.port.out.SplitRepositoryPort;
import com.splitflow.domain.port.out.UserRepositoryPort;
import com.splitflow.domain.service.FifoWaterfallSettlementEngine;
import com.splitflow.domain.service.FunkyNameGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseApplicationService implements
        LogExpenseUseCase,
        ClaimPaymentUseCase,
        ConfirmSettlementUseCase,
        DisputeSplitUseCase,
        ResolveDisputeUseCase,
        SettleBalanceUseCase {

    private final SplitRepositoryPort splitRepository;
    private final PairwiseLedgerRepositoryPort ledgerRepository;
    private final UserRepositoryPort userRepository;
    private final MessagingGatewayPort messagingGateway;
    private final IdempotencyLockPort idempotencyLock;
    private final FifoWaterfallSettlementEngine fifoEngine = new FifoWaterfallSettlementEngine();
    private final FunkyNameGenerator funkyNameGenerator = new FunkyNameGenerator();

    public ExpenseApplicationService(
            SplitRepositoryPort splitRepository,
            PairwiseLedgerRepositoryPort ledgerRepository,
            UserRepositoryPort userRepository,
            MessagingGatewayPort messagingGateway,
            IdempotencyLockPort idempotencyLock) {
        this.splitRepository = splitRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.messagingGateway = messagingGateway;
        this.idempotencyLock = idempotencyLock;
    }

    @Override
    @Transactional
    public Split logExpense(String creditorPhone, ParsedExpense parsed) {
        User creditor = getOrCreateUser(creditorPhone, "Creditor");

        // Resolve counterparty alias
        String debtorPhone = creditor.resolveContactAlias(parsed.counterpartyAlias());
        if (debtorPhone == null) {
            debtorPhone = parsed.counterpartyAlias(); // Fallback if number or unaliased
        }
        User debtor = getOrCreateUser(debtorPhone, parsed.counterpartyAlias());

        // Determine split amounts
        BigDecimal totalBill = parsed.amount();
        BigDecimal debtorShare;
        if (parsed.splitType() == SplitType.EQUAL_SPLIT) {
            // Equal 50/50 split
            debtorShare = totalBill.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            // Full amount
            debtorShare = totalBill;
        }

        // Feature 1.1 Dual-Layer Naming: User Provided or Funky Autonomous Generator
        String displayName = (parsed.userTitle() != null && !parsed.userTitle().isBlank())
                ? parsed.userTitle().trim()
                : funkyNameGenerator.generate();

        // Check if Debtor has an existing Credit Advance with Creditor
        PairwiseLedger ledger = getOrCreateLedger(creditor.getId(), debtor.getId());
        PairwiseLedger.OffsetResult offsetResult = ledger.offsetNewExpense(debtor.getId(), debtorShare);
        ledgerRepository.save(ledger);

        BigDecimal effectiveDebtorDebt = offsetResult.remainingBillAmount();
        BigDecimal creditApplied = offsetResult.creditApplied();

        Split split = Split.createNew(displayName, creditor.getId(), debtor.getId(), debtorShare);
        if (creditApplied.compareTo(BigDecimal.ZERO) > 0) {
            // Offset applied automatically
            split.applyPartialPayment(creditApplied);
        }
        Split savedSplit = splitRepository.save(split);

        // Notify Creditor
        if (creditApplied.compareTo(BigDecimal.ZERO) > 0) {
            messagingGateway.sendTextMessage(creditorPhone,
                    "Logged " + displayName + " for ₹" + totalBill + ".\nApplied ₹" + creditApplied +
                            " existing credit advance with " + debtor.getDisplayName() + ". They now owe ₹" + effectiveDebtorDebt + ".");
        } else {
            messagingGateway.sendTextMessage(creditorPhone,
                    "Logged " + displayName + " for ₹" + totalBill + ".\n" + debtor.getDisplayName() + " owes ₹" + effectiveDebtorDebt + ".");
        }

        // Notify Debtor with WhatsApp Interactive Card (AC-6: technical UUID hidden in button payload)
        messagingGateway.sendSplitCard(debtorPhone, savedSplit, creditor.getDisplayName());

        return savedSplit;
    }

    @Override
    @Transactional
    public boolean claimPayment(UUID splitId, String debtorPhone) {
        String lockKey = "LOCK:CLAIM:" + splitId;
        // 1. Anti-Duplicate Concurrency Shield (AC-1)
        if (!idempotencyLock.tryAcquire(lockKey, 5)) {
            messagingGateway.sendTextMessage(debtorPhone, "We've already registered your claim for this bill. Awaiting creditor confirmation.");
            return false;
        }

        try {
            // 2. Fetch with Pessimistic DB Lock
            Split split = splitRepository.findByIdWithLock(splitId)
                    .orElseThrow(() -> new IllegalArgumentException("Split not found: " + splitId));

            // State Machine check
            boolean newlyClaimed = split.claimPaid();
            if (!newlyClaimed) {
                // Was already in CLAIMED_PAID
                messagingGateway.sendTextMessage(debtorPhone, "We've already registered your claim for this bill. Awaiting creditor confirmation.");
                return false;
            }

            splitRepository.save(split);

            User creditor = userRepository.findById(split.getCreditorId()).orElseThrow();
            User debtor = userRepository.findById(split.getDebtorId()).orElseThrow();

            // Notify debtor of success
            messagingGateway.sendTextMessage(debtorPhone, "Got it! We notified " + creditor.getDisplayName() + " to verify your payment.");

            // Notify Creditor with Verification Card (Double-Handshake AC-1 / AC-5)
            messagingGateway.sendClaimVerificationCard(creditor.getPhoneNumber(), split, debtor.getDisplayName());

            return true;
        } finally {
            idempotencyLock.release(lockKey);
        }
    }

    @Override
    @Transactional
    public void confirmSettlement(UUID splitId, String creditorPhone, boolean confirmed) {
        Split split = splitRepository.findByIdWithLock(splitId)
                .orElseThrow(() -> new IllegalArgumentException("Split not found: " + splitId));

        User creditor = userRepository.findById(split.getCreditorId()).orElseThrow();
        User debtor = userRepository.findById(split.getDebtorId()).orElseThrow();

        if (confirmed) {
            // Double-Handshake: Creditor verified
            split.confirmSettled();
            splitRepository.save(split);

            messagingGateway.sendTextMessage(creditorPhone, "Confirmed! " + split.getDisplayName() + " is marked as settled.");
            messagingGateway.sendTextMessage(debtor.getPhoneNumber(), "🎉 " + creditor.getDisplayName() + " confirmed your payment for " + split.getDisplayName() + ". All settled!");
        } else {
            // Creditor says Not Received
            split.rejectPaymentClaim();
            splitRepository.save(split);

            messagingGateway.sendTextMessage(creditorPhone, "Marked as not received. We notified " + debtor.getDisplayName() + ".");
            messagingGateway.sendTextMessage(debtor.getPhoneNumber(), "⚠️ " + creditor.getDisplayName() + " indicated they did not receive the payment for " + split.getDisplayName() + ". Please check your bank/UPI transaction.");
        }
    }

    @Override
    @Transactional
    public void disputeSplit(UUID splitId, String debtorPhone, String reason) {
        Split split = splitRepository.findByIdWithLock(splitId)
                .orElseThrow(() -> new IllegalArgumentException("Split not found: " + splitId));

        // Enforce Single-Turn Dispute Circuit Breaker (AC-2)
        split.raiseDispute(reason);
        splitRepository.save(split);

        User creditor = userRepository.findById(split.getCreditorId()).orElseThrow();
        User debtor = userRepository.findById(split.getDebtorId()).orElseThrow();

        messagingGateway.sendTextMessage(debtorPhone, "Dispute registered. We paused auto-reminders and notified " + creditor.getDisplayName() + ".");
        messagingGateway.sendDisputeNotificationToCreditor(creditor.getPhoneNumber(), split, debtor.getDisplayName(), reason);
    }

    @Override
    @Transactional
    public void resolveDispute(UUID splitId, String creditorPhone, boolean adjustAmount, BigDecimal correctedAmount) {
        Split split = splitRepository.findByIdWithLock(splitId)
                .orElseThrow(() -> new IllegalArgumentException("Split not found: " + splitId));

        User creditor = userRepository.findById(split.getCreditorId()).orElseThrow();
        User debtor = userRepository.findById(split.getDebtorId()).orElseThrow();

        if (adjustAmount && correctedAmount != null) {
            split.resolveDisputeWithAdjustment(correctedAmount);
        } else {
            split.resolveDisputeWithRejection();
        }

        // AC-2: Dispute is permanently locked now
        splitRepository.save(split);

        messagingGateway.sendTextMessage(creditorPhone, "Dispute resolved. Dispute option is now locked for this bill.");

        // Debtor receives final card without dispute option (AC-2)
        messagingGateway.sendSplitCard(debtor.getPhoneNumber(), split, creditor.getDisplayName());
    }

    @Override
    @Transactional
    public FifoWaterfallSettlementEngine.SettlementResult settleWithFifo(String debtorPhone, String creditorPhone, BigDecimal amount) {
        User debtor = getOrCreateUser(debtorPhone, "Debtor");
        User creditor = getOrCreateUser(creditorPhone, "Creditor");

        PairwiseLedger ledger = getOrCreateLedger(creditor.getId(), debtor.getId());
        List<Split> openSplits = splitRepository.findOpenSplits(debtor.getId(), creditor.getId());

        // Execute Scenario A (FIFO) & Scenario B (Overpayment Credit Advance)
        FifoWaterfallSettlementEngine.SettlementResult result = fifoEngine.applyPayment(openSplits, amount, ledger, debtor.getId());

        for (Split cleared : result.fullyClearedSplits()) {
            splitRepository.save(cleared);
        }
        if (result.partiallyPaidSplit() != null) {
            splitRepository.save(result.partiallyPaidSplit());
        }
        ledgerRepository.save(ledger);

        // Feedback messaging
        StringBuilder sb = new StringBuilder();
        sb.append("Applied ₹").append(result.totalApplied()).append(" across your debts to ").append(creditor.getDisplayName()).append(".\n");
        if (!result.fullyClearedSplits().isEmpty()) {
            sb.append("✅ Cleared: ").append(result.fullyClearedSplits().size()).append(" bills.\n");
        }
        if (result.partiallyPaidSplit() != null) {
            sb.append("⏳ Partially paid: ").append(result.partiallyPaidSplit().getDisplayName())
              .append(" (₹").append(result.partiallyPaidSplit().getRemainingAmount()).append(" remaining).\n");
        }
        if (result.creditAdvanceGenerated().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("🎁 ₹").append(result.creditAdvanceGenerated()).append(" saved as Credit Advance for your next bills!\n");
        }
        sb.append("Total outstanding dues: ₹").append(result.remainingDues());

        messagingGateway.sendTextMessage(debtorPhone, sb.toString());
        messagingGateway.sendTextMessage(creditorPhone, debtor.getDisplayName() + " sent ₹" + amount + ".\n" + sb);

        return result;
    }

    private User getOrCreateUser(String phoneNumber, String defaultName) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> userRepository.save(User.createNew(phoneNumber, defaultName)));
    }

    private PairwiseLedger getOrCreateLedger(UUID user1, UUID user2) {
        return ledgerRepository.findByUsers(user1, user2)
                .orElseGet(() -> ledgerRepository.save(PairwiseLedger.createNew(user1, user2)));
    }
}
