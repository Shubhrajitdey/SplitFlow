package com.splitflow.domain.service;

import com.splitflow.domain.model.Split;
import com.splitflow.domain.model.SplitStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitStateMachineAndDisputeTest {

    @Test
    @DisplayName("AC-2: Dispute Circuit Breaker - One turn only, permanent lockout once creditor resolves")
    void testDisputeCircuitBreaker() {
        UUID creditorId = UUID.randomUUID();
        UUID debtorId = UUID.randomUUID();
        Split split = Split.createNew("🍕 Cheesy-Blast", creditorId, debtorId, new BigDecimal("1200.00"));

        assertThat(split.getDisputeCount()).isEqualTo(0);
        assertThat(split.isDisputeLocked()).isFalse();

        // 1. Debtor disputes the split for the 1st time
        split.raiseDispute("Wrong total");
        assertThat(split.getDisputeCount()).isEqualTo(1);
        assertThat(split.getStatus()).isEqualTo(SplitStatus.DISPUTED);

        // 2. Debtor cannot dispute again while already in dispute
        assertThatThrownBy(() -> split.raiseDispute("Another dispute"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dispute can only be triggered once per split");

        // 3. Creditor resolves the dispute by adjusting amount to 1000
        split.resolveDisputeWithAdjustment(new BigDecimal("1000.00"));

        assertThat(split.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(split.getStatus()).isEqualTo(SplitStatus.PENDING);
        assertThat(split.isDisputeLocked()).isTrue();

        // 4. Permanent Dispute Lockout: Debtor attempts to dispute again
        assertThatThrownBy(() -> split.raiseDispute("Try again"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dispute window is permanently closed");
    }

    @Test
    @DisplayName("Double-Handshake: Moves to SETTLED only after creditor confirms")
    void testDoubleHandshakeSettlement() {
        UUID creditorId = UUID.randomUUID();
        UUID debtorId = UUID.randomUUID();
        Split split = Split.createNew("🍕 Cheesy-Blast", creditorId, debtorId, new BigDecimal("500.00"));

        // Debtor claims paid
        boolean firstClaim = split.claimPaid();
        assertThat(firstClaim).isTrue();
        assertThat(split.getStatus()).isEqualTo(SplitStatus.CLAIMED_PAID);

        // Creditor confirms receipt
        split.confirmSettled();
        assertThat(split.getStatus()).isEqualTo(SplitStatus.SETTLED);
        assertThat(split.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
