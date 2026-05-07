package com.edulearn.payment.service;

import com.edulearn.payment.dto.PaymentDto;
import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.Subscription;
import com.edulearn.payment.exception.PaymentNotFoundException;
import com.edulearn.payment.exception.SubscriptionNotFoundException;
import com.edulearn.payment.repository.PaymentRepository;
import com.edulearn.payment.repository.SubscriptionRepository;
import com.edulearn.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock PaymentRepository paymentRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock RestTemplate restTemplate;
    @InjectMocks PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "enrollmentServiceUrl", "http://localhost:8084");
    }

    // ── processPayment ────────────────────────────────────────────────────────

    @Test @DisplayName("processPayment — creates SUCCESS payment and triggers enrollment")
    void processPayment_success() {
        Payment saved = Payment.builder().paymentId(1L).studentId(10L).courseId(20L)
                .amount(BigDecimal.valueOf(999)).status(Payment.PaymentStatus.SUCCESS)
                .mode(Payment.PaymentMode.UPI).transactionId("TXN-ABC").currency("INR").build();
        when(paymentRepository.save(any())).thenReturn(saved);

        PaymentDto.PaymentRequest req = PaymentDto.PaymentRequest.builder()
                .courseId(20L).amount(BigDecimal.valueOf(999))
                .mode(Payment.PaymentMode.UPI).build();

        PaymentDto.PaymentResponse resp = paymentService.processPayment(10L, req);
        assertThat(resp.getStatus()).isEqualTo("SUCCESS");
        assertThat(resp.getStudentId()).isEqualTo(10L);
        assertThat(resp.getCourseId()).isEqualTo(20L);
        verify(paymentRepository).save(any());
    }

    @Test @DisplayName("processPayment — generates unique transaction ID")
    void processPayment_uniqueTxnId() {
        Payment p1 = Payment.builder().paymentId(1L).studentId(10L).courseId(20L)
                .amount(BigDecimal.valueOf(999)).status(Payment.PaymentStatus.SUCCESS)
                .mode(Payment.PaymentMode.CARD).transactionId("TXN-A").currency("INR").build();
        Payment p2 = Payment.builder().paymentId(2L).studentId(10L).courseId(21L)
                .amount(BigDecimal.valueOf(499)).status(Payment.PaymentStatus.SUCCESS)
                .mode(Payment.PaymentMode.CARD).transactionId("TXN-B").currency("INR").build();
        when(paymentRepository.save(any())).thenReturn(p1).thenReturn(p2);

        PaymentDto.PaymentRequest req1 = PaymentDto.PaymentRequest.builder().courseId(20L)
                .amount(BigDecimal.valueOf(999)).mode(Payment.PaymentMode.CARD).build();
        PaymentDto.PaymentRequest req2 = PaymentDto.PaymentRequest.builder().courseId(21L)
                .amount(BigDecimal.valueOf(499)).mode(Payment.PaymentMode.CARD).build();

        PaymentDto.PaymentResponse r1 = paymentService.processPayment(10L, req1);
        PaymentDto.PaymentResponse r2 = paymentService.processPayment(10L, req2);
        assertThat(r1.getTransactionId()).isNotEqualTo(r2.getTransactionId());
    }

    // ── getPaymentsByStudent ──────────────────────────────────────────────────

    @Test @DisplayName("getPaymentsByStudent — returns list")
    void getPaymentsByStudent() {
        Payment p = Payment.builder().paymentId(1L).studentId(10L).courseId(20L)
                .amount(BigDecimal.valueOf(999)).status(Payment.PaymentStatus.SUCCESS)
                .mode(Payment.PaymentMode.CARD).transactionId("TX1").currency("INR").build();
        when(paymentRepository.findByStudentId(10L)).thenReturn(List.of(p));
        assertThat(paymentService.getPaymentsByStudent(10L)).hasSize(1);
    }

    // ── refundPayment ─────────────────────────────────────────────────────────

    @Test @DisplayName("refundPayment — sets status to REFUNDED")
    void refundPayment_success() {
        Payment p = Payment.builder().paymentId(1L).studentId(10L).courseId(20L)
                .amount(BigDecimal.valueOf(999)).status(Payment.PaymentStatus.SUCCESS)
                .mode(Payment.PaymentMode.CARD).transactionId("TX1").currency("INR").build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
        when(paymentRepository.save(any())).thenReturn(p);
        PaymentDto.PaymentResponse r = paymentService.refundPayment(1L);
        assertThat(r.getStatus()).isEqualTo("REFUNDED");
    }

    @Test @DisplayName("refundPayment — not found throws PaymentNotFoundException")
    void refundPayment_notFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.refundPayment(99L))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // ── subscribe ─────────────────────────────────────────────────────────────

    @Test @DisplayName("subscribe — MONTHLY plan creates subscription ending in 1 month")
    void subscribe_monthly() {
        when(subscriptionRepository.findByStudentIdAndStatus(10L, Subscription.SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        Subscription saved = Subscription.builder().subscriptionId(1L).studentId(10L)
                .plan(Subscription.SubscriptionPlan.MONTHLY).startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1)).status(Subscription.SubscriptionStatus.ACTIVE)
                .amountPaid(BigDecimal.valueOf(299)).autoRenew(true).build();
        when(subscriptionRepository.save(any())).thenReturn(saved);

        PaymentDto.SubscriptionRequest req = new PaymentDto.SubscriptionRequest(
                Subscription.SubscriptionPlan.MONTHLY, Payment.PaymentMode.UPI, true);
        PaymentDto.SubscriptionResponse resp = paymentService.subscribe(10L, req);
        assertThat(resp.getPlan()).isEqualTo("MONTHLY");
        assertThat(resp.getStatus()).isEqualTo("ACTIVE");
    }

    @Test @DisplayName("subscribe — ANNUAL plan creates subscription ending in 1 year")
    void subscribe_annual() {
        when(subscriptionRepository.findByStudentIdAndStatus(10L, Subscription.SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        Subscription saved = Subscription.builder().subscriptionId(2L).studentId(10L)
                .plan(Subscription.SubscriptionPlan.ANNUAL).startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1)).status(Subscription.SubscriptionStatus.ACTIVE)
                .amountPaid(BigDecimal.valueOf(2999)).autoRenew(false).build();
        when(subscriptionRepository.save(any())).thenReturn(saved);

        PaymentDto.SubscriptionRequest req = new PaymentDto.SubscriptionRequest(
                Subscription.SubscriptionPlan.ANNUAL, Payment.PaymentMode.CARD, false);
        PaymentDto.SubscriptionResponse resp = paymentService.subscribe(10L, req);
        assertThat(resp.getPlan()).isEqualTo("ANNUAL");
        assertThat(resp.getEndDate()).isEqualTo(LocalDate.now().plusYears(1));
    }

    // ── cancelSubscription ────────────────────────────────────────────────────

    @Test @DisplayName("cancelSubscription — sets status CANCELLED")
    void cancelSubscription() {
        Subscription sub = Subscription.builder().subscriptionId(1L).studentId(10L)
                .plan(Subscription.SubscriptionPlan.MONTHLY).startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1)).status(Subscription.SubscriptionStatus.ACTIVE)
                .amountPaid(BigDecimal.valueOf(299)).autoRenew(true).build();
        when(subscriptionRepository.findByStudentIdAndStatus(10L, Subscription.SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenReturn(sub);
        PaymentDto.SubscriptionResponse r = paymentService.cancelSubscription(10L);
        assertThat(r.getStatus()).isEqualTo("CANCELLED");
    }

    @Test @DisplayName("cancelSubscription — no subscription throws SubscriptionNotFoundException")
    void cancelSubscription_notFound() {
        when(subscriptionRepository.findByStudentIdAndStatus(10L, Subscription.SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.cancelSubscription(10L))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    // ── isSubscriptionActive ──────────────────────────────────────────────────

    @Test @DisplayName("isSubscriptionActive — returns true for valid active subscription")
    void isSubscriptionActive_true() {
        Subscription sub = Subscription.builder().subscriptionId(1L).studentId(10L)
                .plan(Subscription.SubscriptionPlan.MONTHLY).startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1)).status(Subscription.SubscriptionStatus.ACTIVE)
                .amountPaid(BigDecimal.valueOf(299)).autoRenew(true).build();
        when(subscriptionRepository.findByStudentIdAndStatus(10L, Subscription.SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(sub));
        assertThat(paymentService.isSubscriptionActive(10L)).isTrue();
    }

    @Test @DisplayName("isSubscriptionActive — returns false when no active subscription")
    void isSubscriptionActive_false() {
        when(subscriptionRepository.findByStudentIdAndStatus(10L, Subscription.SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThat(paymentService.isSubscriptionActive(10L)).isFalse();
    }
}
