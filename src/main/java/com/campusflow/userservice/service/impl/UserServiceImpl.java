package com.campusflow.userservice.service.impl;

import com.campusflow.userservice.dto.request.LoginRequest;
import com.campusflow.userservice.dto.request.RegisterRequest;
import com.campusflow.userservice.dto.request.UpdateStatusRequest;
import com.campusflow.userservice.dto.request.UpdateUserRequest;
import com.campusflow.userservice.dto.response.AuthResponse;
import com.campusflow.userservice.dto.response.UserResponse;
import com.campusflow.userservice.entity.Role;
import com.campusflow.userservice.entity.User;
import com.campusflow.userservice.entity.UserStatus;
import com.campusflow.userservice.exception.AuthenticationException;
import com.campusflow.userservice.exception.DuplicateResourceException;
import com.campusflow.userservice.exception.ResourceNotFoundException;
import com.campusflow.userservice.mapper.UserMapper;
import com.campusflow.userservice.repository.UserRepository;
import com.campusflow.userservice.security.JwtTokenProvider;
import com.campusflow.userservice.service.AuditService;
import com.campusflow.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           UserMapper userMapper,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(request.getPhone());

        // Parse role, default to STUDENT
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }

        User savedUser = userRepository.save(user);
        log.info("User registered: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        auditService.logEvent("USER_REGISTERED", savedUser.getId().toString(),
                "User registered: " + savedUser.getEmail());

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("Account is " + user.getStatus().name().toLowerCase());
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        UserResponse userResponse = userMapper.toResponse(user);

        log.info("User logged in: {}", user.getEmail());

        auditService.logEvent("USER_LOGIN", user.getId().toString(),
                "User logged in: " + user.getEmail());

        return new AuthResponse(token, jwtTokenProvider.getExpirationMs() / 1000, userResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userId) {
        UUID id = UUID.fromString(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String role, String status, Pageable pageable) {
        if (role != null && status != null) {
            return userRepository.findByRoleAndStatus(
                    Role.valueOf(role.toUpperCase()),
                    UserStatus.valueOf(status.toUpperCase()),
                    pageable
            ).map(userMapper::toResponse);
        } else if (role != null) {
            return userRepository.findByRole(Role.valueOf(role.toUpperCase()), pageable)
                    .map(userMapper::toResponse);
        } else if (status != null) {
            return userRepository.findByStatus(UserStatus.valueOf(status.toUpperCase()), pageable)
                    .map(userMapper::toResponse);
        }
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        User updated = userRepository.save(user);
        log.info("User updated: {}", updated.getEmail());
        return userMapper.toResponse(updated);
    }

    @Override
    public UserResponse updateUserStatus(UUID id, UpdateStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        try {
            UserStatus newStatus = UserStatus.valueOf(request.getStatus().toUpperCase());
            user.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }

        User updated = userRepository.save(user);
        log.info("User status updated: {} -> {}", updated.getEmail(), updated.getStatus());
        return userMapper.toResponse(updated);
    }
}
