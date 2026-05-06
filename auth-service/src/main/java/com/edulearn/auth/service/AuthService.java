package com.edulearn.auth.service;

import com.edulearn.auth.dto.AuthDto;
import com.edulearn.auth.entity.User;

import java.util.List;

/**
 * Contract for all authentication and user management operations.
 */
public interface AuthService {

    /**
     * Register a new user (Student or Instructor).
     */
    AuthDto.AuthResponse register(AuthDto.RegisterRequest request);

    /**
     * Authenticate a user and return a JWT token.
     */
    AuthDto.AuthResponse login(AuthDto.LoginRequest request);

    /**
     * Validate a JWT token and return the embedded claims.
     */
    AuthDto.ValidateTokenResponse validateToken(String token);

    /**
     * Issue a fresh JWT for a still-valid existing token.
     */
    AuthDto.AuthResponse refreshToken(String token);

    /**
     * Look up a user by email address.
     */
    AuthDto.UserResponse getUserByEmail(String email);

    /**
     * Look up a user by their primary key.
     */
    AuthDto.UserResponse getUserById(Long userId);

    /**
     * Change a user's password after verifying the current one.
     */
    void changePassword(Long userId, AuthDto.ChangePasswordRequest request);

    /**
     * Update mutable profile fields (name, mobile, bio, picture).
     */
    AuthDto.UserResponse updateProfile(Long userId, AuthDto.UpdateProfileRequest request);

    /**
     * Soft-delete (deactivate) a user account.
     */
    void deleteUser(Long userId);

    /**
     * Return all users holding the given role.
     */
    List<AuthDto.UserResponse> getUsersByRole(User.Role role);

    /**
     * Search users by a partial full-name match.
     */
    List<AuthDto.UserResponse> searchUsersByName(String name);

    /**
     * Find or create an OAuth2 user on successful provider login.
     */
    AuthDto.AuthResponse handleOAuthLogin(String provider, String providerId,
                                          String email, String fullName,
                                          String profilePicUrl);
}
