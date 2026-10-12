package com.splitflow.domain.port.out;

public interface IdempotencyLockPort {
    boolean tryAcquire(String lockKey, long ttlSeconds);
    void release(String lockKey);
}
