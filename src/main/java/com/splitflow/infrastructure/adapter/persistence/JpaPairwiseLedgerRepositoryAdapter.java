package com.splitflow.infrastructure.adapter.persistence;

import com.splitflow.domain.model.PairwiseLedger;
import com.splitflow.domain.port.out.PairwiseLedgerRepositoryPort;
import com.splitflow.infrastructure.adapter.persistence.entity.PairwiseLedgerJpaEntity;
import com.splitflow.infrastructure.adapter.persistence.mapper.LedgerMapper;
import com.splitflow.infrastructure.adapter.persistence.repository.SpringDataLedgerRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPairwiseLedgerRepositoryAdapter implements PairwiseLedgerRepositoryPort {

    private final SpringDataLedgerRepository repository;

    public JpaPairwiseLedgerRepositoryAdapter(SpringDataLedgerRepository repository) {
        this.repository = repository;
    }

    @Override
    public PairwiseLedger save(PairwiseLedger ledger) {
        PairwiseLedgerJpaEntity entity = LedgerMapper.toEntity(ledger);
        PairwiseLedgerJpaEntity saved = repository.save(entity);
        return LedgerMapper.toDomain(saved);
    }

    @Override
    public Optional<PairwiseLedger> findByUsers(UUID userAId, UUID userBId) {
        return repository.findByUsers(userAId, userBId).map(LedgerMapper::toDomain);
    }
}
