package com.splitflow.infrastructure.adapter.persistence.mapper;

import com.splitflow.domain.model.Split;
import com.splitflow.infrastructure.adapter.persistence.entity.SplitJpaEntity;

public class SplitMapper {

    public static Split toDomain(SplitJpaEntity entity) {
        if (entity == null) return null;
        return new Split(
                entity.getId(),
                entity.getDisplayName(),
                entity.getCreditorId(),
                entity.getDebtorId(),
                entity.getTotalAmount(),
                entity.getRemainingAmount(),
                entity.getStatus(),
                entity.getDisputeCount(),
                entity.isDisputeLocked(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static SplitJpaEntity toEntity(Split domain) {
        if (domain == null) return null;
        SplitJpaEntity entity = new SplitJpaEntity();
        entity.setId(domain.getId());
        entity.setDisplayName(domain.getDisplayName());
        entity.setCreditorId(domain.getCreditorId());
        entity.setDebtorId(domain.getDebtorId());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setRemainingAmount(domain.getRemainingAmount());
        entity.setStatus(domain.getStatus());
        entity.setDisputeCount(domain.getDisputeCount());
        entity.setDisputeLocked(domain.isDisputeLocked());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
