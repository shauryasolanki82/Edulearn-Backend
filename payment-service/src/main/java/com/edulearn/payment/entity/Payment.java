package com.edulearn.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long studentId;

    @Column
    private Long courseId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode mode;

    @Column(unique = true)
    private String transactionId;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column
    private String failureReason;

    @CreationTimestamp
    @Column(name = "paid_at", updatable = false)
    private LocalDateTime paidAt;

    public enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
    public enum PaymentMode   { CARD, UPI, WALLET, NET_BANKING }
}
