package com.splitflow.domain.model;

public enum SplitStatus {
    PENDING,
    PARTIALLY_PAID,
    CLAIMED_PAID,
    DISPUTED,
    SETTLED;

    public boolean canTransitionTo(SplitStatus target) {
        return switch (this) {
            case PENDING, PARTIALLY_PAID -> target == CLAIMED_PAID || target == DISPUTED || target == PARTIALLY_PAID;
            case CLAIMED_PAID -> target == SETTLED || target == DISPUTED;
            case DISPUTED -> target == PENDING || target == CLAIMED_PAID || target == SETTLED;
            case SETTLED -> false; // Terminal state
        };
    }
}
