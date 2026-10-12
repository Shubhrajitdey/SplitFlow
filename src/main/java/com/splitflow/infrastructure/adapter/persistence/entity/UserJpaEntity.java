package com.splitflow.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String displayName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_contacts", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "alias_name")
    @Column(name = "target_phone")
    private Map<String, String> contactAliases = new HashMap<>();

    @Column(nullable = false)
    private Instant createdAt;
}
