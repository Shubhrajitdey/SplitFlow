package com.splitflow.infrastructure;

import com.splitflow.domain.model.ParsedExpense;
import com.splitflow.domain.model.Split;
import com.splitflow.domain.model.SplitStatus;
import com.splitflow.domain.port.in.ClaimPaymentUseCase;
import com.splitflow.domain.port.in.LogExpenseUseCase;
import com.splitflow.domain.port.in.ProcessIncomingMessageUseCase;
import com.splitflow.domain.port.out.SplitRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ConcurrencyShieldAndGuardrailsIntegrationTest {

    @Autowired
    private LogExpenseUseCase logExpenseUseCase;

    @Autowired
    private ClaimPaymentUseCase claimPaymentUseCase;

    @Autowired
    private SplitRepositoryPort splitRepository;

    @Autowired
    private ProcessIncomingMessageUseCase processIncomingMessageUseCase;

    @Test
    @DisplayName("AC-1: Duplicate Click Shield - 5 rapid concurrent clicks yield exactly 1 claim")
    void testAntiDuplicateConcurrencyShield() throws InterruptedException {
        String creditorPhone = "+919876543210";
        String debtorPhone = "+919123456789";

        // Log an expense
        ParsedExpense expense = ParsedExpense.logExpense(
                new BigDecimal("1200.00"), debtorPhone, "Pizza", com.splitflow.domain.model.SplitType.EQUAL_SPLIT, "Paid 1200 for Pizza with " + debtorPhone
        );
        Split split = logExpenseUseCase.logExpense(creditorPhone, expense);
        UUID splitId = split.getId();

        // Simulate 5 simultaneous clicks on "I've Paid"
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successClaims = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    boolean success = claimPaymentUseCase.claimPayment(splitId, debtorPhone);
                    if (success) {
                        successClaims.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly 1 thread must win and transition state to CLAIMED_PAID
        assertThat(successClaims.get()).isEqualTo(1);

        // Split in DB must be in CLAIMED_PAID
        Split updated = splitRepository.findById(splitId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SplitStatus.CLAIMED_PAID);
    }

    @Test
    @DisplayName("AC-6: Funky Identity - Bills without title get creative funky tags")
    void testFunkyIdentityOptionB() {
        String creditorPhone = "+919876543210";
        String debtorPhone = "+919123456789";

        // User says "Paid 1200 with Rahul" (title is null)
        ParsedExpense expenseWithoutTitle = ParsedExpense.logExpense(
                new BigDecimal("1200.00"), debtorPhone, null, com.splitflow.domain.model.SplitType.EQUAL_SPLIT, "Paid 1200 with " + debtorPhone
        );

        Split split = logExpenseUseCase.logExpense(creditorPhone, expenseWithoutTitle);

        // Display name should not be blank and should contain a funky tag
        assertThat(split.getDisplayName()).isNotBlank();
        assertThat(split.getDisplayName()).matches(".*[🍕🌮☕🍔🍣🚕🍿🎳🍹🍪🍩🍦🍜🥑🥞🥟].*");
    }

    @Test
    @DisplayName("AC-5: Input Guardrails - Prohibited media triggers immediate refusal")
    void testProhibitedMediaGuardrail() {
        // Does not throw exceptions, invokes guardrail response
        processIncomingMessageUseCase.processProhibitedMedia("+919876543210", "image");
    }
}
