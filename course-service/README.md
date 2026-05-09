# EduLearn — Course Service

> Microservice #2 of the EduLearn LMS platform.  
> Manages the complete course catalog: creation, search, filtering, publishing, and featured listings.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JWT (validates tokens from auth-service) |
| Database | MySQL 8 (production), H2 (tests) |
| ORM | Spring Data JPA + Hibernate |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Build | Maven |
| Java | 17 |

---

## Project Structure

```
course-service/
├── src/main/java/com/edulearn/course/
│   ├── CourseServiceApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java           ← JWT filter chain, public GET routes
│   │   └── SwaggerConfig.java
│   ├── controller/
│   │   └── CourseResource.java           ← REST endpoints (/api/v1/courses/*)
│   ├── dto/
│   │   └── CourseDto.java                ← All request/response/paged DTOs
│   ├── entity/
│   │   └── Course.java                   ← JPA entity with Category/Level/Language enums
│   ├── exception/
│   │   ├── CourseNotFoundException.java
│   │   ├── ForbiddenException.java
│   │   └── GlobalExceptionHandler.java
│   ├── repository/
│   │   └── CourseRepository.java         ← 15 query methods + JPQL search
│   ├── security/
│   │   ├── JwtUtil.java                  ← Validates JWTs from auth-service
│   │   └── JwtAuthenticationFilter.java
│   └── service/
│       ├── CourseService.java            ← Business contract
│       └── impl/
│           └── CourseServiceImpl.java    ← Full implementation
└── src/test/
    ├── resources/application-test.properties
    └── java/com/edulearn/course/
        ├── service/CourseServiceImplTest.java
        └── controller/CourseResourceIntegrationTest.java
```

---

## Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.0+
- **auth-service running on port 8081** (for JWT validation)

---

## Quick Start

### 1. Create the MySQL database

```sql
CREATE DATABASE edulearn_course;
```

### 2. Configure credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=YOUR_MYSQL_USER
spring.datasource.password=YOUR_MYSQL_PASSWORD
app.jwt.secret=       # Must match auth-service exactly
app.auth-service.url=http://localhost:8081
```

### 3. Build and run

```bash
cd course-service
mvn clean install -DskipTests
mvn spring-boot:run
```

Service starts on **http://localhost:8082**

---

## API Reference

### Swagger UI

```
http://localhost:8082/swagger-ui.html
```

### Endpoints

| Method | URL | Auth | Description |
|--------|-----|:----:|-------------|
| GET | `/api/v1/courses` | No | List all published courses (paginated) |
| GET | `/api/v1/courses/search` | No | Search with keyword/category/level/language/maxPrice |
| GET | `/api/v1/courses/featured` | No | Get featured courses |
| GET | `/api/v1/courses/top?limit=10` | No | Top N courses by enrollment |
| GET | `/api/v1/courses/free` | No | Free (price=0) courses |
| GET | `/api/v1/courses/category/{cat}` | No | Courses by category |
| GET | `/api/v1/courses/{id}` | No | Course details |
| GET | `/api/v1/courses/instructor/{id}` | Yes | Instructor's courses |
| POST | `/api/v1/courses` | INSTRUCTOR/ADMIN | Create course |
| PUT | `/api/v1/courses/{id}` | INSTRUCTOR/ADMIN | Update course |
| PUT | `/api/v1/courses/{id}/publish` | INSTRUCTOR/ADMIN | Publish/unpublish |
| DELETE | `/api/v1/courses/{id}` | INSTRUCTOR/ADMIN | Delete course |
| PUT | `/api/v1/courses/{id}/featured` | ADMIN | Toggle featured |
| POST | `/api/v1/courses/internal/enrollment-count` | Internal | Update enrollment count |
| PUT | `/api/v1/courses/internal/{id}/duration` | Internal | Update total duration |

---

## Sample Requests

### Create a course (Instructor)

```bash
curl -X POST http://localhost:8082/api/v1/courses \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "X-User-Name: Jane Doe" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Complete Java Masterclass",
    "description": "Learn Java from zero to hero",
    "category": "PROGRAMMING",
    "level": "BEGINNER",
    "price": 1299.00,
    "language": "ENGLISH",
    "whatYouWillLearn": "OOP, Collections, Streams, Spring Boot",
    "requirements": "Basic computer knowledge"
  }'
```

### Search courses

```bash
curl "http://localhost:8082/api/v1/courses/search?keyword=java&category=PROGRAMMING&level=BEGINNER&maxPrice=2000&sortBy=popular&page=0&size=10"
```

### Publish a course

```bash
curl -X PUT http://localhost:8082/api/v1/courses/1/publish \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"published": true}'
```

---

## Course Enums

### Category
`PROGRAMMING`, `WEB_DEVELOPMENT`, `DATA_SCIENCE`, `MACHINE_LEARNING`, `MOBILE_DEVELOPMENT`,
`DATABASE`, `DEVOPS`, `CLOUD`, `CYBERSECURITY`, `DESIGN`, `BUSINESS`, `MARKETING`,
`PHOTOGRAPHY`, `MUSIC`, `HEALTH`, `OTHER`

### Level
`BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `ALL_LEVELS`

### Language
`ENGLISH`, `HINDI`, `SPANISH`, `FRENCH`, `GERMAN`, `PORTUGUESE`, `ARABIC`,
`CHINESE`, `JAPANESE`, `KOREAN`, `OTHER`

### Sort Options
`newest` (default), `popular`, `rating`, `price_asc`, `price_desc`

---

## Inter-Service Communication

### From lesson-service (update duration)
```
PUT /api/v1/courses/internal/{courseId}/duration?totalMinutes=300
```

### From enrollment-service (update enrollment count)
```
POST /api/v1/courses/internal/enrollment-count
Body: { "courseId": 1, "delta": 1 }
```

---

## Running Tests

```bash
mvn test                                          # All tests (H2)
mvn test -Dtest=CourseServiceImplTest             # Unit tests only
mvn test -Dtest=CourseResourceIntegrationTest     # Integration tests only
```

---

## Health Check

```bash
curl http://localhost:8082/actuator/health
```
