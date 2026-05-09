# EduLearn — Lesson Service

> Microservice #3 of the EduLearn LMS platform.
> Manages lesson content, ordering, preview flags, and resource attachments per course.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JWT |
| Database | MySQL 8 (production), H2 (tests) |
| ORM | Spring Data JPA + Hibernate |
| Inter-service | RestTemplate → course-service (duration sync) |
| Build | Maven / Java 17 |

---

## Project Structure

```
lesson-service/
├── src/main/java/com/edulearn/lesson/
│   ├── LessonServiceApplication.java
│   ├── config/
│   │   ├── AppConfig.java              ← RestTemplate bean
│   │   ├── SecurityConfig.java
│   │   └── SwaggerConfig.java
│   ├── controller/
│   │   └── LessonResource.java         ← REST endpoints (/api/v1/lessons/*)
│   ├── dto/
│   │   └── LessonDto.java
│   ├── entity/
│   │   ├── Lesson.java                 ← courseId, instructorId, orderIndex, isPreview
│   │   └── Resource.java               ← PDF, slides, code attachments
│   ├── exception/
│   │   ├── ForbiddenException.java
│   │   ├── LessonNotFoundException.java
│   │   ├── ResourceNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   ├── repository/
│   │   ├── LessonRepository.java
│   │   └── ResourceRepository.java
│   ├── security/
│   │   ├── JwtUtil.java
│   │   └── JwtAuthenticationFilter.java
│   └── service/
│       ├── LessonService.java
│       └── impl/
│           └── LessonServiceImpl.java
└── src/test/...
```

---

## Quick Start

### 1. Create MySQL database
```sql
CREATE DATABASE edulearn_lesson;
```

### 2. Configure
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.username=YOUR_USER
spring.datasource.password=YOUR_PASS
app.course-service.url=http://localhost:8082
```

### 3. Run
```bash
mvn spring-boot:run
```
Service starts on **http://localhost:8083**

Swagger UI: `http://localhost:8083/swagger-ui.html`

---

## API Reference

### Public Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/v1/lessons/course/{id}/preview` | Get preview lessons (no auth) |
| GET | `/api/v1/lessons/{id}/preview` | Get a single preview lesson (no auth) |

### Authenticated Endpoints

| Method | URL | Role | Description |
|--------|-----|------|-------------|
| GET | `/api/v1/lessons/course/{id}` | Any | List all lessons for a course |
| GET | `/api/v1/lessons/{id}` | Any | Full lesson details + resources |
| POST | `/api/v1/lessons/course/{id}` | INSTRUCTOR/ADMIN | Add a lesson |
| PUT | `/api/v1/lessons/{id}` | INSTRUCTOR/ADMIN | Update a lesson |
| DELETE | `/api/v1/lessons/{id}` | INSTRUCTOR/ADMIN | Delete a lesson |
| PUT | `/api/v1/lessons/course/{id}/reorder` | INSTRUCTOR/ADMIN | Reorder lessons |
| POST | `/api/v1/lessons/{id}/resources` | INSTRUCTOR/ADMIN | Attach a resource |
| DELETE | `/api/v1/lessons/{id}/resources/{rid}` | INSTRUCTOR/ADMIN | Remove a resource |
| GET | `/api/v1/lessons/{id}/resources` | Any | List lesson resources |

### Internal Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/v1/lessons/internal/course/{id}/count` | Lesson count for a course |

---

## Inter-Service Calls

When a lesson is added, updated, or deleted, lesson-service automatically calls:
```
PUT http://course-service:8082/api/v1/courses/internal/{courseId}/duration?totalMinutes=N
```
This keeps `Course.totalDuration` in sync. The call is fire-and-forget — if course-service is unavailable, the update is logged and skipped gracefully.

---

## Running Tests
```bash
mvn test
```
