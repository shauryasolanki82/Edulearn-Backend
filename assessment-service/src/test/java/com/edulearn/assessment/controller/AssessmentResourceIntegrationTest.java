package com.edulearn.assessment.controller;

import com.edulearn.assessment.dto.AssessmentDto;
import com.edulearn.assessment.entity.Question;
import com.edulearn.assessment.repository.*;
import com.edulearn.assessment.security.JwtUtil;
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
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AssessmentResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired QuizRepository quizRepository;
    @Autowired AttemptRepository attemptRepository;
    @Autowired JwtUtil jwtUtil;

    private static String instructorToken, studentToken, adminToken;
    private static Long quizId;
    private static Long questionId;

    @BeforeEach void setUp() {
        // FIX: assessment-service JwtUtil (like lesson/enrollment services) has NO
        // jwtExpirationMs field — expiry is hardcoded inside generateToken().
        // Only jwtSecret needs to be injected via ReflectionTestUtils.
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        // REMOVED: ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L);

        instructorToken = jwtUtil.generateToken(5L,  "inst@test.com",    "INSTRUCTOR");
        studentToken    = jwtUtil.generateToken(20L, "student@test.com", "STUDENT");
        adminToken      = jwtUtil.generateToken(1L,  "admin@test.com",   "ADMIN");
    }

    @Test @Order(1) @DisplayName("POST /quizzes — instructor creates quiz")
    void createQuiz() throws Exception {
        quizRepository.deleteAll(); attemptRepository.deleteAll();
        AssessmentDto.QuizRequest req = AssessmentDto.QuizRequest.builder()
                .courseId(10L).title("Spring Quiz").timeLimitMinutes(20)
                .passingScore(60.0).maxAttempts(2).build();
        MvcResult r = mockMvc.perform(post("/api/v1/quizzes")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring Quiz"))
                .andExpect(jsonPath("$.isPublished").value(false)).andReturn();
        quizId = objectMapper.readValue(r.getResponse().getContentAsString(),
                AssessmentDto.QuizResponse.class).getQuizId();
        assertThat(quizId).isNotNull();
    }

    @Test @Order(2) @DisplayName("POST /quizzes/{id}/questions — instructor adds question")
    void addQuestion() throws Exception {
        AssessmentDto.QuestionRequest req = AssessmentDto.QuestionRequest.builder()
                .text("What is Spring Boot?").type(Question.QuestionType.MCQ)
                .options(List.of("Framework","Database","Language","OS"))
                .correctAnswer("0").marks(2).build();
        MvcResult r = mockMvc.perform(post("/api/v1/quizzes/" + quizId + "/questions")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("What is Spring Boot?")).andReturn();
        questionId = objectMapper.readValue(r.getResponse().getContentAsString(),
                AssessmentDto.QuestionResponse.class).getQuestionId();
    }

    @Test @Order(3) @DisplayName("PUT /quizzes/{id}/publish — publishes quiz")
    void publishQuiz() throws Exception {
        mockMvc.perform(put("/api/v1/quizzes/" + quizId + "/publish")
                        .header("Authorization", "Bearer " + instructorToken)
                        .param("publish", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublished").value(true));
    }

    @Test @Order(4) @DisplayName("GET /quizzes/{id}/questions — student sees no correctAnswer field")
    void getQuestionsForStudent() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + quizId + "/questions")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("What is Spring Boot?"))
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
    }

    @Test @Order(5) @DisplayName("GET /quizzes/{id}/questions — instructor sees correctAnswer")
    void getQuestionsForInstructor() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + quizId + "/questions")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correctAnswer").value("0"));
    }

    @Test @Order(6) @DisplayName("POST /quizzes/{id}/start — student starts attempt")
    void startAttempt() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes/" + quizId + "/start")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(20))
                .andExpect(jsonPath("$.quizId").value(quizId));
    }

    @Test @Order(7) @DisplayName("POST /attempts/submit — auto-grades attempt")
    void submitAttempt() throws Exception {
        AssessmentDto.SubmitAttemptRequest req = new AssessmentDto.SubmitAttemptRequest(
                quizId, Map.of(questionId, "0")); // correct answer
        mockMvc.perform(post("/api/v1/attempts/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.score").value(100.0));
    }

    @Test @Order(8) @DisplayName("GET /attempts/my — returns student attempt history")
    void getMyAttempts() throws Exception {
        mockMvc.perform(get("/api/v1/attempts/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(20));
    }

    @Test @Order(9) @DisplayName("GET /quizzes/{id}/best-score — returns best attempt")
    void getBestScore() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + quizId + "/best-score")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber());
    }

    @Test @Order(10) @DisplayName("DELETE /quizzes/{id} — instructor deletes quiz")
    void deleteQuiz() throws Exception {
        mockMvc.perform(delete("/api/v1/quizzes/" + quizId)
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(quizRepository.findById(quizId)).isEmpty();
    }
}