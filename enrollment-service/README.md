# EduLearn — Enrollment Service (port 8084)
Manages course enrollments, progress sync, and certificate issuance triggers.

## Quick Start
```sql
CREATE DATABASE edulearn_enrollment;
```
```bash
mvn spring-boot:run
```
Swagger: http://localhost:8084/swagger-ui.html

## Key Endpoints
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | /api/v1/enrollments | STUDENT | Enroll in a course |
| DELETE | /api/v1/enrollments/{courseId} | STUDENT | Unenroll |
| GET | /api/v1/enrollments/my | Any | My enrollments |
| GET | /api/v1/enrollments/check | Public | isEnrolled check |
| PUT | /api/v1/enrollments/{courseId}/progress | Any | Update progress % |
| PUT | /api/v1/enrollments/{courseId}/complete | Any | Mark complete |
| POST | /api/v1/enrollments/{courseId}/certificate | Any | Issue certificate |
| GET | /api/v1/enrollments/course/{id}/stats | INSTRUCTOR/ADMIN | Course stats |
| POST | /api/v1/enrollments/internal/enroll | Internal | Payment-service trigger |

## Inter-service calls
- Notifies **course-service** (`/api/v1/courses/internal/enrollment-count`) on enroll/unenroll.
