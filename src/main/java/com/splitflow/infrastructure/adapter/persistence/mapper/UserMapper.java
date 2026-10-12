package com.splitflow.infrastructure.adapter.persistence.mapper;

import com.splitflow.domain.model.User;
import com.splitflow.infrastructure.adapter.persistence.entity.UserJpaEntity;

import java.util.HashMap;

public class UserMapper {

    public static User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;
        return new User(
                entity.getId(),
                entity.getPhoneNumber(),
                entity.getDisplayName(),
                entity.getContactAliases(),
                entity.getCreatedAt()
        );
    }

    public static UserJpaEntity toEntity(User domain) {
        if (domain == null) return null;
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setDisplayName(domain.getDisplayName());
        entity.setContactAliases(new HashMap<>(domain.getContactAliases()));
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
