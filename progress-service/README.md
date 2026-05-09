# EduLearn — Progress & Certificate Service (port 8086)
Tracks lesson-level watch time and completion, computes course progress %, and issues verifiable certificates.

## Quick Start
```sql
CREATE DATABASE edulearn_progress;
```
```bash
mvn spring-boot:run
```
Swagger: http://localhost:8086/swagger-ui.html

## Key Endpoints
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | /api/v1/progress | STUDENT | Track watch time + completion |
| PUT | /api/v1/progress/lesson/{id}/complete | STUDENT | Mark lesson complete |
| GET | /api/v1/progress/lesson/{id} | Any | Lesson progress |
| GET | /api/v1/progress/course/{id} | Any | Course progress % |
| GET | /api/v1/progress/my | Any | All my progress |
| POST | /api/v1/certificates | STUDENT | Issue certificate |
| GET | /api/v1/certificates/my | Any | My certificates |
| GET | /api/v1/certificates/course/{id} | Any | Certificate for course |
| GET | /api/v1/certificates/verify/{code} | **Public** | Verify certificate (no auth) |
| GET | /api/v1/certificates/student/{id} | ADMIN | Admin view student certs |

## Certificate Verification
The verify endpoint is publicly accessible — no JWT required. Third parties can verify certificates at:
```
GET http://localhost:8086/api/v1/certificates/verify/{verificationCode}
```
