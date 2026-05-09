# EduLearn — Auth Service

> Microservice #1 of the EduLearn LMS platform.  
> Handles user registration, login, JWT issuance, OAuth2, and profile management.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JWT (JJWT 0.11.5) |
| OAuth2 | Spring Security OAuth2 Client (GitHub & Google) |
| Database | MySQL 8 (production), H2 (tests) |
| ORM | Spring Data JPA + Hibernate |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Build | Maven |
| Java | 17 |

---

## Project Structure

```
auth-service/
├── src/main/java/com/edulearn/auth/
│   ├── AuthServiceApplication.java       ← Entry point
│   ├── config/
│   │   ├── SecurityConfig.java           ← Spring Security + JWT filter chain
│   │   └── SwaggerConfig.java            ← OpenAPI 3.0 configuration
│   ├── controller/
│   │   └── AuthResource.java             ← REST endpoints (/api/v1/auth/*)
│   ├── dto/
│   │   └── AuthDto.java                  ← All request/response DTOs
│   ├── entity/
│   │   └── User.java                     ← JPA entity (users table)
│   ├── exception/
│   │   ├── UserNotFoundException.java
│   │   ├── DuplicateEmailException.java
│   │   ├── BadCredentialsException.java
│   │   └── GlobalExceptionHandler.java   ← @RestControllerAdvice
│   ├── repository/
│   │   └── UserRepository.java           ← Spring Data JPA repository
│   ├── security/
│   │   ├── JwtUtil.java                  ← JWT generate / parse / validate
│   │   ├── JwtAuthenticationFilter.java  ← OncePerRequestFilter
│   │   └── CustomOAuth2UserService.java  ← OAuth2 user load/save
│   └── service/
│       ├── AuthService.java              ← Business contract interface
│       └── impl/
│           └── AuthServiceImpl.java      ← Full implementation
└── src/test/java/com/edulearn/auth/
    ├── controller/
    │   └── AuthResourceIntegrationTest.java
    ├── security/
    │   └── JwtUtilTest.java
    └── service/
        └── AuthServiceImplTest.java
```

---

## Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.0+ running locally

---

## Quick Start

### 1. Create the MySQL database

```sql
CREATE DATABASE edulearn_auth;
```

### 2. Configure credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=YOUR_MYSQL_USER
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

> The schema is auto-created by Hibernate (`ddl-auto=update`) on first startup.

### 3. Build and run

```bash
cd auth-service
mvn clean install -DskipTests
mvn spring-boot:run
```

The service starts on **http://localhost:8081**

---

## API Reference

### Swagger UI (interactive docs)

```
http://localhost:8081/swagger-ui.html
```

### Endpoints

| Method | URL | Auth Required | Description |
|--------|-----|:---:|-------------|
| POST | `/api/v1/auth/register` | No | Register new user |
| POST | `/api/v1/auth/login` | No | Login, receive JWT |
| GET | `/api/v1/auth/validate` | No | Validate a JWT token |
| POST | `/api/v1/auth/refresh` | No | Refresh a JWT token |
| GET | `/api/v1/auth/profile` | Yes | Get own profile |
| PUT | `/api/v1/auth/profile` | Yes | Update own profile |
| PUT | `/api/v1/auth/password` | Yes | Change password |
| DELETE | `/api/v1/auth/delete` | Yes | Deactivate own account |
| GET | `/api/v1/auth/user/{id}` | Yes | Get user by ID |
| GET | `/api/v1/auth/user/email/{email}` | Yes | Get user by email |
| GET | `/api/v1/auth/users/role/{role}` | Admin | List users by role |
| GET | `/api/v1/auth/users/search?name=` | Admin | Search users by name |
| DELETE | `/api/v1/admin/users/{id}` | Admin | Admin deactivate user |

---

## Sample Requests

### Register

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Alice Smith",
    "email": "alice@example.com",
    "password": "secret123",
    "role": "STUDENT"
  }'
```

### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "secret123"
  }'
```

### Use the token

```bash
# Copy the accessToken from login response, then:
curl http://localhost:8081/api/v1/auth/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## OAuth2 Setup (Optional)

### GitHub

1. Go to GitHub → Settings → Developer Settings → OAuth Apps → New OAuth App
2. Set **Homepage URL**: `http://localhost:8081`
3. Set **Callback URL**: `http://localhost:8081/login/oauth2/code/github`
4. Copy Client ID and Secret into `application.properties`

### Google

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials
2. Create OAuth 2.0 Client ID (Web application)
3. Add redirect URI: `http://localhost:8081/login/oauth2/code/google`
4. Copy Client ID and Secret into `application.properties`

After OAuth2 login, the `CustomOAuth2UserService` automatically creates/updates the local user record.

---

## Running Tests

```bash
# Unit + Integration tests (uses H2 in-memory DB)
mvn test

# Run only unit tests
mvn test -Dtest=AuthServiceImplTest,JwtUtilTest

# Run only integration tests
mvn test -Dtest=AuthResourceIntegrationTest
```

---

## JWT Configuration

| Property | Default | Description |
|---|---|---|
| `app.jwt.secret` | Base64 key | HMAC-SHA256 signing secret |
| `app.jwt.expiration-ms` | `86400000` | Token lifetime (24 hours) |

> **Production**: Replace the default secret with a cryptographically random 256-bit key.  
> Generate one with: `openssl rand -base64 32`

---

## Health Check

```bash
curl http://localhost:8081/actuator/health
```

---

## Inter-Service Communication

Other microservices (Course-Service, Enrollment-Service, etc.) validate user tokens by calling:

```
GET /api/v1/auth/validate
Authorization: Bearer <token>
```

Response:
```json
{
  "valid": true,
  "userId": 1,
  "email": "alice@example.com",
  "role": "STUDENT"
}
```

---

## Roles

| Role | Description |
|---|---|
| `STUDENT` | Default role; can enroll, learn, take quizzes |
| `INSTRUCTOR` | Can create courses, manage lessons and quizzes |
| `ADMIN` | Full platform management access |
