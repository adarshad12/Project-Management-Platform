# Render Hosted Prototype Deployment

This guide deploys the API as a Render Docker web service with managed PostgreSQL and Redis-compatible Key Value.

## Target Shape

```text
Reviewer browser / curl
  -> Render Web Service: Spring Boot API
       -> Render PostgreSQL: source of truth and Flyway migrations
       -> Render Key Value: Redis-compatible cache, rate limits, replay buffers, presence
```

Render provides the public HTTPS URL, TLS termination, WebSocket support, service logs, health checks, and managed datastore credentials.

## Prerequisites

- GitHub repository with this project pushed.
- Render account connected to GitHub.
- Render project/environment for the prototype.
- The repository root must contain `Dockerfile`, `pom.xml`, and `src/`.

## Create Datastores

Create a Render PostgreSQL database:

- Name: `project-management-postgres`
- Database: `project_management`
- Region: same region as the API service
- Copy the internal connection details for the API service.

Create a Render Key Value instance:

- Name: `project-management-redis`
- Region: same region as the API service
- Maxmemory policy: `allkeys-lru` for the prototype cache/replay workload
- Copy the internal Redis URL for the API service.

## Create API Web Service

Create a Render Web Service from the GitHub repository:

- Runtime: Docker
- Dockerfile path: `Dockerfile`
- Branch: the submission branch
- Health check path: `/actuator/health`
- Auto-deploy: enabled for the submission branch, or disabled if you want manual deploy control

The app binds to Render's `PORT` environment variable. Locally it still falls back to `SERVER_PORT` and then `8080`.

## Environment Variables

Set these variables on the API web service. Do not commit real values to GitHub.

| Variable | Required | Notes |
| --- | --- | --- |
| `DB_URL` | yes | PostgreSQL JDBC URL using the Render internal database host |
| `DB_USERNAME` | yes | Render PostgreSQL user |
| `DB_PASSWORD` | yes | Render PostgreSQL password |
| `REDIS_URL` | yes | Render Key Value internal Redis URL |
| `SECURITY_JWT_SECRET` | yes | Strong random HMAC secret; replace the local default |
| `SECURITY_JWT_ISSUER` | no | Defaults to `project-management-platform` |
| `SECURITY_JWT_TTL_SECONDS` | no | Defaults to `86400` |
| `SECURITY_RATE_LIMIT_REQUESTS_PER_MINUTE` | no | Defaults to `120`; raise temporarily only for load validation |
| `DB_POOL_MAX_SIZE` | no | Start with `5` or `10` depending on the Render/Postgres tier |
| `DB_POOL_MIN_IDLE` | no | Start with `1` or `2` |
| `SHUTDOWN_PHASE_TIMEOUT` | no | Defaults to `30s` |

Example JDBC URL shape:

```text
jdbc:postgresql://<render-internal-postgres-host>:5432/project_management
```

Example Redis URL shape:

```text
redis://<render-internal-redis-host>:6379
```

If Render provides a credentialed Redis URL, use it as provided.

## First Deploy

1. Trigger the first API deploy.
2. Watch Render logs until the Spring Boot app starts.
3. Confirm Flyway applies all migrations.
4. Confirm the health check passes.

Expected public URLs:

```text
API:      https://<render-service-name>.onrender.com
Swagger:  https://<render-service-name>.onrender.com/swagger-ui.html
Health:   https://<render-service-name>.onrender.com/actuator/health
```

## Hosted Smoke Test

Set the hosted base URL:

```bash
BASE_URL=https://<render-service-name>.onrender.com
```

Check health:

```bash
curl "$BASE_URL/actuator/health"
```

Issue a reviewer token:

```bash
TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"email":"viewer@example.com"}' | jq -r '.accessToken')
```

Query the seeded board:

```bash
curl "$BASE_URL/api/v1/projects/00000000-0000-0000-0000-000000000201/board" \
  -H "Authorization: Bearer $TOKEN"
```

Use `member@example.com` or `lead@example.com` for mutation smoke checks.

## Hosted Load Validation

The recorded 100-viewer result in `load/results/board-viewers-100vus.md` was captured locally against Docker infrastructure. For hosted validation, run a short sanity pass first:

```bash
BASE_URL=https://<render-service-name>.onrender.com \
PROJECT_ID=00000000-0000-0000-0000-000000000201 \
USER_EMAIL=viewer@example.com \
VUS=10 \
DURATION=30s \
k6 run load/board-viewers.js
```

Only run the full 100-VU hosted test if the selected Render service and datastore tiers allow that traffic. If running the full test with one seeded viewer token, temporarily raise `SECURITY_RATE_LIMIT_REQUESTS_PER_MINUTE` so the test measures board-read capacity instead of the rate limiter.

## Operational Assumptions

- This is a hosted prototype, not a production SSO deployment.
- Auth uses seeded demo users and JWT issuance through `POST /api/v1/auth/token`.
- Swagger and token issuance remain public for reviewer convenience.
- PostgreSQL is the source of truth; Flyway runs automatically at API startup.
- Redis is used for board cache, rate limiting, WebSocket replay buffers, and presence state.
- A single API instance is enough for review. Multiple instances need shared Redis, already required here, and should keep the same JWT secret.
- Notifications are queued/simulated inside the application boundary.
- Free or low-tier hosting can introduce cold starts, constrained CPU, limited database connections, and lower load-test ceilings.
- Secrets are stored in Render environment variables or environment groups, never in the repository.

## Submission Fields

Fill these in after deploy:

```text
GitHub repo: https://github.com/adarshad12/Project-Management-Platform
Hosted prototype: https://project-management-platform-api.onrender.com
Swagger: https://project-management-platform-api.onrender.com/swagger-ui.html
Health: https://project-management-platform-api.onrender.com/actuator/health
```
