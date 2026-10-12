package com.splitflow.domain.port.out;

import com.splitflow.domain.model.Split;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SplitRepositoryPort {
    Split save(Split split);
    Optional<Split> findById(UUID id);
    Optional<Split> findByIdWithLock(UUID id); // Pessimistic lock for Concurrency Shield
    List<Split> findOpenSplits(UUID debtorId, UUID creditorId);
}
