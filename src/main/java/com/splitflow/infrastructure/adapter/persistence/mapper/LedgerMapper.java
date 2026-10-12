package com.splitflow.infrastructure.adapter.persistence.mapper;

import com.splitflow.domain.model.PairwiseLedger;
import com.splitflow.infrastructure.adapter.persistence.entity.PairwiseLedgerJpaEntity;

public class LedgerMapper {

    public static PairwiseLedger toDomain(PairwiseLedgerJpaEntity entity) {
        if (entity == null) return null;
        return new PairwiseLedger(
                entity.getId(),
                entity.getUserAId(),
                entity.getUserBId(),
                entity.getNetBalance(),
                entity.getUserACreditAdvance(),
                entity.getUserBCreditAdvance(),
                entity.getUpdatedAt()
        );
    }

    public static PairwiseLedgerJpaEntity toEntity(PairwiseLedger domain) {
        if (domain == null) return null;
        PairwiseLedgerJpaEntity entity = new PairwiseLedgerJpaEntity();
        entity.setId(domain.getId());
        entity.setUserAId(domain.getUserAId());
        entity.setUserBId(domain.getUserBId());
        entity.setNetBalance(domain.getNetBalance());
        entity.setUserACreditAdvance(domain.getUserACreditAdvance());
        entity.setUserBCreditAdvance(domain.getUserBCreditAdvance());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
