package com.edulearn.auth.service;

import com.edulearn.auth.dto.AuthDto;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.exception.BadCredentialsException;
import com.edulearn.auth.exception.DuplicateEmailException;
import com.edulearn.auth.exception.UserNotFoundException;
import com.edulearn.auth.repository.UserRepository;
import com.edulearn.auth.security.JwtUtil;
import com.edulearn.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1L)
                .fullName("Alice Smith")
                .email("alice@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(User.Role.STUDENT)
                .provider("local")
                .isActive(true)
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — happy path creates user and returns token")
    void register_success() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("$hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .fullName("Alice Smith")
                .email("alice@example.com")
                .password("secret")
                .build();

        AuthDto.AuthResponse resp = authService.register(req);

        assertThat(resp.getAccessToken()).isEqualTo("jwt.token");
        assertThat(resp.getUser().getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register — duplicate email throws DuplicateEmailException")
    void register_duplicateEmail() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .fullName("Alice Smith")
                .email("alice@example.com")
                .password("secret")
                .build();

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("alice@example.com");
    }

    @Test
    @DisplayName("register — defaults role to STUDENT when null")
    void register_defaultRoleStudent() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("tok");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> {
            User u = captor.getValue();
            u.setUserId(99L);
            return u;
        });

        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .fullName("Bob")
                .email("bob@example.com")
                .password("pass123")
                .role(null)
                .build();

        authService.register(req);

        assertThat(captor.getValue().getRole()).isEqualTo(User.Role.STUDENT);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login — correct credentials return token")
    void login_success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        AuthDto.LoginRequest req = new AuthDto.LoginRequest("alice@example.com", "secret");
        AuthDto.AuthResponse resp = authService.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("jwt.token");
    }

    @Test
    @DisplayName("login — wrong password throws BadCredentialsException")
    void login_wrongPassword() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new AuthDto.LoginRequest("alice@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login — unknown email throws BadCredentialsException")
    void login_unknownEmail() {
        when(userRepository.findByEmail("unknown@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthDto.LoginRequest("unknown@x.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login — inactive account throws BadCredentialsException")
    void login_inactiveUser() {
        sampleUser.setIsActive(false);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.login(new AuthDto.LoginRequest("alice@example.com", "secret")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("suspended");
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken — valid token returns populated response")
    void validateToken_valid() {
        when(jwtUtil.validateToken("good.token")).thenReturn(true);
        when(jwtUtil.extractUserId("good.token")).thenReturn(1L);
        when(jwtUtil.extractEmail("good.token")).thenReturn("alice@example.com");
        when(jwtUtil.extractRole("good.token")).thenReturn("STUDENT");

        AuthDto.ValidateTokenResponse resp = authService.validateToken("good.token");

        assertThat(resp.isValid()).isTrue();
        assertThat(resp.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("validateToken — invalid token returns valid=false")
    void validateToken_invalid() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);

        AuthDto.ValidateTokenResponse resp = authService.validateToken("bad");
        assertThat(resp.isValid()).isFalse();
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById — returns correct user")
    void getUserById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        AuthDto.UserResponse resp = authService.getUserById(1L);
        assertThat(resp.getUserId()).isEqualTo(1L);
        assertThat(resp.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("getUserById — unknown id throws UserNotFoundException")
    void getUserById_notFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile — updates mutable fields")
    void updateProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(sampleUser);

        AuthDto.UpdateProfileRequest req = new AuthDto.UpdateProfileRequest(
                "Alice Updated", "9999999999", "LMS learner", null);

        AuthDto.UserResponse resp = authService.updateProfile(1L, req);

        assertThat(sampleUser.getFullName()).isEqualTo("Alice Updated");
        assertThat(sampleUser.getMobile()).isEqualTo("9999999999");
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword — correct old password updates hash")
    void changePassword_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass", sampleUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("$newHash");
        when(userRepository.save(any())).thenReturn(sampleUser);

        authService.changePassword(1L, new AuthDto.ChangePasswordRequest("oldPass", "newPass"));

        assertThat(sampleUser.getPasswordHash()).isEqualTo("$newHash");
    }

    @Test
    @DisplayName("changePassword — wrong old password throws BadCredentialsException")
    void changePassword_wrongOld() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() ->
                authService.changePassword(1L, new AuthDto.ChangePasswordRequest("wrong", "new")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser — soft-deletes by setting isActive=false")
    void deleteUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(sampleUser);

        authService.deleteUser(1L);

        assertThat(sampleUser.getIsActive()).isFalse();
        verify(userRepository).save(sampleUser);
    }

    // ── getUsersByRole ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUsersByRole — returns list for given role")
    void getUsersByRole_success() {
        when(userRepository.findAllByRole(User.Role.STUDENT)).thenReturn(List.of(sampleUser));

        List<AuthDto.UserResponse> list = authService.getUsersByRole(User.Role.STUDENT);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getRole()).isEqualTo("STUDENT");
    }

    // ── searchUsersByName ─────────────────────────────────────────────────────

    @Test
    @DisplayName("searchUsersByName — returns matching users")
    void searchUsersByName_success() {
        when(userRepository.findByFullNameContainingIgnoreCase("alice"))
                .thenReturn(List.of(sampleUser));

        List<AuthDto.UserResponse> list = authService.searchUsersByName("alice");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getFullName()).isEqualTo("Alice Smith");
    }
}
