package com.edulearn.payment.repository;

import com.edulearn.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByStudentIdAndStatus(Long studentId, Subscription.SubscriptionStatus status);
    List<Subscription>     findByStudentId(Long studentId);
    List<Subscription>     findByEndDateBeforeAndStatus(LocalDate date, Subscription.SubscriptionStatus status);
    long                   countByPlan(Subscription.SubscriptionPlan plan);
    boolean existsByStudentIdAndStatus(Long studentId, Subscription.SubscriptionStatus status);
}
