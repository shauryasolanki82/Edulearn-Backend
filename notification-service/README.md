# EduLearn — Notification Service (port 8089)

In-app and email notifications: single, bulk, read/unread tracking, and badge count.

## Quick Start
```sql
CREATE DATABASE edulearn_notification;
```
Configure SMTP in `application.properties`:
```properties
spring.mail.username=noreply@yourapp.com
spring.mail.password=YOUR_APP_PASSWORD
```
```bash
mvn spring-boot:run
```
Swagger: http://localhost:8089/swagger-ui.html

## Key Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| GET | `/api/v1/notifications/my` | Any | Paginated notification inbox |
| GET | `/api/v1/notifications/my/unread-count` | Any | Unread badge count |
| PUT | `/api/v1/notifications/{id}/read` | Any | Mark one as read |
| PUT | `/api/v1/notifications/my/read-all` | Any | Mark all as read |
| DELETE | `/api/v1/notifications/{id}` | Any | Delete a notification |
| POST | `/api/v1/notifications/bulk` | ADMIN | Bulk send to multiple users |
| GET | `/api/v1/notifications/admin/user/{id}` | ADMIN | View any user's inbox |
| POST | `/api/v1/notifications/internal/send` | Internal | Send notification from other service |
| POST | `/api/v1/notifications/internal/send-email` | Internal | Send email alert |

## Notification Types
`ENROLLMENT` · `PAYMENT` · `QUIZ_RESULT` · `CERTIFICATE` · `COURSE_PUBLISHED` · `FORUM_REPLY` · `GENERAL`

## Inter-service Usage
Other services trigger notifications via the internal endpoint (no auth required):
```bash
POST http://localhost:8089/api/v1/notifications/internal/send
{
  "userId": 10,
  "type": "ENROLLMENT",
  "title": "Enrolled!",
  "message": "You enrolled in Spring Boot Mastery.",
  "relatedEntityId": 20,
  "relatedEntityType": "COURSE"
}
```
