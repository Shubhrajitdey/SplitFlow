package com.splitflow.infrastructure.adapter.persistence.entity;

import com.splitflow.domain.model.SplitStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "splits", indexes = {
        @Index(name = "idx_split_debtor_creditor", columnList = "debtorId, creditorId, status")
})
@Getter
@Setter
@NoArgsConstructor
public class SplitJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private UUID creditorId;

    @Column(nullable = false)
    private UUID debtorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitStatus status;

    @Column(nullable = false)
    private int disputeCount;

    @Column(nullable = false)
    private boolean disputeLocked;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
