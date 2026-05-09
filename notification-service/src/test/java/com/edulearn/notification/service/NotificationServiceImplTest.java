package com.edulearn.notification.service;

import com.edulearn.notification.dto.NotificationDto;
import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.exception.NotificationNotFoundException;
import com.edulearn.notification.repository.NotificationRepository;
import com.edulearn.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock JavaMailSender          mailSender;
    @InjectMocks NotificationServiceImpl service;

    private Notification sample;

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@edulearn.com");
        sample = Notification.builder()
                .notificationId(1L).userId(10L)
                .type(Notification.NotificationType.ENROLLMENT)
                .title("Enrolled!").message("You enrolled in Spring Boot Mastery.")
                .isRead(false).relatedEntityId(20L).relatedEntityType("COURSE").build();
    }

    // ── sendNotification ──────────────────────────────────────────────────────

    @Test @DisplayName("sendNotification — saves and returns notification")
    void sendNotification_success() {
        when(notificationRepository.save(any())).thenReturn(sample);
        NotificationDto.SendRequest req = NotificationDto.SendRequest.builder()
                .userId(10L).type(Notification.NotificationType.ENROLLMENT)
                .title("Enrolled!").message("You enrolled in Spring Boot Mastery.")
                .relatedEntityId(20L).relatedEntityType("COURSE").build();
        NotificationDto.NotificationResponse resp = service.sendNotification(req);
        assertThat(resp.getTitle()).isEqualTo("Enrolled!");
        assertThat(resp.getIsRead()).isFalse();
        assertThat(resp.getType()).isEqualTo("ENROLLMENT");
        verify(notificationRepository).save(any());
    }

    // ── sendBulkNotification ──────────────────────────────────────────────────

    @Test @DisplayName("sendBulkNotification — creates one notification per user")
    void sendBulkNotification_success() {
        Notification n1 = Notification.builder().notificationId(1L).userId(10L)
                .type(Notification.NotificationType.GENERAL).title("Update")
                .message("Platform update.").isRead(false).build();
        Notification n2 = Notification.builder().notificationId(2L).userId(11L)
                .type(Notification.NotificationType.GENERAL).title("Update")
                .message("Platform update.").isRead(false).build();
        when(notificationRepository.saveAll(any())).thenReturn(List.of(n1, n2));

        NotificationDto.BulkSendRequest req = NotificationDto.BulkSendRequest.builder()
                .userIds(List.of(10L, 11L)).type(Notification.NotificationType.GENERAL)
                .title("Update").message("Platform update.").build();
        List<NotificationDto.NotificationResponse> result = service.sendBulkNotification(req);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
        assertThat(result.get(1).getUserId()).isEqualTo(11L);
    }

    // ── getByUser ─────────────────────────────────────────────────────────────

    @Test @DisplayName("getByUser — returns paginated notifications")
    void getByUser_success() {
        Page<Notification> page = new PageImpl<>(List.of(sample), PageRequest.of(0, 20), 1);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(page);
        NotificationDto.PagedResponse resp = service.getByUser(10L, 0, 20);
        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getTotalElements()).isEqualTo(1);
        assertThat(resp.getContent().get(0).getTitle()).isEqualTo("Enrolled!");
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    @Test @DisplayName("markAsRead — sets isRead=true")
    void markAsRead_success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sample));
        when(notificationRepository.save(any())).thenReturn(sample);
        NotificationDto.NotificationResponse resp = service.markAsRead(1L, 10L);
        assertThat(sample.getIsRead()).isTrue();
    }

    @Test @DisplayName("markAsRead — not found throws NotificationNotFoundException")
    void markAsRead_notFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markAsRead(99L, 10L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    // ── markAllRead ───────────────────────────────────────────────────────────

    @Test @DisplayName("markAllRead — returns count of updated notifications")
    void markAllRead_success() {
        when(notificationRepository.markAllReadByUserId(10L)).thenReturn(5);
        int count = service.markAllRead(10L);
        assertThat(count).isEqualTo(5);
    }

    // ── getUnreadCount ────────────────────────────────────────────────────────

    @Test @DisplayName("getUnreadCount — returns correct unread count")
    void getUnreadCount_success() {
        when(notificationRepository.countByUserIdAndIsRead(10L, false)).thenReturn(3L);
        NotificationDto.UnreadCountResponse resp = service.getUnreadCount(10L);
        assertThat(resp.getUnreadCount()).isEqualTo(3L);
        assertThat(resp.getUserId()).isEqualTo(10L);
    }

    // ── deleteNotification ────────────────────────────────────────────────────

    @Test @DisplayName("deleteNotification — removes notification")
    void deleteNotification_success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sample));
        service.deleteNotification(1L, 10L);
        verify(notificationRepository).delete(sample);
    }

    @Test @DisplayName("deleteNotification — not found throws NotificationNotFoundException")
    void deleteNotification_notFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteNotification(99L, 10L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    // ── sendEmailAlert ────────────────────────────────────────────────────────

    @Test @DisplayName("sendEmailAlert — calls mailSender without throwing")
    void sendEmailAlert_success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        assertThatNoException().isThrownBy(() ->
                service.sendEmailAlert(10L, "Welcome to EduLearn", "Your account is ready."));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test @DisplayName("sendEmailAlert — mail failure is logged, not thrown")
    void sendEmailAlert_mailFail_noThrow() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThatNoException().isThrownBy(() ->
                service.sendEmailAlert(10L, "Subject", "Body"));
    }
}
