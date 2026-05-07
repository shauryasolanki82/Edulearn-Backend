package com.edulearn.payment.service.impl;

import com.edulearn.payment.dto.PaymentDto;
import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.Subscription;
import com.edulearn.payment.exception.PaymentNotFoundException;
import com.edulearn.payment.exception.SubscriptionNotFoundException;
import com.edulearn.payment.repository.PaymentRepository;
import com.edulearn.payment.repository.SubscriptionRepository;
import com.edulearn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository     paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RestTemplate           restTemplate;

    @Value("${app.enrollment-service.url}") private String enrollmentServiceUrl;

    // ── Process Payment ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDto.PaymentResponse processPayment(Long studentId, PaymentDto.PaymentRequest req) {
        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Payment payment = Payment.builder()
                .studentId(studentId)
                .courseId(req.getCourseId())
                .amount(req.getAmount())
                .mode(req.getMode())
                .transactionId(txId)
                .currency(req.getCurrency() != null ? req.getCurrency() : "INR")
                .status(Payment.PaymentStatus.SUCCESS)   // In prod: call payment gateway
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} processed for student {} course {}", txId, studentId, req.getCourseId());

        // Trigger enrollment after successful payment
        if (saved.getStatus() == Payment.PaymentStatus.SUCCESS) {
            triggerEnrollment(studentId, req.getCourseId());
        }
        return toPaymentResponse(saved);
    }

    // ── Get Payments By Student ───────────────────────────────────────────────

    @Override
    public List<PaymentDto.PaymentResponse> getPaymentsByStudent(Long studentId) {
        return paymentRepository.findByStudentId(studentId)
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    // ── Refund Payment ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDto.PaymentResponse refundPayment(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        p.setStatus(Payment.PaymentStatus.REFUNDED);
        log.info("Payment {} refunded", paymentId);
        return toPaymentResponse(paymentRepository.save(p));
    }

    // ── Subscribe ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDto.SubscriptionResponse subscribe(Long studentId, PaymentDto.SubscriptionRequest req) {
        // Cancel any existing active subscription
        subscriptionRepository.findByStudentIdAndStatus(studentId, Subscription.SubscriptionStatus.ACTIVE)
                .ifPresent(s -> { s.setStatus(Subscription.SubscriptionStatus.CANCELLED); subscriptionRepository.save(s); });

        LocalDate start = LocalDate.now();
        LocalDate end   = req.getPlan() == Subscription.SubscriptionPlan.ANNUAL
                ? start.plusYears(1) : start.plusMonths(1);
        BigDecimal price = req.getPlan() == Subscription.SubscriptionPlan.ANNUAL
                ? BigDecimal.valueOf(2999) : BigDecimal.valueOf(299);

        Subscription sub = Subscription.builder()
                .studentId(studentId).plan(req.getPlan())
                .startDate(start).endDate(end)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .amountPaid(price)
                .autoRenew(req.isAutoRenew())
                .build();

        Subscription saved = subscriptionRepository.save(sub);
        log.info("Student {} subscribed to {} plan until {}", studentId, req.getPlan(), end);
        return toSubResponse(saved);
    }

    // ── Get Subscription ──────────────────────────────────────────────────────

    @Override
    public PaymentDto.SubscriptionResponse getSubscription(Long studentId) {
        Subscription sub = subscriptionRepository
                .findByStudentIdAndStatus(studentId, Subscription.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription for student: " + studentId));
        return toSubResponse(sub);
    }

    // ── Cancel Subscription ───────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDto.SubscriptionResponse cancelSubscription(Long studentId) {
        Subscription sub = subscriptionRepository
                .findByStudentIdAndStatus(studentId, Subscription.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription for student: " + studentId));
        sub.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        sub.setAutoRenew(false);
        log.info("Subscription cancelled for student {}", studentId);
        return toSubResponse(subscriptionRepository.save(sub));
    }

    // ── Renew Subscription ────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDto.SubscriptionResponse renewSubscription(Long studentId) {
        Subscription sub = subscriptionRepository
                .findByStudentIdAndStatus(studentId, Subscription.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription for student: " + studentId));
        LocalDate newEnd = sub.getPlan() == Subscription.SubscriptionPlan.ANNUAL
                ? sub.getEndDate().plusYears(1) : sub.getEndDate().plusMonths(1);
        sub.setEndDate(newEnd);
        log.info("Subscription renewed for student {} until {}", studentId, newEnd);
        return toSubResponse(subscriptionRepository.save(sub));
    }

    // ── Is Subscription Active ────────────────────────────────────────────────

    @Override
    public boolean isSubscriptionActive(Long studentId) {
        return subscriptionRepository.findByStudentIdAndStatus(studentId, Subscription.SubscriptionStatus.ACTIVE)
                .map(s -> !s.getEndDate().isBefore(LocalDate.now()))
                .orElse(false);
    }

    // ── Platform Revenue ──────────────────────────────────────────────────────

    @Override
    public PaymentDto.RevenueResponse getPlatformRevenue() {
        BigDecimal total = paymentRepository.totalRevenue();
        long payments    = paymentRepository.findByStatus(Payment.PaymentStatus.SUCCESS).size();
        long activeSubs  = subscriptionRepository.countByPlan(Subscription.SubscriptionPlan.MONTHLY)
                         + subscriptionRepository.countByPlan(Subscription.SubscriptionPlan.ANNUAL);
        return PaymentDto.RevenueResponse.builder()
                .totalRevenue(total).totalPayments(payments).activeSubscriptions(activeSubs).build();
    }

    // ── Inter-service: trigger enrollment ────────────────────────────────────

    private void triggerEnrollment(Long studentId, Long courseId) {
        try {
            String url = enrollmentServiceUrl + "/api/v1/enrollments/internal/enroll"
                    + "?studentId=" + studentId + "&courseId=" + courseId;
            restTemplate.postForEntity(url, null, Void.class);
            log.debug("Enrollment triggered for student={} course={}", studentId, courseId);
        } catch (Exception e) {
            log.warn("Could not auto-enroll student {} in course {}: {}", studentId, courseId, e.getMessage());
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private PaymentDto.PaymentResponse toPaymentResponse(Payment p) {
        return PaymentDto.PaymentResponse.builder()
                .paymentId(p.getPaymentId()).studentId(p.getStudentId()).courseId(p.getCourseId())
                .amount(p.getAmount()).status(p.getStatus().name()).mode(p.getMode().name())
                .transactionId(p.getTransactionId()).currency(p.getCurrency()).paidAt(p.getPaidAt()).build();
    }

    private PaymentDto.SubscriptionResponse toSubResponse(Subscription s) {
        return PaymentDto.SubscriptionResponse.builder()
                .subscriptionId(s.getSubscriptionId()).studentId(s.getStudentId())
                .plan(s.getPlan().name()).startDate(s.getStartDate()).endDate(s.getEndDate())
                .status(s.getStatus().name()).amountPaid(s.getAmountPaid())
                .autoRenew(s.getAutoRenew()).createdAt(s.getCreatedAt()).build();
    }
}
