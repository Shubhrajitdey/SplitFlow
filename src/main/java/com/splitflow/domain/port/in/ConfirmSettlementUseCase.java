package com.splitflow.domain.port.in;

import java.util.UUID;

public interface ConfirmSettlementUseCase {
    void confirmSettlement(UUID splitId, String creditorPhone, boolean confirmed);
}
