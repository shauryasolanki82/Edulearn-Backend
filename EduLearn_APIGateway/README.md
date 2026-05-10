# EduLearn LMS — Full Stack Setup Guide

## Architecture

```
Browser (port 3000)
     │
     ▼
React Frontend (Vite dev / Nginx prod)
     │  all /api/v1/* requests
     ▼
API Gateway (port 8080)  ◄─── Single entry point
     │
     ├──► Auth Service        (8081) → DB: edulearn_auth
     ├──► Course Service      (8082) → DB: edulearn_course
     ├──► Lesson Service      (8083) → DB: edulearn_lesson
     ├──► Enrollment Service  (8084) → DB: edulearn_enrollment
     ├──► Payment Service     (8085) → DB: edulearn_payment
     ├──► Progress Service    (8086) → DB: edulearn_progress
     ├──► Assessment Service  (8087) → DB: edulearn_assessment
     ├──► Discussion Service  (8088) → DB: edulearn_discussion
     └──► Notification Service(8089) → DB: edulearn_notification
```

### What the API Gateway does
- **Single entry point** — all frontend requests hit `localhost:8080`
- **JWT authentication** — validates Bearer tokens before forwarding
- **CORS** — handles pre-flight OPTIONS requests centrally
- **Request logging** — logs method, path, status and duration
- **Header forwarding** — injects `X-User-Id`, `X-User-Email`, `X-User-Role` headers
- **Public endpoint bypass** — login, register, featured courses are freely accessible

---

## Route Mapping

| URL Prefix            | Service             | Port |
|-----------------------|---------------------|------|
| `/api/v1/auth/**`         | auth-service        | 8081 |
| `/api/v1/courses/**`      | course-service      | 8082 |
| `/api/v1/lessons/**`      | lesson-service      | 8083 |
| `/api/v1/enrollments/**`  | enrollment-service  | 8084 |
| `/api/v1/payments/**`     | payment-service     | 8085 |
| `/api/v1/progress/**`     | progress-service    | 8086 |
| `/api/v1/certificates/**` | progress-service    | 8086 |
| `/api/v1/quizzes/**`      | assessment-service  | 8087 |
| `/api/v1/questions/**`    | assessment-service  | 8087 |
| `/api/v1/attempts/**`     | assessment-service  | 8087 |
| `/api/v1/threads/**`      | discussion-service  | 8088 |
| `/api/v1/replies/**`      | discussion-service  | 8088 |
| `/api/v1/notifications/**`| notification-service| 8089 |

---

## Folder Structure

```
project/
├── api-gateway/                  ← NEW: Spring Cloud Gateway
│   ├── src/main/java/com/edulearn/gateway/
│   │   ├── ApiGatewayApplication.java
│   │   ├── config/
│   │   │   ├── GatewayConfig.java   (CORS bean)
│   │   │   └── JwtUtil.java         (token validator)
│   │   └── filter/
│   │       ├── AuthFilter.java      (JWT guard on routes)
│   │       └── LoggingFilter.java   (request/response logger)
│   ├── src/main/resources/
│   │   ├── application.yml          (local dev — localhost URIs)
│   │   └── application-docker.yml   (Docker — service-name URIs)
│   ├── Dockerfile
│   └── pom.xml
│
├── LMS_Backend/                  ← Your existing backend (unchanged)
│   ├── auth-service/
│   ├── course-service/
│   ├── lesson-service/
│   ├── enrollment-service/
│   ├── payment-service/
│   ├── progress-service/
│   ├── assessment-service/
│   ├── discussion-service/
│   └── notification-service/
│
├── edulearn-react/               ← Updated frontend
│   ├── src/api/index.js          ← UPDATED: all calls go to gateway:8080
│   ├── vite.config.js            ← UPDATED: single proxy to gateway
│   ├── Dockerfile                ← NEW: Nginx production build
│   └── nginx.conf                ← NEW: SPA + /api proxy config
│
├── docker-compose.yml            ← NEW: entire stack in one command
├── init-db.sql                   ← NEW: creates all 9 MySQL databases
├── start-local.sh                ← NEW: local dev startup script
└── stop-local.sh                 ← NEW: local dev stop script
```

---

## Option 1 — Local Development (without Docker)

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8 running on port 3306
- Node.js 18+

### 1. Set up MySQL
```sql
-- Run once in MySQL
source init-db.sql;
-- OR manually:
CREATE DATABASE edulearn_auth;
CREATE DATABASE edulearn_course;
-- (etc. — see init-db.sql for full list)
```

### 2. Start all backend services + gateway
```bash
chmod +x start-local.sh stop-local.sh
./start-local.sh
```
Logs are written to `./logs/<service-name>.log`

### 3. Start the React frontend
```bash
cd edulearn-react
npm install
npm run dev
```

### 4. Open your browser
```
http://localhost:3000
```
All API calls route through the gateway at `http://localhost:8080`.

### Stop everything
```bash
./stop-local.sh
```

---

## Option 2 — Docker Compose (Full Stack)

### Prerequisites
- Docker Desktop or Docker Engine + Compose plugin

### Start
```bash
docker compose up --build
```
First build takes ~5-10 minutes (Maven downloads dependencies for each service).

### Stop
```bash
docker compose down
# To also remove MySQL data:
docker compose down -v
```

### Open your browser
```
http://localhost:3000
```

---

## Public Endpoints (no token required)
These endpoints bypass the JWT check:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET  /api/v1/auth/validate`
- `GET  /api/v1/auth/oauth/success`
- `GET  /api/v1/courses` (browse)
- `GET  /api/v1/courses/search`
- `GET  /api/v1/courses/featured`
- `GET  /api/v1/courses/top`
- `GET  /api/v1/courses/free`
- `GET  /api/v1/certificates/verify/{code}`
- `GET  /actuator/health`

---

## Gateway Health Check
```
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/gateway/routes
```

---

## Environment Variables (quick reference)

| Variable | Value |
|---|---|
| `APP_JWT_SECRET` | `ZWR1bGVhcm5TZWNyZXRLZXlGb3JKV1QyMDI0UHJvZHVjdGlvblVzZU9ubHk=` |
| `MYSQL_ROOT_PASSWORD` | `manish` |
| `SPRING_PROFILES_ACTIVE` | `docker` (in Docker only) |

---

## Troubleshooting

**Gateway returns 401 on public endpoints**
→ Check that the path exactly matches the `public-endpoints` list in `application.yml`.

**CORS errors in browser**
→ Make sure frontend runs on `http://localhost:3000` (the only allowed origin).
→ Do not add trailing slashes to the allowed origin.

**Service not found / 503 from gateway**
→ Check the target service is running: `curl http://localhost:808x/actuator/health`
→ In Docker, check logs: `docker compose logs <service-name>`

**MySQL connection refused**
→ Ensure MySQL is running and the password is `manish` (matches `application.properties`).

**Docker build fails for a service**
→ Each service builds itself inside Docker (Maven runs inside the container).
→ Ensure you have a working internet connection for the first build.
