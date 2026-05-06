package com.edulearn.lesson.controller;

import com.edulearn.lesson.dto.LessonDto;
import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import com.edulearn.lesson.repository.LessonRepository;
import com.edulearn.lesson.repository.ResourceRepository;
import com.edulearn.lesson.security.JwtUtil;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LessonResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired LessonRepository lessonRepository;
    @Autowired ResourceRepository resourceRepository;
    @Autowired JwtUtil jwtUtil;

    // Mock RestTemplate so inter-service calls to course-service don't fail in tests
    @MockBean RestTemplate restTemplate;

    private static String instructorToken;
    private static String adminToken;
    private static String studentToken;
    private static Long createdLessonId;
    private static Long createdResourceId;
    private static final Long COURSE_ID    = 10L;
    private static final Long INSTRUCTOR_ID = 5L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L);

        instructorToken = jwtUtil.generateToken(INSTRUCTOR_ID, "instructor@test.com", "INSTRUCTOR");
        adminToken      = jwtUtil.generateToken(1L,  "admin@test.com",      "ADMIN");
        studentToken    = jwtUtil.generateToken(20L, "student@test.com",    "STUDENT");
    }

    // ── Add lesson ────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /lessons/course/{id} — instructor adds lesson")
    void addLesson_instructorSuccess() throws Exception {
        lessonRepository.deleteAll();

        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Introduction to Spring Boot")
                .contentType(Lesson.ContentType.VIDEO)
                .contentUrl("https://cdn.example.com/intro.mp4")
                .durationMinutes(45)
                .isPreview(true)
                .build();

        MvcResult result = mockMvc.perform(
                post("/api/v1/lessons/course/" + COURSE_ID)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Introduction to Spring Boot"))
                .andExpect(jsonPath("$.courseId").value(COURSE_ID))
                .andExpect(jsonPath("$.isPreview").value(true))
                .andExpect(jsonPath("$.orderIndex").value(1))
                .andReturn();

        LessonDto.LessonResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), LessonDto.LessonResponse.class);
        createdLessonId = resp.getLessonId();
        assertThat(createdLessonId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("POST /lessons/course/{id} — no auth returns 403")
    void addLesson_noAuth() throws Exception {
        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Unauthorized").contentType(Lesson.ContentType.ARTICLE).build();

        mockMvc.perform(post("/api/v1/lessons/course/" + COURSE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── Preview lessons (public) ──────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("GET /lessons/course/{id}/preview — public access returns previews")
    void getPreviewLessons_public() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/course/" + COURSE_ID + "/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].isPreview").value(true));
    }

    // ── Get all lessons for course ────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("GET /lessons/course/{id} — authenticated returns lesson list")
    void getLessonsByCourse_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/course/" + COURSE_ID)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Introduction to Spring Boot"));
    }

    // ── Get single lesson ─────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("GET /lessons/{id} — instructor gets full lesson including contentUrl")
    void getLessonById_instructor() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/" + createdLessonId)
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(createdLessonId))
                .andExpect(jsonPath("$.contentUrl").value("https://cdn.example.com/intro.mp4"));
    }

    // ── Get single preview lesson (public) ───────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /lessons/{id}/preview — public access to preview lesson")
    void getSinglePreviewLesson_public() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/" + createdLessonId + "/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPreview").value(true));
    }

    // ── Update lesson ─────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("PUT /lessons/{id} — instructor updates lesson title and duration")
    void updateLesson_instructorSuccess() throws Exception {
        LessonDto.LessonRequest req = LessonDto.LessonRequest.builder()
                .title("Spring Boot Deep Dive")
                .contentType(Lesson.ContentType.VIDEO)
                .durationMinutes(90)
                .isPreview(false)
                .build();

        mockMvc.perform(put("/api/v1/lessons/" + createdLessonId)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Boot Deep Dive"))
                .andExpect(jsonPath("$.durationMinutes").value(90));
    }

    // ── Add resource ──────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("POST /lessons/{id}/resources — instructor adds resource")
    void addResource_instructorSuccess() throws Exception {
        LessonDto.ResourceRequest req = LessonDto.ResourceRequest.builder()
                .name("Slide Deck")
                .fileUrl("https://cdn.example.com/slides.pdf")
                .fileType(Resource.FileType.PDF)
                .sizeKb(512L)
                .build();

        MvcResult result = mockMvc.perform(
                post("/api/v1/lessons/" + createdLessonId + "/resources")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Slide Deck"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andReturn();

        LessonDto.ResourceResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), LessonDto.ResourceResponse.class);
        createdResourceId = resp.getResourceId();
        assertThat(createdResourceId).isNotNull();
    }

    // ── Get resources ─────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("GET /lessons/{id}/resources — returns resource list")
    void getResources_success() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/" + createdLessonId + "/resources")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Slide Deck"));
    }

    // ── Add second lesson then reorder ────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("PUT /lessons/course/{id}/reorder — admin reorders lessons")
    void reorderLessons_adminSuccess() throws Exception {
        // Add second lesson first
        LessonDto.LessonRequest req2 = LessonDto.LessonRequest.builder()
                .title("Lesson 2")
                .contentType(Lesson.ContentType.ARTICLE)
                .durationMinutes(20)
                .isPreview(false)
                .build();

        MvcResult r2 = mockMvc.perform(
                post("/api/v1/lessons/course/" + COURSE_ID)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        Long lesson2Id = objectMapper.readValue(
                r2.getResponse().getContentAsString(),
                LessonDto.LessonResponse.class).getLessonId();

        // Now reorder: put lesson 2 first
        LessonDto.ReorderRequest reorderReq =
                new LessonDto.ReorderRequest(List.of(lesson2Id, createdLessonId));

        mockMvc.perform(put("/api/v1/lessons/course/" + COURSE_ID + "/reorder")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reorderReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lessonId").value(lesson2Id))
                .andExpect(jsonPath("$[0].orderIndex").value(1));
    }

    // ── Internal count endpoint ───────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("GET /lessons/internal/course/{id}/count — returns lesson count")
    void countLessons_internal() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/internal/course/" + COURSE_ID + "/count"))
                .andExpect(status().isOk());
    }

    // ── Remove resource ───────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("DELETE /lessons/{id}/resources/{rid} — instructor removes resource")
    void removeResource_instructorSuccess() throws Exception {
        mockMvc.perform(
                delete("/api/v1/lessons/" + createdLessonId + "/resources/" + createdResourceId)
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(resourceRepository.findById(createdResourceId)).isEmpty();
    }

    // ── Delete lesson ─────────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("DELETE /lessons/{id} — admin deletes lesson")
    void deleteLesson_adminSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/lessons/" + createdLessonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(lessonRepository.findById(createdLessonId)).isEmpty();
    }
}
