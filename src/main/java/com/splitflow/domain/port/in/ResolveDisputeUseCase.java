package com.splitflow.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ResolveDisputeUseCase {
    void resolveDispute(UUID splitId, String creditorPhone, boolean adjustAmount, BigDecimal correctedAmount);
}
