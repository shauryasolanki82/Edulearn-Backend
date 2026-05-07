package com.edulearn.payment.repository;

import com.edulearn.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment>    findByStudentId(Long studentId);
    List<Payment>    findByCourseId(Long courseId);
    List<Payment>    findByStatus(Payment.PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.studentId=:sid AND p.status='SUCCESS'")
    BigDecimal sumAmountByStudentId(@Param("sid") Long studentId);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.status='SUCCESS'")
    BigDecimal totalRevenue();

    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, Payment.PaymentStatus status);
}
