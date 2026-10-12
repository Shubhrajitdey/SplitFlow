package com.splitflow.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PairwiseLedger {
    private final UUID id;
    private final UUID userAId;
    private final UUID userBId;
    private BigDecimal netBalance; // > 0 means User A owes User B; < 0 means User B owes User A
    private BigDecimal userACreditAdvance; // Wallet advance of User A with User B
    private BigDecimal userBCreditAdvance; // Wallet advance of User B with User A
    private Instant updatedAt;

    public PairwiseLedger(UUID id, UUID userAId, UUID userBId, BigDecimal netBalance,
                          BigDecimal userACreditAdvance, BigDecimal userBCreditAdvance, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Ledger ID cannot be null");
        this.userAId = Objects.requireNonNull(userAId, "User A ID cannot be null");
        this.userBId = Objects.requireNonNull(userBId, "User B ID cannot be null");
        this.netBalance = netBalance != null ? netBalance : BigDecimal.ZERO;
        this.userACreditAdvance = userACreditAdvance != null ? userACreditAdvance : BigDecimal.ZERO;
        this.userBCreditAdvance = userBCreditAdvance != null ? userBCreditAdvance : BigDecimal.ZERO;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static PairwiseLedger createNew(UUID userAId, UUID userBId) {
        // Enforce deterministic ordering (e.g., smaller UUID first)
        UUID first = userAId.compareTo(userBId) < 0 ? userAId : userBId;
        UUID second = userAId.compareTo(userBId) < 0 ? userBId : userAId;
        return new PairwiseLedger(UUID.randomUUID(), first, second, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
    }

    public BigDecimal getCreditAdvanceFor(UUID userId) {
        if (userId.equals(userAId)) {
            return userACreditAdvance;
        } else if (userId.equals(userBId)) {
            return userBCreditAdvance;
        }
        throw new IllegalArgumentException("User " + userId + " is not a participant in this pairwise ledger.");
    }

    public void addCreditAdvance(UUID userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        if (userId.equals(userAId)) {
            this.userACreditAdvance = this.userACreditAdvance.add(amount);
        } else if (userId.equals(userBId)) {
            this.userBCreditAdvance = this.userBCreditAdvance.add(amount);
        } else {
            throw new IllegalArgumentException("User " + userId + " does not belong to this ledger.");
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Applies credit advance to offset a new bill.
     * @return remaining bill amount after applying credit advance.
     */
    public OffsetResult offsetNewExpense(UUID debtorId, BigDecimal expenseAmount) {
        BigDecimal credit = getCreditAdvanceFor(debtorId);
        if (credit.compareTo(BigDecimal.ZERO) <= 0) {
            return new OffsetResult(expenseAmount, BigDecimal.ZERO);
        }

        if (credit.compareTo(expenseAmount) >= 0) {
            // Credit covers the entire bill!
            BigDecimal newCredit = credit.subtract(expenseAmount);
            setCreditAdvance(debtorId, newCredit);
            this.updatedAt = Instant.now();
            return new OffsetResult(BigDecimal.ZERO, expenseAmount);
        } else {
            // Credit covers part of the bill
            BigDecimal remainingExpense = expenseAmount.subtract(credit);
            setCreditAdvance(debtorId, BigDecimal.ZERO);
            this.updatedAt = Instant.now();
            return new OffsetResult(remainingExpense, credit);
        }
    }

    private void setCreditAdvance(UUID userId, BigDecimal amount) {
        if (userId.equals(userAId)) {
            this.userACreditAdvance = amount;
        } else {
            this.userBCreditAdvance = amount;
        }
    }

    public record OffsetResult(BigDecimal remainingBillAmount, BigDecimal creditApplied) {}

    // Getters
    public UUID getId() { return id; }
    public UUID getUserAId() { return userAId; }
    public UUID getUserBId() { return userBId; }
    public BigDecimal getNetBalance() { return netBalance; }
    public BigDecimal getUserACreditAdvance() { return userACreditAdvance; }
    public BigDecimal getUserBCreditAdvance() { return userBCreditAdvance; }
    public Instant getUpdatedAt() { return updatedAt; }
}
