package com.campusflow.userservice.controller;

import com.campusflow.userservice.dto.request.UpdateStatusRequest;
import com.campusflow.userservice.dto.request.UpdateUserRequest;
import com.campusflow.userservice.dto.response.ApiResponse;
import com.campusflow.userservice.dto.response.UserResponse;
import com.campusflow.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized — missing user identity"));
        }
        UserResponse user = userService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", user));
    }

    @GetMapping
    @Operation(summary = "List all users (admin only)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String filterRole,
            @RequestParam(required = false) String filterStatus,
            @PageableDefault(size = 20) Pageable pageable) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden — admin access required"));
        }
        Page<UserResponse> users = userService.getAllUsers(filterRole, filterStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID id) {
        if (!"ADMIN".equals(role) && !"STAFF".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden — staff or admin access required"));
        }
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        // Allow owner or admin
        boolean isOwner = currentUserId != null && currentUserId.equals(id.toString());
        boolean isAdmin = "ADMIN".equals(currentUserRole);
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden — can only update own profile or be admin"));
        }
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status (admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden — admin access required"));
        }
        UserResponse user = userService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("User status updated", user));
    }
}
