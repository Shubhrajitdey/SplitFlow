package com.splitflow.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Split {
    private final UUID id;
    private final String displayName;
    private final UUID creditorId;
    private final UUID debtorId;
    private final BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private SplitStatus status;
    private int disputeCount;
    private boolean disputeLocked;
    private final Instant createdAt;
    private Instant updatedAt;

    public Split(UUID id, String displayName, UUID creditorId, UUID debtorId,
                 BigDecimal totalAmount, BigDecimal remainingAmount, SplitStatus status,
                 int disputeCount, boolean disputeLocked, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Split ID cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.creditorId = Objects.requireNonNull(creditorId, "Creditor ID cannot be null");
        this.debtorId = Objects.requireNonNull(debtorId, "Debtor ID cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        this.remainingAmount = Objects.requireNonNull(remainingAmount, "Remaining amount cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.disputeCount = disputeCount;
        this.disputeLocked = disputeLocked;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Remaining amount cannot be negative");
        }
    }

    public static Split createNew(String displayName, UUID creditorId, UUID debtorId, BigDecimal amount) {
        return new Split(
                UUID.randomUUID(),
                displayName,
                creditorId,
                debtorId,
                amount,
                amount,
                SplitStatus.PENDING,
                0,
                false,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Debtor claims payment (Anti-duplicate concurrency shield checked here).
     */
    public boolean claimPaid() {
        if (this.status == SplitStatus.CLAIMED_PAID) {
            // Already claimed; duplicate ignored safely
            return false;
        }
        if (this.status == SplitStatus.SETTLED) {
            throw new IllegalStateException("Split is already fully settled.");
        }
        this.status = SplitStatus.CLAIMED_PAID;
        this.updatedAt = Instant.now();
        return true;
    }

    /**
     * Creditor verifies payment received -> moves to SETTLED.
     */
    public void confirmSettled() {
        this.status = SplitStatus.SETTLED;
        this.remainingAmount = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    /**
     * Creditor marks payment as NOT received -> reverts to DISPUTED or PENDING.
     */
    public void rejectPaymentClaim() {
        if (this.status != SplitStatus.CLAIMED_PAID) {
            throw new IllegalStateException("Can only reject a claim that is in CLAIMED_PAID status.");
        }
        this.status = SplitStatus.DISPUTED;
        this.updatedAt = Instant.now();
    }

    /**
     * Debtor triggers a dispute (Single-Turn Dispute Circuit Breaker).
     */
    public void raiseDispute(String reason) {
        if (this.disputeLocked) {
            throw new IllegalStateException("Dispute window is permanently closed for this bill.");
        }
        if (this.disputeCount >= 1) {
            throw new IllegalStateException("Dispute can only be triggered once per split.");
        }
        if (this.status == SplitStatus.SETTLED) {
            throw new IllegalStateException("Cannot dispute an already settled bill.");
        }
        this.disputeCount++;
        this.status = SplitStatus.DISPUTED;
        this.updatedAt = Instant.now();
    }

    /**
     * Creditor adjusts amount after dispute. Permanent dispute lockout applied.
     */
    public void resolveDisputeWithAdjustment(BigDecimal correctedAmount) {
        if (correctedAmount.compareTo(BigDecimal.ZERO) <= 0 || correctedAmount.compareTo(this.totalAmount) > 0) {
            throw new IllegalArgumentException("Corrected amount must be positive and not exceed original amount.");
        }
        this.remainingAmount = correctedAmount;
        this.disputeLocked = true;
        this.status = SplitStatus.PENDING;
        this.updatedAt = Instant.now();
    }

    /**
     * Creditor rejects dispute (Amount is correct). Permanent dispute lockout applied.
     */
    public void resolveDisputeWithRejection() {
        this.disputeLocked = true;
        this.status = SplitStatus.PENDING;
        this.updatedAt = Instant.now();
    }

    /**
     * Applies partial payment (used in FIFO waterfall).
     */
    public BigDecimal applyPartialPayment(BigDecimal paymentAllocated) {
        if (paymentAllocated.compareTo(BigDecimal.ZERO) <= 0) {
            return paymentAllocated;
        }

        if (paymentAllocated.compareTo(this.remainingAmount) >= 0) {
            // Fully covers this split
            BigDecimal remainder = paymentAllocated.subtract(this.remainingAmount);
            this.remainingAmount = BigDecimal.ZERO;
            this.status = SplitStatus.CLAIMED_PAID;
            this.updatedAt = Instant.now();
            return remainder;
        } else {
            // Partially covers this split
            this.remainingAmount = this.remainingAmount.subtract(paymentAllocated);
            this.status = SplitStatus.PARTIALLY_PAID;
            this.updatedAt = Instant.now();
            return BigDecimal.ZERO;
        }
    }

    // Getters
    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public UUID getCreditorId() { return creditorId; }
    public UUID getDebtorId() { return debtorId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public SplitStatus getStatus() { return status; }
    public int getDisputeCount() { return disputeCount; }
    public boolean isDisputeLocked() { return disputeLocked; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
