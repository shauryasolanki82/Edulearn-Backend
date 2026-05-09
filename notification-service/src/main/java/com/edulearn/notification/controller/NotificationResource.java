package com.edulearn.notification.controller;

import com.edulearn.notification.dto.NotificationDto;
import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app and email notification management")
public class NotificationResource {

    private final NotificationService notificationService;

    // ── GET /api/v1/notifications/my — paginated inbox ────────────────────────
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated notifications for the authenticated user")
    public ResponseEntity<NotificationDto.PagedResponse> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return ResponseEntity.ok(notificationService.getByUser(userId, page, size));
    }

    // ── GET /api/v1/notifications/my/unread-count — badge count ──────────────
    @GetMapping("/my/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notification count (for the notification bell badge)")
    public ResponseEntity<NotificationDto.UnreadCountResponse> getUnreadCount(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    // ── PUT /api/v1/notifications/{id}/read — mark one as read ───────────────
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<NotificationDto.NotificationResponse> markAsRead(
            @PathVariable Long notificationId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    // ── PUT /api/v1/notifications/my/read-all — mark all read ────────────────
    @PutMapping("/my/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read for the authenticated user")
    public ResponseEntity<NotificationDto.ApiResponse> markAllRead(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        int count = notificationService.markAllRead(userId);
        return ResponseEntity.ok(NotificationDto.ApiResponse.builder()
                .success(true).message(count + " notifications marked as read").build());
    }

    // ── DELETE /api/v1/notifications/{id} — delete one ───────────────────────
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<NotificationDto.ApiResponse> deleteNotification(
            @PathVariable Long notificationId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(NotificationDto.ApiResponse.builder()
                .success(true).message("Notification deleted").build());
    }

    // ── POST /api/v1/notifications/bulk — admin bulk send ────────────────────
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: send a notification to multiple users at once")
    public ResponseEntity<List<NotificationDto.NotificationResponse>> sendBulk(
            @Valid @RequestBody NotificationDto.BulkSendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendBulkNotification(request));
    }

    // ── GET /api/v1/notifications/admin/user/{userId} — admin view user inbox ─
    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: get all notifications for a specific user")
    public ResponseEntity<NotificationDto.PagedResponse> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getByUser(userId, page, size));
    }

    // ── POST /api/v1/notifications/internal/send — inter-service trigger ──────
    @PostMapping("/internal/send")
    @Operation(summary = "Internal: send a notification triggered by another service")
    public ResponseEntity<NotificationDto.NotificationResponse> internalSend(
            @Valid @RequestBody NotificationDto.SendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendNotification(request));
    }

    // ── POST /api/v1/notifications/internal/send-email — email alert ─────────
    @PostMapping("/internal/send-email")
    @Operation(summary = "Internal: send an email alert to a user")
    public ResponseEntity<NotificationDto.ApiResponse> sendEmail(
            @RequestParam Long userId,
            @RequestParam String subject,
            @RequestParam String body) {
        notificationService.sendEmailAlert(userId, subject, body);
        return ResponseEntity.ok(NotificationDto.ApiResponse.builder()
                .success(true).message("Email dispatched").build());
    }
}
