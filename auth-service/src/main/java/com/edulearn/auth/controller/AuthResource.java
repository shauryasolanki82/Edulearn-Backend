package com.edulearn.auth.controller;

import com.edulearn.auth.dto.AuthDto;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, token management, and profile APIs")
public class AuthResource {

    private final AuthService authService;

    // ── POST /api/v1/auth/register ────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user (Student or Instructor)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Email already in use"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<AuthDto.AuthResponse> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── POST /api/v1/auth/login ───────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email and password, returns JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthDto.AuthResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── GET /api/v1/auth/validate ─────────────────────────────────────────────

    @GetMapping("/validate")
    @Operation(summary = "Validate a JWT token and return its claims")
    public ResponseEntity<AuthDto.ValidateTokenResponse> validate(
            @Parameter(description = "Bearer token to validate")
            @RequestHeader("Authorization") String authHeader) {
        String token = extractBearer(authHeader);
        return ResponseEntity.ok(authService.validateToken(token));
    }

    // ── POST /api/v1/auth/refresh ─────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(summary = "Refresh an existing JWT token")
    public ResponseEntity<AuthDto.AuthResponse> refresh(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getToken()));
    }

    // ── GET /api/v1/auth/profile ──────────────────────────────────────────────

    @GetMapping("/profile")
    @Operation(summary = "Get the authenticated user's profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.UserResponse> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    // ── PUT /api/v1/auth/profile ──────────────────────────────────────────────

    @PutMapping("/profile")
    @Operation(summary = "Update the authenticated user's profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.UserResponse> updateProfile(
            HttpServletRequest request,
            @RequestBody AuthDto.UpdateProfileRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(authService.updateProfile(userId, body));
    }

    // ── PUT /api/v1/auth/password ─────────────────────────────────────────────

    @PutMapping("/password")
    @Operation(summary = "Change the authenticated user's password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.ApiResponse> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody AuthDto.ChangePasswordRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        authService.changePassword(userId, body);
        return ResponseEntity.ok(AuthDto.ApiResponse.builder()
                .success(true)
                .message("Password changed successfully")
                .build());
    }

    // ── DELETE /api/v1/auth/delete ────────────────────────────────────────────

    @DeleteMapping("/delete")
    @Operation(summary = "Deactivate (soft-delete) the authenticated user's account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.ApiResponse> deleteAccount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        authService.deleteUser(userId);
        return ResponseEntity.ok(AuthDto.ApiResponse.builder()
                .success(true)
                .message("Account deactivated successfully")
                .build());
    }

    // ── GET /api/v1/auth/user/{id} ────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get a user by ID (inter-service or admin use)")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<AuthDto.UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    // ── GET /api/v1/auth/user/email/{email} ───────────────────────────────────

    @GetMapping("/user/email/{email}")
    @Operation(summary = "Get a user by email (inter-service use)")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<AuthDto.UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    // ── GET /api/v1/auth/users/role/{role} ────────────────────────────────────

    @GetMapping("/users/role/{role}")
    @Operation(summary = "List all users with a given role (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthDto.UserResponse>> getUsersByRole(
            @PathVariable User.Role role) {
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    // ── GET /api/v1/auth/users/search ─────────────────────────────────────────

    @GetMapping("/users/search")
    @Operation(summary = "Search users by name (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuthDto.UserResponse>> searchUsers(
            @RequestParam String name) {
        return ResponseEntity.ok(authService.searchUsersByName(name));
    }

    // ── DELETE /api/v1/admin/users/{userId} ───────────────────────────────────

    @DeleteMapping("/admin/users/{userId}")
    @Operation(summary = "Admin: deactivate any user account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthDto.ApiResponse> adminDeleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.ok(AuthDto.ApiResponse.builder()
                .success(true)
                .message("User " + userId + " deactivated")
                .build());
    }

    // ── GET /api/v1/auth/oauth/success ────────────────────────────────────────
    // Called by Spring Security after a successful OAuth2 login redirect

    @GetMapping("/oauth/success")
    @Operation(summary = "OAuth2 success callback — returns a JWT after provider login")
    public ResponseEntity<AuthDto.AuthResponse> oauthSuccess(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new IllegalStateException("OAuth2 authentication is required");
        }

        OAuth2User principal = oauthToken.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        String providerId = extractProviderId(attributes);
        String email = (String) attributes.get("email");
        String fullName = firstNonBlank((String) attributes.get("name"), (String) attributes.get("login"));
        String pictureUrl = firstNonBlank((String) attributes.get("picture"), (String) attributes.get("avatar_url"));

        return ResponseEntity.ok(authService.handleOAuthLogin(
                provider,
                providerId,
                email,
                fullName,
                pictureUrl
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractBearer(String header) {
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }

    private String extractProviderId(Map<String, Object> attributes) {
        Object providerId = attributes.get("sub");
        if (providerId == null) {
            providerId = attributes.get("id");
        }
        if (providerId == null) {
            throw new IllegalStateException("OAuth2 provider did not return a user identifier");
        }
        return String.valueOf(providerId);
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return null;
    }
}
