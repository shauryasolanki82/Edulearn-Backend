package com.edulearn.payment.dto;

import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.Subscription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentRequest {
        @NotNull private Long courseId;
        @NotNull @Positive private BigDecimal amount;
        @NotNull private Payment.PaymentMode mode;
        private String currency;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentResponse {
        private Long paymentId;
        private Long studentId;
        private Long courseId;
        private BigDecimal amount;
        private String status;
        private String mode;
        private String transactionId;
        private String currency;
        private LocalDateTime paidAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubscriptionRequest {
        @NotNull private Subscription.SubscriptionPlan plan;
        @NotNull private Payment.PaymentMode mode;
        private boolean autoRenew;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubscriptionResponse {
        private Long subscriptionId;
        private Long studentId;
        private String plan;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private BigDecimal amountPaid;
        private Boolean autoRenew;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RevenueResponse {
        private BigDecimal totalRevenue;
        private long totalPayments;
        private long activeSubscriptions;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}
