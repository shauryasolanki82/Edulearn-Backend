package com.edulearn.discussion.controller;

import com.edulearn.discussion.dto.DiscussionDto;
import com.edulearn.discussion.repository.*;
import com.edulearn.discussion.security.JwtUtil;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DiscussionResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ThreadRepository threadRepository;
    @Autowired ReplyRepository replyRepository;
    @Autowired JwtUtil jwtUtil;

    private static String studentToken, instructorToken, adminToken;
    private static Long threadId, replyId;

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        studentToken    = jwtUtil.generateToken(10L, "student@test.com",    "STUDENT");
        instructorToken = jwtUtil.generateToken(5L,  "instructor@test.com", "INSTRUCTOR");
        adminToken      = jwtUtil.generateToken(1L,  "admin@test.com",      "ADMIN");
    }

    @Test @Order(1) @DisplayName("POST /threads — student creates thread")
    void createThread() throws Exception {
        threadRepository.deleteAll();
        DiscussionDto.ThreadRequest req = new DiscussionDto.ThreadRequest(
                10L, null, "How does Spring work?", "Can someone explain Spring Boot?");
        MvcResult r = mockMvc.perform(post("/api/v1/threads")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("How does Spring work?"))
                .andExpect(jsonPath("$.authorId").value(10))
                .andExpect(jsonPath("$.isPinned").value(false)).andReturn();
        threadId = objectMapper.readValue(r.getResponse().getContentAsString(),
                DiscussionDto.ThreadResponse.class).getThreadId();
        assertThat(threadId).isNotNull();
    }

    @Test @Order(2) @DisplayName("GET /threads/course/{id} — lists threads for course")
    void getThreadsByCourse() throws Exception {
        mockMvc.perform(get("/api/v1/threads/course/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(10));
    }

    @Test @Order(3) @DisplayName("POST /threads/{id}/replies — student posts reply")
    void postReply() throws Exception {
        DiscussionDto.ReplyRequest req = new DiscussionDto.ReplyRequest(
                "Spring Boot auto-configures based on your classpath.");
        MvcResult r = mockMvc.perform(post("/api/v1/threads/" + threadId + "/replies")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Spring Boot auto-configures based on your classpath."))
                .andExpect(jsonPath("$.upvotes").value(0)).andReturn();
        replyId = objectMapper.readValue(r.getResponse().getContentAsString(),
                DiscussionDto.ReplyResponse.class).getReplyId();
    }

    @Test @Order(4) @DisplayName("GET /threads/{id} — returns thread with replies")
    void getThread() throws Exception {
        mockMvc.perform(get("/api/v1/threads/" + threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thread.title").value("How does Spring work?"))
                .andExpect(jsonPath("$.replies[0].body").isNotEmpty());
    }

    @Test @Order(5) @DisplayName("POST /replies/{id}/upvote — increments upvote count")
    void upvoteReply() throws Exception {
        mockMvc.perform(post("/api/v1/replies/" + replyId + "/upvote")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1));
    }

    @Test @Order(6) @DisplayName("PUT /replies/{id}/accept — thread author accepts reply")
    void acceptReply() throws Exception {
        mockMvc.perform(put("/api/v1/replies/" + replyId + "/accept")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAccepted").value(true));
    }

    @Test @Order(7) @DisplayName("PUT /threads/{id}/pin — instructor pins thread")
    void pinThread() throws Exception {
        mockMvc.perform(put("/api/v1/threads/" + threadId + "/pin")
                        .header("Authorization", "Bearer " + instructorToken)
                        .param("pin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPinned").value(true));
    }

    @Test @Order(8) @DisplayName("PUT /threads/{id}/close — instructor closes thread")
    void closeThread() throws Exception {
        mockMvc.perform(put("/api/v1/threads/" + threadId + "/close")
                        .header("Authorization", "Bearer " + instructorToken)
                        .param("close", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isClosed").value(true));
    }

    @Test @Order(9) @DisplayName("GET /threads/course/{id}/search — keyword search")
    void searchThreads() throws Exception {
        mockMvc.perform(get("/api/v1/threads/course/10/search").param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").isNotEmpty());
    }

    @Test @Order(10) @DisplayName("DELETE /replies/{id} — admin deletes reply")
    void deleteReply() throws Exception {
        mockMvc.perform(delete("/api/v1/replies/" + replyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(replyRepository.findById(replyId)).isEmpty();
    }

    @Test @Order(11) @DisplayName("DELETE /threads/{id} — author deletes thread")
    void deleteThread() throws Exception {
        mockMvc.perform(delete("/api/v1/threads/" + threadId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(threadRepository.findById(threadId)).isEmpty();
    }
}
