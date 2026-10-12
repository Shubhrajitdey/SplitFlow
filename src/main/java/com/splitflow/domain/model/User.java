package com.splitflow.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final UUID id;
    private final String phoneNumber;
    private String displayName;
    private final Map<String, String> contactAliases; // aliasName -> phoneNumber
    private final Instant createdAt;

    public User(UUID id, String phoneNumber, String displayName, Map<String, String> contactAliases, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
        this.displayName = displayName != null ? displayName : phoneNumber;
        this.contactAliases = contactAliases != null ? new HashMap<>(contactAliases) : new HashMap<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static User createNew(String phoneNumber, String displayName) {
        return new User(UUID.randomUUID(), phoneNumber, displayName, new HashMap<>(), Instant.now());
    }

    public void addContactAlias(String aliasName, String targetPhoneNumber) {
        if (aliasName != null && targetPhoneNumber != null) {
            this.contactAliases.put(aliasName.trim().toLowerCase(), targetPhoneNumber.trim());
        }
    }

    public String resolveContactAlias(String aliasName) {
        if (aliasName == null) return null;
        return this.contactAliases.get(aliasName.trim().toLowerCase());
    }

    public UUID getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Map<String, String> getContactAliases() { return Collections.unmodifiableMap(contactAliases); }
    public Instant getCreatedAt() { return createdAt; }
}
