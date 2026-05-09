package com.edulearn.payment.controller;

import com.edulearn.payment.dto.PaymentDto;
import com.edulearn.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Course purchases, subscriptions, and refunds")
public class PaymentResource {

    private final PaymentService paymentService;

    // POST /api/v1/payments — process a course purchase
    @PostMapping
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Process a one-time course purchase payment")
    public ResponseEntity<PaymentDto.PaymentResponse> processPayment(
            @Valid @RequestBody PaymentDto.PaymentRequest request,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(studentId, request));
    }

    // GET /api/v1/payments/my — student's own payment history
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment history for the authenticated student")
    public ResponseEntity<List<PaymentDto.PaymentResponse>> getMyPayments(HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(paymentService.getPaymentsByStudent(studentId));
    }

    // GET /api/v1/payments/student/{studentId} — admin view
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: get payment history for any student")
    public ResponseEntity<List<PaymentDto.PaymentResponse>> getPaymentsByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(paymentService.getPaymentsByStudent(studentId));
    }

    // POST /api/v1/payments/{paymentId}/refund — refund
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: refund a payment")
    public ResponseEntity<PaymentDto.PaymentResponse> refund(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }

    // POST /api/v1/payments/subscriptions — subscribe
    @PostMapping("/subscriptions")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Subscribe to a Monthly or Annual plan")
    public ResponseEntity<PaymentDto.SubscriptionResponse> subscribe(
            @Valid @RequestBody PaymentDto.SubscriptionRequest request,
            HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.subscribe(studentId, request));
    }

    // GET /api/v1/payments/subscriptions/my — current subscription
    @GetMapping("/subscriptions/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the authenticated student's active subscription")
    public ResponseEntity<PaymentDto.SubscriptionResponse> getMySubscription(HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(paymentService.getSubscription(studentId));
    }

    // DELETE /api/v1/payments/subscriptions/my — cancel
    @DeleteMapping("/subscriptions/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel the authenticated student's active subscription")
    public ResponseEntity<PaymentDto.SubscriptionResponse> cancelSubscription(HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(paymentService.cancelSubscription(studentId));
    }

    // POST /api/v1/payments/subscriptions/my/renew — renew
    @PostMapping("/subscriptions/my/renew")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Renew the authenticated student's active subscription")
    public ResponseEntity<PaymentDto.SubscriptionResponse> renewSubscription(HttpServletRequest httpRequest) {
        Long studentId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(paymentService.renewSubscription(studentId));
    }

    // GET /api/v1/payments/subscriptions/active — inter-service check
    @GetMapping("/subscriptions/active")
    @Operation(summary = "Check if a student has an active subscription (inter-service)")
    public ResponseEntity<Boolean> isSubscriptionActive(@RequestParam Long studentId) {
        return ResponseEntity.ok(paymentService.isSubscriptionActive(studentId));
    }

    // GET /api/v1/payments/admin/revenue — platform analytics
    @GetMapping("/admin/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: get platform-wide revenue analytics")
    public ResponseEntity<PaymentDto.RevenueResponse> getRevenue() {
        return ResponseEntity.ok(paymentService.getPlatformRevenue());
    }
}
