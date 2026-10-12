package com.splitflow.infrastructure.adapter.persistence.repository;

import com.splitflow.infrastructure.adapter.persistence.entity.IdempotencyLockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface SpringDataLockRepository extends JpaRepository<IdempotencyLockJpaEntity, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyLockJpaEntity l WHERE l.expiresAt < :now")
    void deleteExpiredLocks(@Param("now") Instant now);
}
