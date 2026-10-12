package com.splitflow.domain.port.in;

import java.util.UUID;

public interface ClaimPaymentUseCase {
    /**
     * @return true if claim was newly registered, false if it was already claimed (idempotent / duplicate shield)
     */
    boolean claimPayment(UUID splitId, String debtorPhone);
}
