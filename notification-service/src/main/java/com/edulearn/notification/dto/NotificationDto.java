package com.edulearn.notification.dto;

import com.edulearn.notification.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SendRequest {
        @NotNull  private Long userId;
        @NotNull  private Notification.NotificationType type;
        @NotBlank private String title;
        @NotBlank private String message;
        private Long relatedEntityId;
        private String relatedEntityType;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BulkSendRequest {
        @NotNull  private List<Long> userIds;
        @NotNull  private Notification.NotificationType type;
        @NotBlank private String title;
        @NotBlank private String message;
        private Long relatedEntityId;
        private String relatedEntityType;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NotificationResponse {
        private Long notificationId;
        private Long userId;
        private String type;
        private String title;
        private String message;
        private Boolean isRead;
        private Long relatedEntityId;
        private String relatedEntityType;
        private LocalDateTime createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UnreadCountResponse {
        private Long userId;
        private long unreadCount;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PagedResponse {
        private List<NotificationResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}
