package com.splitflow.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "idempotency_locks")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyLockJpaEntity {

    @Id
    private String lockKey;

    @Column(nullable = false)
    private Instant expiresAt;

    public IdempotencyLockJpaEntity(String lockKey, Instant expiresAt) {
        this.lockKey = lockKey;
        this.expiresAt = expiresAt;
    }
}
