package com.edulearn.notification.service;

import com.edulearn.notification.dto.NotificationDto;
import java.util.List;

public interface NotificationService {
    NotificationDto.NotificationResponse sendNotification(NotificationDto.SendRequest request);
    List<NotificationDto.NotificationResponse> sendBulkNotification(NotificationDto.BulkSendRequest request);
    NotificationDto.PagedResponse getByUser(Long userId, int page, int size);
    NotificationDto.NotificationResponse markAsRead(Long notificationId, Long userId);
    int markAllRead(Long userId);
    NotificationDto.UnreadCountResponse getUnreadCount(Long userId);
    void deleteNotification(Long notificationId, Long userId);
    void sendEmailAlert(Long userId, String subject, String body);
}
