package com.edulearn.payment.controller;

import com.edulearn.payment.dto.PaymentDto;
import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.Subscription;
import com.edulearn.payment.repository.PaymentRepository;
import com.edulearn.payment.repository.SubscriptionRepository;
import com.edulearn.payment.security.JwtUtil;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PaymentRepository paymentRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired JwtUtil jwtUtil;
    @MockBean RestTemplate restTemplate;

    private static String studentToken;
    private static String adminToken;
    private static Long createdPaymentId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret",
                "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=");
        studentToken = jwtUtil.generateToken(10L, "student@test.com", "STUDENT");
        adminToken   = jwtUtil.generateToken(1L,  "admin@test.com",   "ADMIN");
    }

    @Test @Order(1) @DisplayName("POST /payments — student purchases a course")
    void processPayment_success() throws Exception {
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();

        PaymentDto.PaymentRequest req = PaymentDto.PaymentRequest.builder()
                .courseId(20L).amount(BigDecimal.valueOf(999))
                .mode(Payment.PaymentMode.UPI).build();

        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.studentId").value(10))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andReturn();

        PaymentDto.PaymentResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), PaymentDto.PaymentResponse.class);
        createdPaymentId = resp.getPaymentId();
        assertThat(createdPaymentId).isNotNull();
    }

    @Test @Order(2) @DisplayName("GET /payments/my — returns payment history")
    void getMyPayments() throws Exception {
        mockMvc.perform(get("/api/v1/payments/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test @Order(3) @DisplayName("POST /payments/{id}/refund — admin refunds payment")
    void refundPayment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + createdPaymentId + "/refund")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test @Order(4) @DisplayName("POST /payments/subscriptions — student subscribes MONTHLY")
    void subscribe_monthly() throws Exception {
        PaymentDto.SubscriptionRequest req = new PaymentDto.SubscriptionRequest(
                Subscription.SubscriptionPlan.MONTHLY, Payment.PaymentMode.CARD, true);

        mockMvc.perform(post("/api/v1/payments/subscriptions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plan").value("MONTHLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.autoRenew").value(true));
    }

    @Test @Order(5) @DisplayName("GET /payments/subscriptions/my — returns active subscription")
    void getMySubscription() throws Exception {
        mockMvc.perform(get("/api/v1/payments/subscriptions/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("MONTHLY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

   @Test @Order(6) @DisplayName("GET /payments/subscriptions/active — inter-service check")
void isSubscriptionActive() throws Exception {
    mockMvc.perform(get("/api/v1/payments/subscriptions/active")
                    .header("Authorization", "Bearer " + studentToken)  // ← add this
                    .param("studentId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));
}

    @Test @Order(7) @DisplayName("POST /payments/subscriptions/my/renew — renews subscription")
    void renewSubscription() throws Exception {
        mockMvc.perform(post("/api/v1/payments/subscriptions/my/renew")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test @Order(8) @DisplayName("GET /payments/admin/revenue — admin gets revenue analytics")
    void getRevenue() throws Exception {
        mockMvc.perform(get("/api/v1/payments/admin/revenue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").isNumber());
    }

    @Test @Order(9) @DisplayName("DELETE /payments/subscriptions/my — cancels subscription")
    void cancelSubscription() throws Exception {
        mockMvc.perform(delete("/api/v1/payments/subscriptions/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
