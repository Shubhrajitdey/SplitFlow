package com.splitflow.domain.port.in;

import com.splitflow.domain.service.FifoWaterfallSettlementEngine;

import java.math.BigDecimal;

public interface SettleBalanceUseCase {
    FifoWaterfallSettlementEngine.SettlementResult settleWithFifo(String debtorPhone, String creditorPhone, BigDecimal amount);
}
