package com.splitflow.infrastructure.adapter.persistence;

import com.splitflow.domain.model.User;
import com.splitflow.domain.port.out.UserRepositoryPort;
import com.splitflow.infrastructure.adapter.persistence.entity.UserJpaEntity;
import com.splitflow.infrastructure.adapter.persistence.mapper.UserMapper;
import com.splitflow.infrastructure.adapter.persistence.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserMapper.toEntity(user);
        UserJpaEntity saved = repository.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return repository.findByPhoneNumber(phoneNumber).map(UserMapper::toDomain);
    }
}
