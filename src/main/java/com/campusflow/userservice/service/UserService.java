package com.campusflow.userservice.service;

import com.campusflow.userservice.dto.request.LoginRequest;
import com.campusflow.userservice.dto.request.RegisterRequest;
import com.campusflow.userservice.dto.request.UpdateStatusRequest;
import com.campusflow.userservice.dto.request.UpdateUserRequest;
import com.campusflow.userservice.dto.response.AuthResponse;
import com.campusflow.userservice.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser(String userId);

    UserResponse getUserById(UUID id);

    Page<UserResponse> getAllUsers(String role, String status, Pageable pageable);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    UserResponse updateUserStatus(UUID id, UpdateStatusRequest request);
}
