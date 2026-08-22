package com.campusflow.userservice.service.impl;

import com.campusflow.userservice.dto.request.LoginRequest;
import com.campusflow.userservice.dto.request.RegisterRequest;
import com.campusflow.userservice.dto.response.AuthResponse;
import com.campusflow.userservice.dto.response.UserResponse;
import com.campusflow.userservice.entity.Role;
import com.campusflow.userservice.entity.User;
import com.campusflow.userservice.entity.UserStatus;
import com.campusflow.userservice.exception.AuthenticationException;
import com.campusflow.userservice.exception.DuplicateResourceException;
import com.campusflow.userservice.mapper.UserMapper;
import com.campusflow.userservice.repository.UserRepository;
import com.campusflow.userservice.security.JwtTokenProvider;
import com.campusflow.userservice.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@university.edu");
        testUser.setPassword("$2a$10$encodedPasswordHash");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(Role.STUDENT);
        testUser.setStatus(UserStatus.ACTIVE);

        testUserResponse = new UserResponse();
        testUserResponse.setId(testUser.getId());
        testUserResponse.setEmail(testUser.getEmail());
        testUserResponse.setFirstName(testUser.getFirstName());
        testUserResponse.setLastName(testUser.getLastName());
        testUserResponse.setRole(testUser.getRole());
        testUserResponse.setStatus(testUser.getStatus());
    }

    @Test
    @DisplayName("Register user successfully")
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@university.edu");
        request.setPassword("password123");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        UserResponse result = userService.register(request);

        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(auditService).logEvent(eq("USER_REGISTERED"), anyString(), anyString());
    }

    @Test
    @DisplayName("Register user fails with duplicate email")
    void register_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@university.edu");
        request.setPassword("password123");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login successfully")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@university.edu");
        request.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), anyString(), anyString())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        AuthResponse result = userService.login(request);

        assertNotNull(result);
        assertEquals("jwt-token", result.getToken());
        assertEquals("Bearer", result.getTokenType());
        verify(auditService).logEvent(eq("USER_LOGIN"), anyString(), anyString());
    }

    @Test
    @DisplayName("Login fails with wrong password")
    void login_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@university.edu");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Login fails with suspended account")
    void login_SuspendedAccount() {
        testUser.setStatus(UserStatus.SUSPENDED);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@university.edu");
        request.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(AuthenticationException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("Get current user profile")
    void getCurrentUser_Success() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse result = userService.getCurrentUser(testUser.getId().toString());

        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
    }
}
