package com.edulearn.progress.controller;

import com.edulearn.progress.dto.ProgressDto;
import com.edulearn.progress.repository.CertificateRepository;
import com.edulearn.progress.repository.ProgressRepository;
import com.edulearn.progress.security.JwtUtil;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProgressResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProgressRepository progressRepository;
    @Autowired CertificateRepository certificateRepository;
    @Autowired JwtUtil jwtUtil;
    @MockBean RestTemplate restTemplate;

    private static String studentToken;
    private static String adminToken;
    private static String verificationCode;

    private static final Long STUDENT_ID = 10L;
    private static final Long COURSE_ID  = 20L;
    private static final Long LESSON_ID  = 30L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        studentToken = jwtUtil.generateToken(STUDENT_ID, "student@test.com", "STUDENT");
        adminToken   = jwtUtil.generateToken(1L, "admin@test.com", "ADMIN");
    }

    // ── Track progress ────────────────────────────────────────────────────────

    @Test @Order(1) @DisplayName("POST /progress — tracks lesson watch time")
    void trackProgress_success() throws Exception {
        progressRepository.deleteAll();
        certificateRepository.deleteAll();

        ProgressDto.TrackRequest req = ProgressDto.TrackRequest.builder()
                .lessonId(LESSON_ID).courseId(COURSE_ID)
                .watchedSeconds(180).markComplete(false).build();

        mockMvc.perform(post("/api/v1/progress")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID))
                .andExpect(jsonPath("$.lessonId").value(LESSON_ID))
                .andExpect(jsonPath("$.watchedSeconds").value(180))
                .andExpect(jsonPath("$.isCompleted").value(false));
    }

    @Test @Order(2) @DisplayName("POST /progress — markComplete=true marks lesson done")
    void trackProgress_markComplete() throws Exception {
        ProgressDto.TrackRequest req = ProgressDto.TrackRequest.builder()
                .lessonId(LESSON_ID).courseId(COURSE_ID)
                .watchedSeconds(450).markComplete(true).build();

        mockMvc.perform(post("/api/v1/progress")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    // ── Get lesson progress ───────────────────────────────────────────────────

    @Test @Order(3) @DisplayName("GET /progress/lesson/{id} — returns lesson progress")
    void getLessonProgress() throws Exception {
        mockMvc.perform(get("/api/v1/progress/lesson/" + LESSON_ID)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(LESSON_ID))
                .andExpect(jsonPath("$.isCompleted").value(true));
    }

    // ── Mark lesson complete ──────────────────────────────────────────────────

    @Test @Order(4) @DisplayName("PUT /progress/lesson/{id}/complete — marks lesson complete")
    void markLessonComplete() throws Exception {
        mockMvc.perform(put("/api/v1/progress/lesson/" + LESSON_ID + "/complete")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("courseId", String.valueOf(COURSE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true));
    }

    // ── Course progress ───────────────────────────────────────────────────────

    @Test @Order(5) @DisplayName("GET /progress/course/{id} — returns course completion")
    void getCourseProgress() throws Exception {
        mockMvc.perform(get("/api/v1/progress/course/" + COURSE_ID)
                        .header("Authorization", "Bearer " + studentToken)
                        .param("totalLessons", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(COURSE_ID))
                .andExpect(jsonPath("$.completionPercent").value(100.0))
                .andExpect(jsonPath("$.completedLessons").value(1));
    }

    // ── All progress for student ──────────────────────────────────────────────

    @Test @Order(6) @DisplayName("GET /progress/my — returns all progress records")
    void getAllMyProgress() throws Exception {
        mockMvc.perform(get("/api/v1/progress/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentId").value(STUDENT_ID));
    }

    // ── Issue certificate ─────────────────────────────────────────────────────

    @Test @Order(7) @DisplayName("POST /certificates — issues certificate")
    void issueCertificate() throws Exception {
        ProgressDto.IssueCertificateRequest req = ProgressDto.IssueCertificateRequest.builder()
                .courseId(COURSE_ID).courseName("Spring Boot Mastery")
                .studentName("Alice Smith").instructorName("John Doe").build();

        MvcResult result = mockMvc.perform(post("/api/v1/certificates")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseName").value("Spring Boot Mastery"))
                .andExpect(jsonPath("$.verificationCode").isNotEmpty())
                .andReturn();

        ProgressDto.CertificateResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProgressDto.CertificateResponse.class);
        verificationCode = resp.getVerificationCode();
        assertThat(verificationCode).isNotBlank();
    }

    // ── Get certificate ───────────────────────────────────────────────────────

    @Test @Order(8) @DisplayName("GET /certificates/course/{id} — returns certificate")
    void getCertificate() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/course/" + COURSE_ID)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(COURSE_ID))
                .andExpect(jsonPath("$.studentName").value("Alice Smith"));
    }

    // ── Get my certificates ───────────────────────────────────────────────────

    @Test @Order(9) @DisplayName("GET /certificates/my — returns list of student certificates")
    void getMyCertificates() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].courseName").value("Spring Boot Mastery"));
    }

    // ── Public verify ─────────────────────────────────────────────────────────

    @Test @Order(10) @DisplayName("GET /certificates/verify/{code} — valid code returns valid=true (public)")
    void verifyCertificate_valid() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/verify/" + verificationCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.courseName").value("Spring Boot Mastery"))
                .andExpect(jsonPath("$.studentName").value("Alice Smith"));
    }

    @Test @Order(11) @DisplayName("GET /certificates/verify/{code} — bad code returns valid=false (public)")
    void verifyCertificate_invalid() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/verify/bad-code-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ── Admin views ───────────────────────────────────────────────────────────

    @Test @Order(12) @DisplayName("GET /certificates/student/{id} — admin views student certs")
    void getCertsByStudentAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/student/" + STUDENT_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(STUDENT_ID));
    }

    @Test @Order(13) @DisplayName("GET /progress/student/{id} — admin views student progress")
    void getProgressByStudentAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/progress/student/" + STUDENT_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
