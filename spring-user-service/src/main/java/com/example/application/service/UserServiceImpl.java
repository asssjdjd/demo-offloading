package com.example.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.application.dto.CreateUserRequest;
import com.example.application.dto.UpdateUserRequest;
import com.example.application.dto.UserResponse;
import com.example.application.gateway.GatewayIdentity;
import com.example.application.mapper.UserMapper;
import com.example.domain.exception.ForbiddenAccessException;
import com.example.domain.exception.UserAlreadyExistsException;
import com.example.domain.exception.UserNotFoundException;
import com.example.domain.model.User;
import com.example.domain.repository.UserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of UserService.
 * 
 * Key Design Decisions (Gateway Offloading):
 * - NO JWT parsing or token validation logic here
 * - User identity comes from GatewayIdentity (extracted from Kong headers)
 * - Service focuses purely on business logic
 * - Authorization checks use role from GatewayIdentity, not from token
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser(GatewayIdentity identity) {
        log.info("Getting current user profile for userId={}", identity.getUserId());

        UUID userId = parseUserId(identity.getUserId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(identity.getUserId()));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        log.info("Getting user by id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        log.info("Getting all users");
        return userMapper.toResponseList(userRepository.findAll());
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with username={}", request.getUsername());

        // Check for duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("User created successfully with id={}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, GatewayIdentity identity) {
        log.info("Updating user id={} by userId={}", id, identity.getUserId());

        // Authorization: users can only update their own profile, admins can update anyone
        UUID requesterId = parseUserId(identity.getUserId());
        if (!identity.isAdmin() && !id.equals(requesterId)) {
            throw new ForbiddenAccessException("You can only update your own profile");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        // Check email uniqueness if changing
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("email", request.getEmail());
            }
        }

        // Domain behavior: update profile through aggregate root
        user.updateProfile(request.getFullName(), request.getEmail(), request.getPhone());
        User updatedUser = userRepository.save(user);

        log.info("User updated successfully id={}", id);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        log.info("Deactivating user id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        user.deactivate();
        userRepository.save(user);

        log.info("User deactivated successfully id={}", id);
    }

    @Override
    @Transactional
    public void activateUser(UUID id) {
        log.info("Activating user id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));

        user.activate();
        userRepository.save(user);

        log.info("User activated successfully id={}", id);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new UserNotFoundException("Invalid user ID format: " + userId);
        }
    }
}
