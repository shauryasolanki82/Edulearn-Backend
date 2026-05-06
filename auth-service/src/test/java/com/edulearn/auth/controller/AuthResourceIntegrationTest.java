package com.edulearn.auth.controller;

import com.edulearn.auth.dto.AuthDto;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;

    private static String jwtToken;
    private static Long registeredUserId;

    @BeforeEach
    void cleanUp() {
        // Only clean before the first test
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /register — creates user and returns JWT")
    void register_success() throws Exception {
        userRepository.deleteAll();

        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .fullName("Integration User")
                .email("integ@test.com")
                .password("password123")
                .role(User.Role.STUDENT)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("integ@test.com"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andReturn();

        AuthDto.AuthResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthDto.AuthResponse.class);
        jwtToken = resp.getAccessToken();
        registeredUserId = resp.getUser().getUserId();
        assertThat(jwtToken).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("POST /register — duplicate email returns 409")
    void register_duplicateEmail() throws Exception {
        AuthDto.RegisterRequest req = AuthDto.RegisterRequest.builder()
                .fullName("Duplicate")
                .email("integ@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("POST /register — missing fields returns 400")
    void register_validation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST /login — valid credentials return JWT")
    void login_success() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest("integ@test.com", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        AuthDto.AuthResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthDto.AuthResponse.class);
        jwtToken = resp.getAccessToken(); // refresh token for later tests
    }

    @Test
    @Order(5)
    @DisplayName("POST /login — wrong password returns 401")
    void login_wrongPassword() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest("integ@test.com", "wrongpass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ── Validate Token ────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /validate — valid token returns valid=true")
    void validate_validToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("integ@test.com"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /validate — garbage token returns valid=false")
    void validate_invalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer garbage.token.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("GET /profile — authenticated user gets their profile")
    void getProfile_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integ@test.com"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /profile — no token returns 403")
    void getProfile_noToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/profile"))
                .andExpect(status().isForbidden());
    }

    // ── Update Profile ────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("PUT /profile — updates bio and mobile")
    void updateProfile_success() throws Exception {
        AuthDto.UpdateProfileRequest req = new AuthDto.UpdateProfileRequest(
                "Integration User Updated", "9876543210", "LMS tester", null);

        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Integration User Updated"))
                .andExpect(jsonPath("$.mobile").value("9876543210"));
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("PUT /password — correct old password updates successfully")
    void changePassword_success() throws Exception {
        AuthDto.ChangePasswordRequest req =
                new AuthDto.ChangePasswordRequest("password123", "newpassword456");

        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("POST /refresh — valid token returns fresh JWT")
    void refreshToken_success() throws Exception {
        AuthDto.RefreshTokenRequest req = new AuthDto.RefreshTokenRequest(jwtToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    // ── Delete Account ────────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("DELETE /delete — deactivates account")
    void deleteAccount_success() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/delete")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify user is deactivated in DB
        User u = userRepository.findByEmail("integ@test.com").orElseThrow();
        assertThat(u.getIsActive()).isFalse();
    }
}
