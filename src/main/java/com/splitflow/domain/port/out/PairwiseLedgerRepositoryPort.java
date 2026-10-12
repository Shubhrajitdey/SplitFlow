package com.splitflow.domain.port.out;

import com.splitflow.domain.model.PairwiseLedger;
import java.util.Optional;
import java.util.UUID;

public interface PairwiseLedgerRepositoryPort {
    PairwiseLedger save(PairwiseLedger ledger);
    Optional<PairwiseLedger> findByUsers(UUID userAId, UUID userBId);
}
