# EduLearn — Payment Service (port 8085)
Handles course purchases, subscription plans, and refunds.

## Quick Start
```sql
CREATE DATABASE edulearn_payment;
```
```bash
mvn spring-boot:run
```
Swagger: http://localhost:8085/swagger-ui.html

## Key Endpoints
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | /api/v1/payments | STUDENT | Purchase a course |
| GET | /api/v1/payments/my | Any | Payment history |
| POST | /api/v1/payments/{id}/refund | ADMIN | Refund a payment |
| POST | /api/v1/payments/subscriptions | STUDENT | Subscribe (MONTHLY/ANNUAL) |
| GET | /api/v1/payments/subscriptions/my | Any | My active subscription |
| DELETE | /api/v1/payments/subscriptions/my | Any | Cancel subscription |
| POST | /api/v1/payments/subscriptions/my/renew | Any | Renew subscription |
| GET | /api/v1/payments/subscriptions/active | Internal | isSubscriptionActive |
| GET | /api/v1/payments/admin/revenue | ADMIN | Platform revenue analytics |

## Inter-service calls
- Triggers **enrollment-service** (`/api/v1/enrollments/internal/enroll`) after successful payment.

## Subscription Pricing
| Plan | Price |
|------|-------|
| MONTHLY | ₹299/month |
| ANNUAL | ₹2999/year |
