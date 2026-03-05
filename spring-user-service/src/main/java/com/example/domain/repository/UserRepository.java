package com.example.domain.repository;

import com.example.domain.model.User;
import com.example.domain.model.UserRole;
import com.example.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain Repository Interface for User aggregate.
 * Defined in the Domain layer; implemented in Infrastructure layer.
 */
public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByStatus(UserStatus status);

    List<User> findAllByRole(UserRole role);

    List<User> findAll();

    User save(User user);

    void deleteById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
