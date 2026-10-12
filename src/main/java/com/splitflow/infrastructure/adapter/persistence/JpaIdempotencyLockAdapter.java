package com.splitflow.infrastructure.adapter.persistence;

import com.splitflow.domain.port.out.IdempotencyLockPort;
import com.splitflow.infrastructure.adapter.persistence.entity.IdempotencyLockJpaEntity;
import com.splitflow.infrastructure.adapter.persistence.repository.SpringDataLockRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class JpaIdempotencyLockAdapter implements IdempotencyLockPort {

    private final SpringDataLockRepository repository;

    public JpaIdempotencyLockAdapter(SpringDataLockRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(String lockKey, long ttlSeconds) {
        Instant now = Instant.now();
        Optional<IdempotencyLockJpaEntity> existing = repository.findById(lockKey);

        if (existing.isPresent()) {
            if (existing.get().getExpiresAt().isAfter(now)) {
                // Lock is active! Reject concurrent execution (Anti-duplicate shield)
                return false;
            } else {
                // Expired lock, refresh TTL
                existing.get().setExpiresAt(now.plusSeconds(ttlSeconds));
                repository.save(existing.get());
                return true;
            }
        }

        try {
            repository.save(new IdempotencyLockJpaEntity(lockKey, now.plusSeconds(ttlSeconds)));
            return true;
        } catch (Exception e) {
            // Concurrently inserted by another thread
            return false;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String lockKey) {
        try {
            repository.deleteById(lockKey);
        } catch (Exception ignored) {
        }
    }
}
