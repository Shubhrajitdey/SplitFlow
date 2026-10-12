package com.splitflow.infrastructure.adapter.persistence.repository;

import com.splitflow.domain.model.SplitStatus;
import com.splitflow.infrastructure.adapter.persistence.entity.SplitJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSplitRepository extends JpaRepository<SplitJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SplitJpaEntity s WHERE s.id = :id")
    Optional<SplitJpaEntity> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT s FROM SplitJpaEntity s WHERE s.debtorId = :debtorId AND s.creditorId = :creditorId AND s.status IN :statuses ORDER BY s.createdAt ASC")
    List<SplitJpaEntity> findOpenSplits(
            @Param("debtorId") UUID debtorId,
            @Param("creditorId") UUID creditorId,
            @Param("statuses") List<SplitStatus> statuses
    );
}
