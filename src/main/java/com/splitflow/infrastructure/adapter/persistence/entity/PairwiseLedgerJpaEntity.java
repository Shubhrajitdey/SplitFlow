package com.splitflow.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pairwise_ledgers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ledger_users", columnNames = {"userAId", "userBId"})
})
@Getter
@Setter
@NoArgsConstructor
public class PairwiseLedgerJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userAId;

    @Column(nullable = false)
    private UUID userBId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netBalance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal userACreditAdvance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal userBCreditAdvance;

    @Column(nullable = false)
    private Instant updatedAt;
}
