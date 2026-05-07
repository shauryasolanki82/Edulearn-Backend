# EduLearn — Assessment Service (port 8087)

Quiz management, question CRUD, timed attempts, and auto-grading engine.

## Quick Start
```sql
CREATE DATABASE edulearn_assessment;
```
```bash
mvn spring-boot:run
```
Swagger: http://localhost:8087/swagger-ui.html

## Key Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/v1/quizzes` | INSTRUCTOR | Create a quiz |
| GET | `/api/v1/quizzes/course/{id}` | Public | List quizzes for a course |
| GET | `/api/v1/quizzes/{id}` | Any | Quiz details |
| PUT | `/api/v1/quizzes/{id}` | INSTRUCTOR | Update quiz |
| PUT | `/api/v1/quizzes/{id}/publish` | INSTRUCTOR | Publish/unpublish |
| DELETE | `/api/v1/quizzes/{id}` | INSTRUCTOR | Delete quiz |
| POST | `/api/v1/quizzes/{id}/questions` | INSTRUCTOR | Add question |
| GET | `/api/v1/quizzes/{id}/questions` | Any | Questions (no correct answers for students) |
| PUT | `/api/v1/questions/{id}` | INSTRUCTOR | Update question |
| DELETE | `/api/v1/questions/{id}` | INSTRUCTOR | Delete question |
| POST | `/api/v1/quizzes/{id}/start` | STUDENT | Start attempt |
| POST | `/api/v1/attempts/submit` | STUDENT | Submit + auto-grade |
| GET | `/api/v1/attempts/my` | STUDENT | My attempt history |
| GET | `/api/v1/quizzes/{id}/best-score` | STUDENT | Best score for a quiz |
| GET | `/api/v1/quizzes/{id}/attempts` | INSTRUCTOR | All attempts for a quiz |

## Auto-grading Logic
Score = (total correct marks / total possible marks) × 100. Attempt is marked `passed=true` if score ≥ `passingScore` threshold.

## Question Types
- `MCQ` — single correct answer (answer = option index as string e.g. "0")
- `TRUE_FALSE` — answer = "true" or "false"
- `MULTI_SELECT` — answer = comma-separated indices e.g. "0,2"
