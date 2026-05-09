package com.edulearn.notification.repository;

import com.edulearn.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIsRead(Long userId, boolean isRead);
    long countByUserIdAndIsRead(Long userId, boolean isRead);
    List<Notification> findByType(Notification.NotificationType type);
    List<Notification> findByRelatedEntityId(Long relatedEntityId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :uid AND n.isRead = false")
    int markAllReadByUserId(@Param("uid") Long userId);
}
