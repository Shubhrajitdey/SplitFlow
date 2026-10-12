package com.splitflow.domain.service;

import com.splitflow.domain.model.PairwiseLedger;
import com.splitflow.domain.model.Split;
import com.splitflow.domain.model.SplitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FifoWaterfallSettlementEngineTest {

    private final FifoWaterfallSettlementEngine engine = new FifoWaterfallSettlementEngine();

    @Test
    @DisplayName("AC-3: FIFO Partial Payment - ₹300 pays off ₹200 and leaves ₹400 on ₹500 bill")
    void testFifoPartialPaymentScenarioA() {
        UUID creditorId = UUID.randomUUID();
        UUID debtorId = UUID.randomUUID();
        PairwiseLedger ledger = PairwiseLedger.createNew(creditorId, debtorId);

        // Bill 1: ₹200 created first
        Split bill1 = Split.createNew("🍕 Cheesy-Blast", creditorId, debtorId, new BigDecimal("200.00"));
        // Bill 2: ₹500 created later
        Split bill2 = Split.createNew("🚕 Turbo-Cab", creditorId, debtorId, new BigDecimal("500.00"));

        List<Split> openSplits = List.of(bill1, bill2);

        // Debtor pays lump-sum ₹300
        FifoWaterfallSettlementEngine.SettlementResult result = engine.applyPayment(
                openSplits, new BigDecimal("300.00"), ledger, debtorId
        );

        // Bill 1 should be fully covered
        assertThat(result.fullyClearedSplits()).hasSize(1);
        assertThat(result.fullyClearedSplits().get(0).getId()).isEqualTo(bill1.getId());
        assertThat(bill1.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill1.getStatus()).isEqualTo(SplitStatus.CLAIMED_PAID);

        // Bill 2 should be partially covered with ₹400 remaining
        assertThat(result.partiallyPaidSplit()).isNotNull();
        assertThat(result.partiallyPaidSplit().getId()).isEqualTo(bill2.getId());
        assertThat(bill2.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(bill2.getStatus()).isEqualTo(SplitStatus.PARTIALLY_PAID);

        // Total remaining dues across remaining bills should be ₹400
        assertThat(result.remainingDues()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(result.creditAdvanceGenerated()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("AC-4: Overpayment Credit - Paying ₹700 for ₹600 debt clears all splits and leaves ₹100 credit advance")
    void testOverpaymentCreditScenarioB() {
        UUID creditorId = UUID.randomUUID();
        UUID debtorId = UUID.randomUUID();
        PairwiseLedger ledger = PairwiseLedger.createNew(creditorId, debtorId);

        // Open bill of ₹600
        Split bill = Split.createNew("🍔 Burger-Binge", creditorId, debtorId, new BigDecimal("600.00"));

        // Debtor pays ₹700
        FifoWaterfallSettlementEngine.SettlementResult result = engine.applyPayment(
                List.of(bill), new BigDecimal("700.00"), ledger, debtorId
        );

        // Bill is cleared
        assertThat(bill.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bill.getStatus()).isEqualTo(SplitStatus.CLAIMED_PAID);

        // ₹100 credit advance generated without reverse splits
        assertThat(result.creditAdvanceGenerated()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(ledger.getCreditAdvanceFor(debtorId)).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.remainingDues()).isEqualByComparingTo(BigDecimal.ZERO);

        // Subsequent bill offset: Next expense ₹250
        PairwiseLedger.OffsetResult offset = ledger.offsetNewExpense(debtorId, new BigDecimal("250.00"));
        assertThat(offset.creditApplied()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(offset.remainingBillAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(ledger.getCreditAdvanceFor(debtorId)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
