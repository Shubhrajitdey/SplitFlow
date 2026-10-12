package com.splitflow.infrastructure.adapter.persistence.repository;

import com.splitflow.infrastructure.adapter.persistence.entity.PairwiseLedgerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataLedgerRepository extends JpaRepository<PairwiseLedgerJpaEntity, UUID> {

    @Query("SELECT l FROM PairwiseLedgerJpaEntity l WHERE (l.userAId = :u1 AND l.userBId = :u2) OR (l.userAId = :u2 AND l.userBId = :u1)")
    Optional<PairwiseLedgerJpaEntity> findByUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
