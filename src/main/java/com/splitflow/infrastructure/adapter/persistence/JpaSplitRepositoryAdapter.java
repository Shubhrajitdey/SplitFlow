package com.splitflow.infrastructure.adapter.persistence;

import com.splitflow.domain.model.Split;
import com.splitflow.domain.model.SplitStatus;
import com.splitflow.domain.port.out.SplitRepositoryPort;
import com.splitflow.infrastructure.adapter.persistence.entity.SplitJpaEntity;
import com.splitflow.infrastructure.adapter.persistence.mapper.SplitMapper;
import com.splitflow.infrastructure.adapter.persistence.repository.SpringDataSplitRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaSplitRepositoryAdapter implements SplitRepositoryPort {

    private final SpringDataSplitRepository repository;

    public JpaSplitRepositoryAdapter(SpringDataSplitRepository repository) {
        this.repository = repository;
    }

    @Override
    public Split save(Split split) {
        SplitJpaEntity entity = SplitMapper.toEntity(split);
        SplitJpaEntity saved = repository.save(entity);
        return SplitMapper.toDomain(saved);
    }

    @Override
    public Optional<Split> findById(UUID id) {
        return repository.findById(id).map(SplitMapper::toDomain);
    }

    @Override
    public Optional<Split> findByIdWithLock(UUID id) {
        return repository.findByIdWithLock(id).map(SplitMapper::toDomain);
    }

    @Override
    public List<Split> findOpenSplits(UUID debtorId, UUID creditorId) {
        List<SplitStatus> openStatuses = List.of(SplitStatus.PENDING, SplitStatus.PARTIALLY_PAID);
        return repository.findOpenSplits(debtorId, creditorId, openStatuses)
                .stream()
                .map(SplitMapper::toDomain)
                .toList();
    }
}
