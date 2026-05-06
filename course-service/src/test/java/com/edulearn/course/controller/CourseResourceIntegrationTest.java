package com.edulearn.course.controller;

import com.edulearn.course.dto.CourseDto;
import com.edulearn.course.entity.Course;
import com.edulearn.course.repository.CourseRepository;
import com.edulearn.course.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CourseResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CourseRepository courseRepository;
    @Autowired JwtUtil jwtUtil;

    private static String instructorToken;
    private static String adminToken;
    private static Long createdCourseId;

    @BeforeEach
    void setUp() {
        // Generate tokens for testing using the test JWT secret
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L);

        instructorToken = jwtUtil.generateToken(10L, "instructor@test.com", "INSTRUCTOR");
        adminToken = jwtUtil.generateToken(1L, "admin@test.com", "ADMIN");
    }

    // ── Public endpoints — no auth needed ─────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /courses — public access, returns paged response")
    void getAllCourses_public() throws Exception {
        courseRepository.deleteAll();

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @Order(2)
    @DisplayName("GET /courses/featured — public access")
    void getFeaturedCourses_public() throws Exception {
        mockMvc.perform(get("/api/v1/courses/featured"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("GET /courses/free — public access")
    void getFreeCourses_public() throws Exception {
        mockMvc.perform(get("/api/v1/courses/free"))
                .andExpect(status().isOk());
    }

    // ── Create course ─────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST /courses — instructor creates course")
    void createCourse_instructor() throws Exception {
        courseRepository.deleteAll();

        CourseDto.CourseRequest req = CourseDto.CourseRequest.builder()
                .title("Spring Boot for Beginners")
                .description("A complete Spring Boot guide")
                .category(Course.Category.PROGRAMMING)
                .level(Course.Level.BEGINNER)
                .price(BigDecimal.valueOf(999))
                .language(Course.Language.ENGLISH)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + instructorToken)
                        .header("X-User-Name", "John Doe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring Boot for Beginners"))
                .andExpect(jsonPath("$.isPublished").value(false))
                .andExpect(jsonPath("$.instructorId").value(10))
                .andReturn();

        CourseDto.CourseResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), CourseDto.CourseResponse.class);
        createdCourseId = resp.getCourseId();
        assertThat(createdCourseId).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("POST /courses — no auth returns 403")
    void createCourse_noAuth() throws Exception {
        CourseDto.CourseRequest req = CourseDto.CourseRequest.builder()
                .title("Unauthorized").category(Course.Category.DESIGN)
                .level(Course.Level.ALL_LEVELS).price(BigDecimal.ZERO).build();

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── Get course by ID ──────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /courses/{id} — returns course details")
    void getCourseById_success() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + createdCourseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(createdCourseId))
                .andExpect(jsonPath("$.title").value("Spring Boot for Beginners"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /courses/{id} — not found returns 404")
    void getCourseById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/courses/99999"))
                .andExpect(status().isNotFound());
    }

    // ── Update course ─────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("PUT /courses/{id} — owner updates successfully")
    void updateCourse_ownerSuccess() throws Exception {
        CourseDto.CourseRequest req = CourseDto.CourseRequest.builder()
                .title("Spring Boot Advanced")
                .category(Course.Category.PROGRAMMING)
                .level(Course.Level.ADVANCED)
                .price(BigDecimal.valueOf(1499))
                .build();

        mockMvc.perform(put("/api/v1/courses/" + createdCourseId)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Boot Advanced"))
                .andExpect(jsonPath("$.level").value("ADVANCED"));
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("PUT /courses/{id}/publish — owner publishes course")
    void publishCourse_success() throws Exception {
        CourseDto.PublishRequest req = new CourseDto.PublishRequest(true);

        mockMvc.perform(put("/api/v1/courses/" + createdCourseId + "/publish")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublished").value(true));
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("GET /courses/search — keyword search returns results")
    void searchCourses_keyword() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search")
                        .param("keyword", "Spring")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(11)
    @DisplayName("GET /courses/search — filter by category")
    void searchCourses_byCategory() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search")
                        .param("category", "PROGRAMMING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    // ── Featured toggle (admin) ───────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("PUT /courses/{id}/featured — admin sets featured=true")
    void toggleFeatured_adminSuccess() throws Exception {
        mockMvc.perform(put("/api/v1/courses/" + createdCourseId + "/featured")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("featured", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFeatured").value(true));
    }

    // ── Internal endpoints ────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("POST /courses/internal/enrollment-count — updates count")
    void updateEnrollmentCount_internal() throws Exception {
        CourseDto.EnrollmentCountUpdate update =
                new CourseDto.EnrollmentCountUpdate(createdCourseId, 1);

        mockMvc.perform(post("/api/v1/courses/internal/enrollment-count")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        Course updated = courseRepository.findById(createdCourseId).orElseThrow();
        assertThat(updated.getTotalEnrollments()).isEqualTo(1);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("DELETE /courses/{id} — owner deletes course")
    void deleteCourse_ownerSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/" + createdCourseId)
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(courseRepository.findById(createdCourseId)).isEmpty();
    }
}
