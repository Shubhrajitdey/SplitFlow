package com.splitflow.domain.port.out;

import com.splitflow.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByPhoneNumber(String phoneNumber);
}
