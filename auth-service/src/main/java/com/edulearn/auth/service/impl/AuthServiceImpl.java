package com.edulearn.auth.service.impl;

import com.edulearn.auth.dto.AuthDto;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.exception.BadCredentialsException;
import com.edulearn.auth.exception.DuplicateEmailException;
import com.edulearn.auth.exception.UserNotFoundException;
import com.edulearn.auth.repository.UserRepository;
import com.edulearn.auth.security.JwtUtil;
import com.edulearn.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "Email already registered: " + request.getEmail());
        }

        User.Role role = request.getRole() != null ? request.getRole() : User.Role.STUDENT;

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider("local")
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {} with role: {}", user.getEmail(), user.getRole());

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(),
                user.getRole().name());

        return AuthDto.AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtUtil.getExpirationMs())
                .user(toUserResponse(user))
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is suspended. Contact support.");
        }

        if (!"local".equals(user.getProvider())) {
            throw new BadCredentialsException(
                    "This account uses " + user.getProvider() + " login. Please use OAuth.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(),
                user.getRole().name());

        return AuthDto.AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtUtil.getExpirationMs())
                .user(toUserResponse(user))
                .build();
    }

    // ── Validate Token ────────────────────────────────────────────────────────

    @Override
    public AuthDto.ValidateTokenResponse validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            return new AuthDto.ValidateTokenResponse(false, null, null, null);
        }
        return AuthDto.ValidateTokenResponse.builder()
                .valid(true)
                .userId(jwtUtil.extractUserId(token))
                .email(jwtUtil.extractEmail(token))
                .role(jwtUtil.extractRole(token))
                .build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Override
    public AuthDto.AuthResponse refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BadCredentialsException("Invalid or expired token");
        }

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        String newToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(),
                user.getRole().name());

        return AuthDto.AuthResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresInMs(jwtUtil.getExpirationMs())
                .user(toUserResponse(user))
                .build();
    }

    // ── Get User By Email ─────────────────────────────────────────────────────

    @Override
    public AuthDto.UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
        return toUserResponse(user);
    }

    // ── Get User By Id ────────────────────────────────────────────────────────

    @Override
    public AuthDto.UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return toUserResponse(user);
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(Long userId, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    // ── Update Profile ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthDto.UserResponse updateProfile(Long userId, AuthDto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getMobile() != null) {
            user.setMobile(request.getMobile());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl());
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return toUserResponse(user);
    }

    // ── Delete User ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getEmail());
    }

    // ── Get Users By Role ─────────────────────────────────────────────────────

    @Override
    public List<AuthDto.UserResponse> getUsersByRole(User.Role role) {
        return userRepository.findAllByRole(role)
                .stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    // ── Search Users By Name ──────────────────────────────────────────────────

    @Override
    public List<AuthDto.UserResponse> searchUsersByName(String name) {
        return userRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    // ── OAuth2 Login / Register ───────────────────────────────────────────────

    @Override
    @Transactional
    public AuthDto.AuthResponse handleOAuthLogin(String provider, String providerId,
                                                  String email, String fullName,
                                                  String profilePicUrl) {
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .or(() -> email != null ? userRepository.findByEmail(email) : java.util.Optional.empty())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .fullName(fullName != null ? fullName : "OAuth User")
                            .email(email != null ? email : provider + "_" + providerId + "@oauth.local")
                            .role(User.Role.STUDENT)
                            .provider(provider)
                            .providerId(providerId)
                            .profilePicUrl(profilePicUrl)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });

        // Update provider info in case user switched providers
        user.setProvider(provider);
        user.setProviderId(providerId);
        if (profilePicUrl != null) user.setProfilePicUrl(profilePicUrl);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(),
                user.getRole().name());

        return AuthDto.AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtUtil.getExpirationMs())
                .user(toUserResponse(user))
                .build();
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private AuthDto.UserResponse toUserResponse(User user) {
        return AuthDto.UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .provider(user.getProvider())
                .mobile(user.getMobile())
                .bio(user.getBio())
                .profilePicUrl(user.getProfilePicUrl())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
