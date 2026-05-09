# EduLearn — Discussion Service (port 8088)

Course forum threads, replies, upvoting, pinning, and moderation.

## Quick Start
```sql
CREATE DATABASE edulearn_discussion;
```
```bash
mvn spring-boot:run
```
Swagger: http://localhost:8088/swagger-ui.html

## Key Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/v1/threads` | Any logged-in | Create a thread |
| GET | `/api/v1/threads/course/{id}` | Public | List threads (pinned first) |
| GET | `/api/v1/threads/lesson/{id}` | Public | Threads for a lesson |
| GET | `/api/v1/threads/{id}` | Public | Thread with all replies |
| GET | `/api/v1/threads/course/{id}/search?keyword=` | Public | Keyword search |
| DELETE | `/api/v1/threads/{id}` | Author/Admin | Delete thread |
| PUT | `/api/v1/threads/{id}/pin` | INSTRUCTOR/ADMIN | Pin/unpin thread |
| PUT | `/api/v1/threads/{id}/close` | INSTRUCTOR/ADMIN | Close/reopen thread |
| POST | `/api/v1/threads/{id}/replies` | Any logged-in | Post a reply |
| GET | `/api/v1/threads/{id}/replies` | Public | Replies (sorted by upvotes) |
| POST | `/api/v1/replies/{id}/upvote` | Any logged-in | Upvote a reply |
| PUT | `/api/v1/replies/{id}/accept` | Thread Author/Admin | Mark best answer |
| DELETE | `/api/v1/replies/{id}` | Author/Admin | Delete reply |

## Business Rules
- Closed threads cannot receive new replies
- Pinned threads always appear first in course listings
- Replies are sorted by upvotes descending, then by creation time
- Only the thread author or admin can delete a thread
- Only the thread author or admin can accept a reply as the best answer
