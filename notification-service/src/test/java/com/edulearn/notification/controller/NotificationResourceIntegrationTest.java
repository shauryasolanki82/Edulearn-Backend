package com.edulearn.notification.controller;

import com.edulearn.notification.dto.NotificationDto;
import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.repository.NotificationRepository;
import com.edulearn.notification.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JwtUtil jwtUtil;
    @MockBean JavaMailSender mailSender; // mock mail to avoid SMTP in tests

    private static String studentToken, adminToken;
    private static Long notificationId;

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        studentToken = jwtUtil.generateToken(10L, "student@test.com", "STUDENT");
        adminToken   = jwtUtil.generateToken(1L,  "admin@test.com",   "ADMIN");
    }

    // ── Internal send ─────────────────────────────────────────────────────────

    @Test @Order(1) @DisplayName("POST /notifications/internal/send — creates in-app notification")
    void internalSend() throws Exception {
        notificationRepository.deleteAll();
        NotificationDto.SendRequest req = NotificationDto.SendRequest.builder()
                .userId(10L).type(Notification.NotificationType.ENROLLMENT)
                .title("Enrolled!").message("You enrolled in Spring Boot Mastery.")
                .relatedEntityId(20L).relatedEntityType("COURSE").build();

        MvcResult r = mockMvc.perform(post("/api/v1/notifications/internal/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Enrolled!"))
                .andExpect(jsonPath("$.isRead").value(false))
                .andExpect(jsonPath("$.type").value("ENROLLMENT")).andReturn();

        NotificationDto.NotificationResponse resp = objectMapper.readValue(
                r.getResponse().getContentAsString(), NotificationDto.NotificationResponse.class);
        notificationId = resp.getNotificationId();
        assertThat(notificationId).isNotNull();
    }

    // ── Add a second notification ─────────────────────────────────────────────

    @Test @Order(2) @DisplayName("POST /notifications/internal/send — second notification")
    void internalSend_second() throws Exception {
        NotificationDto.SendRequest req = NotificationDto.SendRequest.builder()
                .userId(10L).type(Notification.NotificationType.PAYMENT)
                .title("Payment Confirmed").message("Your payment of ₹999 was successful.")
                .relatedEntityId(20L).relatedEntityType("COURSE").build();

        mockMvc.perform(post("/api/v1/notifications/internal/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PAYMENT"));
    }

    // ── Get my notifications ──────────────────────────────────────────────────

    @Test @Order(3) @DisplayName("GET /notifications/my — returns paginated inbox")
    void getMyNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/my")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(10));
    }

    // ── Unread count ──────────────────────────────────────────────────────────

    @Test @Order(4) @DisplayName("GET /notifications/my/unread-count — returns badge count")
    void getUnreadCount() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/my/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andExpect(jsonPath("$.userId").value(10));
    }

    // ── Mark single as read ───────────────────────────────────────────────────

    @Test @Order(5) @DisplayName("PUT /notifications/{id}/read — marks as read")
    void markAsRead() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    // ── Unread count after reading one ────────────────────────────────────────

    @Test @Order(6) @DisplayName("GET /notifications/my/unread-count — decremented after marking read")
    void getUnreadCount_afterRead() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/my/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    // ── Mark all read ─────────────────────────────────────────────────────────

    @Test @Order(7) @DisplayName("PUT /notifications/my/read-all — marks all as read")
    void markAllRead() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/my/read-all")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Unread count = 0 after read-all ──────────────────────────────────────

    @Test @Order(8) @DisplayName("GET /notifications/my/unread-count — 0 after read-all")
    void getUnreadCount_zero() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/my/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    // ── Bulk send (admin) ─────────────────────────────────────────────────────

    @Test @Order(9) @DisplayName("POST /notifications/bulk — admin sends to multiple users")
    void bulkSend() throws Exception {
        NotificationDto.BulkSendRequest req = NotificationDto.BulkSendRequest.builder()
                .userIds(List.of(10L, 11L, 12L))
                .type(Notification.NotificationType.GENERAL)
                .title("Platform Maintenance").message("Scheduled maintenance on Sunday 2am-4am.")
                .build();

        mockMvc.perform(post("/api/v1/notifications/bulk")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // ── Admin view user inbox ─────────────────────────────────────────────────

    @Test @Order(10) @DisplayName("GET /notifications/admin/user/{id} — admin views user inbox")
    void adminGetUserNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/admin/user/10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── Delete notification ───────────────────────────────────────────────────

    @Test @Order(11) @DisplayName("DELETE /notifications/{id} — deletes notification")
    void deleteNotification() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/" + notificationId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(notificationRepository.findById(notificationId)).isEmpty();
    }
}
