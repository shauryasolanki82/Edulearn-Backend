package com.edulearn.enrollment.controller;

import com.edulearn.enrollment.dto.EnrollmentDto;
import com.edulearn.enrollment.repository.EnrollmentRepository;
import com.edulearn.enrollment.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnrollmentResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired JwtUtil jwtUtil;
    @MockBean RestTemplate restTemplate;

    private static String studentToken;
    private static String adminToken;
    private static String instructorToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L);
        studentToken    = jwtUtil.generateToken(10L, "student@test.com",    "STUDENT");
        adminToken      = jwtUtil.generateToken(1L,  "admin@test.com",      "ADMIN");
        instructorToken = jwtUtil.generateToken(5L,  "instructor@test.com", "INSTRUCTOR");
    }

    @Test @Order(1) @DisplayName("POST /enrollments — student enrolls in a course")
    void enroll_success() throws Exception {
        enrollmentRepository.deleteAll();
        EnrollmentDto.EnrollRequest req = EnrollmentDto.EnrollRequest.builder().courseId(20L).build();
        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(10))
                .andExpect(jsonPath("$.courseId").value(20))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @Order(2) @DisplayName("POST /enrollments — duplicate returns 409")
    void enroll_duplicate() throws Exception {
        EnrollmentDto.EnrollRequest req = EnrollmentDto.EnrollRequest.builder().courseId(20L).build();
        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @Order(3) @DisplayName("GET /enrollments/my — returns student's enrollments")
    void getMyEnrollments() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(10));
    }

    @Test @Order(4) @DisplayName("GET /enrollments/check — returns enrolled=true")
    void isEnrolled_true() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/check")
                        .param("studentId", "10").param("courseId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true));
    }

    @Test @Order(5) @DisplayName("GET /enrollments/check — returns enrolled=false for unknown")
    void isEnrolled_false() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/check")
                        .param("studentId", "10").param("courseId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(false));
    }

    @Test @Order(6) @DisplayName("PUT /enrollments/{courseId}/progress — updates progress")
    void updateProgress() throws Exception {
        EnrollmentDto.ProgressUpdateRequest req = new EnrollmentDto.ProgressUpdateRequest(75.0);
        mockMvc.perform(put("/api/v1/enrollments/20/progress")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(75.0));
    }

    @Test @Order(7) @DisplayName("PUT /enrollments/{courseId}/complete — marks complete")
    void markComplete() throws Exception {
        mockMvc.perform(put("/api/v1/enrollments/20/complete")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercent").value(100.0));
    }

    @Test @Order(8) @DisplayName("POST /enrollments/{courseId}/certificate — issues certificate")
    void issueCertificate() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments/20/certificate")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateIssued").value(true));
    }

    @Test @Order(9) @DisplayName("GET /enrollments/course/{id}/count — public count endpoint")
    void getCount() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/course/20/count"))
                .andExpect(status().isOk());
    }

    @Test @Order(10) @DisplayName("GET /enrollments/course/{id}/stats — instructor stats")
    void getStats() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/course/20/stats")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(20));
    }

    @Test @Order(11) @DisplayName("DELETE /enrollments/{courseId} — student unenrolls")
    void unenroll() throws Exception {
        mockMvc.perform(delete("/api/v1/enrollments/20")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
