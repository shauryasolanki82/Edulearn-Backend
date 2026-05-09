package com.edulearn.notification.service.impl;

import com.edulearn.notification.dto.NotificationDto;
import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.exception.NotificationNotFoundException;
import com.edulearn.notification.repository.NotificationRepository;
import com.edulearn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender          mailSender;

    @Value("${spring.mail.username:noreply@edulearn.com}")
    private String fromEmail;

    // ── Send single notification ──────────────────────────────────────────────

    @Override @Transactional
    public NotificationDto.NotificationResponse sendNotification(NotificationDto.SendRequest req) {
        Notification n = Notification.builder()
                .userId(req.getUserId()).type(req.getType())
                .title(req.getTitle()).message(req.getMessage())
                .relatedEntityId(req.getRelatedEntityId())
                .relatedEntityType(req.getRelatedEntityType())
                .isRead(false).build();
        Notification saved = notificationRepository.save(n);
        log.info("Notification sent: userId={} type={}", req.getUserId(), req.getType());
        return toResponse(saved);
    }

    // ── Send bulk notifications ───────────────────────────────────────────────

    @Override @Transactional
    public List<NotificationDto.NotificationResponse> sendBulkNotification(NotificationDto.BulkSendRequest req) {
        List<Notification> notifications = req.getUserIds().stream()
                .map(uid -> Notification.builder()
                        .userId(uid).type(req.getType()).title(req.getTitle()).message(req.getMessage())
                        .relatedEntityId(req.getRelatedEntityId())
                        .relatedEntityType(req.getRelatedEntityType()).isRead(false).build())
                .collect(Collectors.toList());
        List<Notification> saved = notificationRepository.saveAll(notifications);
        log.info("Bulk notification sent to {} users, type={}", req.getUserIds().size(), req.getType());
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Get notifications for user (paginated) ────────────────────────────────

    @Override
    public NotificationDto.PagedResponse getByUser(Long userId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> pg = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        return NotificationDto.PagedResponse.builder()
                .content(pg.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(pg.getNumber()).size(pg.getSize())
                .totalElements(pg.getTotalElements()).totalPages(pg.getTotalPages()).build();
    }

    // ── Mark single notification as read ─────────────────────────────────────

    @Override @Transactional
    public NotificationDto.NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationId));
        n.setIsRead(true);
        return toResponse(notificationRepository.save(n));
    }

    // ── Mark all notifications as read ────────────────────────────────────────

    @Override @Transactional
    public int markAllRead(Long userId) {
        int count = notificationRepository.markAllReadByUserId(userId);
        log.info("Marked {} notifications as read for userId={}", count, userId);
        return count;
    }

    // ── Get unread count (for notification bell) ──────────────────────────────

    @Override
    public NotificationDto.UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsRead(userId, false);
        return NotificationDto.UnreadCountResponse.builder().userId(userId).unreadCount(count).build();
    }

    // ── Delete notification ───────────────────────────────────────────────────

    @Override @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationId));
        notificationRepository.delete(n);
        log.info("Notification {} deleted by userId={}", notificationId, userId);
    }

    // ── Send email alert ──────────────────────────────────────────────────────

    @Override
    public void sendEmailAlert(Long userId, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo("user_" + userId + "@edulearn.com"); // In prod: look up real email from auth-service
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to userId={} subject='{}'", userId, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to userId={}: {}", userId, e.getMessage());
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private NotificationDto.NotificationResponse toResponse(Notification n) {
        return NotificationDto.NotificationResponse.builder()
                .notificationId(n.getNotificationId()).userId(n.getUserId())
                .type(n.getType().name()).title(n.getTitle()).message(n.getMessage())
                .isRead(n.getIsRead()).relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType()).createdAt(n.getCreatedAt()).build();
    }
}
