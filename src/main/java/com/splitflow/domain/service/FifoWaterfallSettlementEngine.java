package com.splitflow.domain.service;

import com.splitflow.domain.model.PairwiseLedger;
import com.splitflow.domain.model.Split;
import com.splitflow.domain.model.SplitStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class FifoWaterfallSettlementEngine {

    public record SettlementResult(
            List<Split> fullyClearedSplits,
            Split partiallyPaidSplit,
            BigDecimal creditAdvanceGenerated,
            BigDecimal totalApplied,
            BigDecimal remainingDues
    ) {}

    /**
     * Executes the FIFO waterfall against open splits ordered by creation time ascending.
     * Any overpayment is deposited as a credit advance into the pairwise ledger.
     */
    public SettlementResult applyPayment(List<Split> openSplits, BigDecimal paymentAmount,
                                         PairwiseLedger ledger, UUID debtorId) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        // Sort by oldest first (FIFO)
        List<Split> sorted = openSplits.stream()
                .filter(s -> s.getStatus() == SplitStatus.PENDING || s.getStatus() == SplitStatus.PARTIALLY_PAID)
                .sorted(Comparator.comparing(Split::getCreatedAt))
                .toList();

        BigDecimal availablePayment = paymentAmount;
        List<Split> fullyCleared = new ArrayList<>();
        Split partiallyPaid = null;

        for (Split split : sorted) {
            if (availablePayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal remainingOnSplit = split.getRemainingAmount();
            if (availablePayment.compareTo(remainingOnSplit) >= 0) {
                // Fully covers this split
                split.applyPartialPayment(remainingOnSplit);
                fullyCleared.add(split);
                availablePayment = availablePayment.subtract(remainingOnSplit);
            } else {
                // Partially covers this split
                split.applyPartialPayment(availablePayment);
                partiallyPaid = split;
                availablePayment = BigDecimal.ZERO;
            }
        }

        BigDecimal creditAdvance = BigDecimal.ZERO;
        if (availablePayment.compareTo(BigDecimal.ZERO) > 0) {
            // Scenario B: Overpayment -> Store in PairwiseLedger as credit advance
            creditAdvance = availablePayment;
            ledger.addCreditAdvance(debtorId, creditAdvance);
        }

        // Calculate remaining dues across all remaining unpaid splits
        BigDecimal totalRemainingDues = sorted.stream()
                .filter(s -> s.getStatus() == SplitStatus.PENDING || s.getStatus() == SplitStatus.PARTIALLY_PAID)
                .map(Split::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalApplied = paymentAmount.subtract(creditAdvance);

        return new SettlementResult(
                fullyCleared,
                partiallyPaid,
                creditAdvance,
                totalApplied,
                totalRemainingDues
        );
    }
}
