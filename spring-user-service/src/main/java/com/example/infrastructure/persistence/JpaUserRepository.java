package com.example.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.domain.model.User;
import com.example.domain.model.UserRole;
import com.example.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for User entity.
 * This is the infrastructure implementation of the domain's UserRepository interface.
 */
@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByStatus(UserStatus status);

    List<User> findAllByRole(UserRole role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
