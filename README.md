# Project Management Platform

Jira-like project management backend built with Java 17, Spring Boot, PostgreSQL, Redis, Flyway, WebSocket/STOMP, Spring Security, and Docker Compose.

The implementation covers issue workflows, sprint management, collaboration, notifications, real-time board sync, RBAC, rate limiting, CQRS/read models, integration tests, concurrency tests, WebSocket tests, and k6 load validation.

## Quick Start

Prerequisites:

- Java 17
- Maven 3.9+
- Docker Desktop or Docker Engine
- k6 for load tests, installable with `brew install k6` on macOS
- `jq` is optional, used only in shell examples that extract JWTs from JSON

Start the full stack with Docker Compose:

```bash
docker compose up --build
```

The API will be available at:

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/api-docs`
- Liveness: `http://localhost:8080/api/health/live`
- Readiness: `http://localhost:8080/api/health/ready`
- Actuator health: `http://localhost:8080/actuator/health`
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`

Alternative local developer mode, using Docker only for PostgreSQL and Redis:

```bash
docker compose up -d postgres redis
DB_URL=jdbc:postgresql://localhost:15432/project_management \
DB_USERNAME=pm_user \
DB_PASSWORD=pm_password \
REDIS_HOST=localhost \
REDIS_PORT=16379 \
mvn spring-boot:run
```

Stop the stack:

```bash
docker compose down
```

Reset local data:

```bash
docker compose down -v
docker compose up --build
```

## Docker Compose Commands

Build and run everything:

```bash
docker compose up --build
```

Run in the background:

```bash
docker compose up -d --build
```

Run only dependencies:

```bash
docker compose up -d postgres redis
```

View logs:

```bash
docker compose logs -f api
docker compose logs -f postgres
docker compose logs -f redis
```

Check service status:

```bash
docker compose ps
```

Stop containers but keep data:

```bash
docker compose down
```

Stop containers and remove PostgreSQL data:

```bash
docker compose down -v
```

## Configuration

Main runtime environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/project_management` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `pm_user` | PostgreSQL user |
| `DB_PASSWORD` | `pm_password` | PostgreSQL password |
| `REDIS_URL` | `redis://localhost:6379` | Redis URL for hosted environments |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `PORT` | unset | Hosted platform port, used before `SERVER_PORT` |
| `SERVER_PORT` | `8080` | Local API port |
| `DB_POOL_MAX_SIZE` | `10` | Hikari max pool size |
| `DB_POOL_MIN_IDLE` | `2` | Hikari min idle connections |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | `30000` | Hikari connection timeout |
| `SHUTDOWN_PHASE_TIMEOUT` | `30s` | Graceful shutdown phase timeout |
| `SECURITY_JWT_SECRET` | `local-dev-secret-change-me` | JWT signing secret; override outside local development |
| `SECURITY_JWT_ISSUER` | `project-management-platform` | JWT issuer |
| `SECURITY_JWT_TTL_SECONDS` | `86400` | JWT lifetime |
| `SECURITY_RATE_LIMIT_REQUESTS_PER_MINUTE` | `120` | Fixed-window API rate limit |

Docker Compose exposes:

- PostgreSQL: `localhost:15432`
- Redis: `localhost:16379`
- API: `localhost:8080`

## Seed Data And Credentials

Flyway seeds a demo workspace, project, workflow, sprint, users, memberships, and sample issues.

Seed project:

| Item | Value |
| --- | --- |
| Workspace | `Demo Workspace` |
| Project key | `PROJ` |
| Project ID | `00000000-0000-0000-0000-000000000201` |
| Workflow | `To Do -> In Progress -> In Review -> Done` |
| Active sprint | `Sprint 1` |
| Issues | `PROJ-1`, `PROJ-2` |

Seed users:

| Email | User ID | Role |
| --- | --- | --- |
| `admin@example.com` | `00000000-0000-0000-0000-000000000101` | `admin` |
| `lead@example.com` | `00000000-0000-0000-0000-000000000102` | `project_lead` |
| `member@example.com` | `00000000-0000-0000-0000-000000000103` | `member` |
| `viewer@example.com` | `00000000-0000-0000-0000-000000000104` | `viewer` |

There are no passwords in the local prototype. Get a JWT by email:

```bash
curl -sS -X POST http://localhost:8080/api/v1/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"email":"lead@example.com"}'
```

Use the returned `accessToken`:

```bash
TOKEN="<accessToken>"
curl -sS http://localhost:8080/api/v1/projects/00000000-0000-0000-0000-000000000201/board \
  -H "Authorization: Bearer $TOKEN"
```

## API Overview

All product APIs are versioned under `/api/v1`.

Authentication:

- `POST /api/v1/auth/token`

Issues and board:

- `POST /api/v1/projects/{projectId}/issues`
- `GET /api/v1/projects/{projectId}/board`
- `PATCH /api/v1/issues/{issueId}`
- `POST /api/v1/issues/{issueId}/transitions`
- `GET /api/v1/search?q=...&cursor=...&limit=...`

Search supports full-text queries and simple structured filters such as:

```text
status = "In Progress" AND assignee = "Jane Smith"
```

Sprints:

- `GET /api/v1/projects/{projectId}/sprints`
- `POST /api/v1/projects/{projectId}/sprints`
- `PATCH /api/v1/sprints/{sprintId}`
- `DELETE /api/v1/sprints/{sprintId}`
- `POST /api/v1/sprints/{sprintId}/start`
- `POST /api/v1/sprints/{sprintId}/complete`
- `POST /api/v1/sprints/{sprintId}/issues`
- `DELETE /api/v1/sprints/{sprintId}/issues/{issueId}`

Collaboration:

- `GET /api/v1/issues/{issueId}/comments`
- `POST /api/v1/issues/{issueId}/comments`
- `GET /api/v1/issues/{issueId}/watchers`
- `POST /api/v1/issues/{issueId}/watchers`
- `DELETE /api/v1/issues/{issueId}/watchers/{userId}`
- `GET /api/v1/users/{userId}/notifications`
- `GET /api/v1/projects/{projectId}/activity`

Custom fields:

- `GET /api/v1/projects/{projectId}/custom-fields`
- `POST /api/v1/projects/{projectId}/custom-fields`
- `PATCH /api/v1/projects/{projectId}/custom-fields/{definitionId}`
- `DELETE /api/v1/projects/{projectId}/custom-fields/{definitionId}`
- `GET /api/v1/issues/{issueId}/custom-fields`
- `PUT /api/v1/issues/{issueId}/custom-fields/{definitionId}`

Project administration:

- `PATCH /api/v1/projects/{projectId}/members/{userId}/role`
- `DELETE /api/v1/projects/{projectId}`

Real-time sync:

- WebSocket/STOMP endpoint: `/ws`
- Board topic: `/topic/projects/{projectId}/board`
- Issue topic: `/topic/issues/{issueId}`
- Board presence topic: `/topic/projects/{projectId}/presence`
- Issue presence topic: `/topic/issues/{issueId}/presence`
- Replay destination: `/app/realtime/replay`
- REST replay helper: `GET /api/v1/realtime/replay`

Common response behavior:

- Invalid workflow transition: `422 WORKFLOW_VIOLATION`
- Stale issue version: `409 CONFLICT`
- Unauthorized request: `401 UNAUTHENTICATED`
- RBAC failure: `403 FORBIDDEN`
- Rate limit exceeded: `429 RATE_LIMITED`

## Example Requests

Create an issue as a member:

```bash
TOKEN="$(curl -sS -X POST http://localhost:8080/api/v1/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"email":"member@example.com"}' | jq -r '.accessToken')"

curl -sS -X POST http://localhost:8080/api/v1/projects/00000000-0000-0000-0000-000000000201/issues \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "type": "task",
    "title": "Review submission package",
    "description": "Validate docs and test evidence",
    "priority": "medium",
    "reporterId": "00000000-0000-0000-0000-000000000103",
    "storyPoints": 2
  }'
```

Move an issue through workflow:

```bash
curl -sS -X POST http://localhost:8080/api/v1/issues/PROJ-2/transitions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"version":0,"toStatus":"In Progress"}'
```

Add a comment with a mention:

```bash
curl -sS -X POST http://localhost:8080/api/v1/issues/PROJ-1/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "authorId": "00000000-0000-0000-0000-000000000103",
    "body": "Please review @lead@example.com"
  }'
```

## Tests

Run all automated tests:

```bash
mvn test
```

The suite includes:

- Unit tests for workflow validation, optimistic locking, sprint carry-over, RBAC, JWTs, mentions, replay, and board cache behavior
- PostgreSQL/Testcontainers integration tests for issue CRUD, board query, sprint start/complete, comments and mentions, activity feed, custom fields, structured search, idempotency, and audited role changes
- Concurrency tests for simultaneous issue updates, WIP-limited moves, and sprint completion advisory locks
- WebSocket tests for board update delivery and missed event replay after reconnect

Current verified result:

```text
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
```

## Load Test

The k6 script is at:

- `load/board-viewers.js`

Install k6 on macOS:

```bash
brew install k6
```

Run the default board load test:

```bash
k6 run load/board-viewers.js
```

Run the 100-concurrent-viewer scenario:

```bash
BASE_URL=http://localhost:8080 \
PROJECT_ID=00000000-0000-0000-0000-000000000201 \
USER_EMAIL=viewer@example.com \
VUS=100 \
DURATION=1m \
k6 run load/board-viewers.js
```

For the recorded load result, the API was run on `8081` with a test-only rate-limit override so the run measured board-read capacity rather than the security rate limiter:

```bash
SERVER_PORT=8081 \
DB_URL=jdbc:postgresql://localhost:15432/project_management \
DB_USERNAME=pm_user \
DB_PASSWORD=pm_password \
REDIS_HOST=localhost \
REDIS_PORT=16379 \
SECURITY_RATE_LIMIT_REQUESTS_PER_MINUTE=10000 \
mvn spring-boot:run
```

Recorded command:

```bash
BASE_URL=http://localhost:8081 \
PROJECT_ID=00000000-0000-0000-0000-000000000201 \
USER_EMAIL=viewer@example.com \
VUS=100 \
DURATION=1m \
k6 run --summary-export=load/results/board-viewers-100vus.json load/board-viewers.js
```

Recorded result on 2026-06-10:

- `100` max VUs for `1m`
- `6000` board iterations, `6001` HTTP requests
- `0.00%` failed requests
- `12001 / 12001` checks passed
- latency: avg `15.49 ms`, p90 `30.60 ms`, p95 `38.12 ms`, max `120.10 ms`
- thresholds passed: `http_req_failed rate<0.01`, `http_req_duration p(95)<500`

Result artifacts:

- `load/results/board-viewers-100vus.md`
- `load/results/board-viewers-100vus.json`

## Documentation

Design docs:

- [Architecture Overview](docs/architecture.md)
- [Data Model And ERD](docs/data-model.md)
- [Workflow Engine](docs/workflow-engine.md)
- [Event-Driven Flow](docs/event-driven-flow.md)
- [CQRS And Board Read Model](docs/cqrs-read-model.md)
- [Events, CQRS, And Reliability](docs/events-cqrs-reliability.md)
- [Scaling Strategy](docs/scaling-strategy.md)
- [Render Hosted Prototype Deployment](docs/deployment-render.md)

ADRs:

- [ADR 0001: Spring Boot Modular Monolith](docs/adr/0001-spring-boot-modular-monolith.md)
- [ADR 0002: PostgreSQL And Redis](docs/adr/0002-postgresql-and-redis.md)
- [ADR 0003: Domain Events And Outbox](docs/adr/0003-domain-events-and-outbox.md)
- [ADR 0004: Optimistic Locking](docs/adr/0004-optimistic-locking.md)
- [ADR 0005: WebSocket Replay Strategy](docs/adr/0005-websocket-replay-strategy.md)
- [ADR 0006: Local Docker Deployment](docs/adr/0006-local-docker-deployment.md)

## Implementation Summary

Implemented phases:

- Phase 1: foundations, health, OpenAPI, Flyway, Docker Compose
- Phase 2: persistence schema, repositories, constraints, search indexes
- Phase 3: issue CRUD surface, board query, workflow transitions, idempotency
- Phase 4: sprint lifecycle, carry-over, advisory locks, completion audit
- Phase 5: comments, mentions, watchers, notifications, activity feed
- Phase 6: WebSocket/STOMP sync, presence, replay buffers
- Phase 7: JWT auth, RBAC, rate limiting
- Phase 8: board read model, Redis caching, metrics, graceful shutdown
- Phase 9: unit, integration, concurrency, WebSocket, and k6 load validation
- Phase 10: submission documentation

## Submission Notes

The repository is self-contained for local review:

- `docker-compose.yml` starts PostgreSQL, Redis, and the API.
- Flyway applies schema migrations and seed data automatically.
- Swagger is available at `http://localhost:8080/swagger-ui.html`.
- Seed users can obtain local JWTs through `POST /api/v1/auth/token`.
- `mvn test` runs the automated test suite.
- `load/results/board-viewers-100vus.md` contains recorded load-test evidence.

Hosted submission fields:

```text
GitHub repo: https://github.com/adarshad12/Project-Management-Platform
Hosted prototype: https://project-management-platform-api.onrender.com
Swagger: https://project-management-platform-api.onrender.com/swagger-ui.html
Health: https://project-management-platform-api.onrender.com/actuator/health
```

For hosted deployment steps and operational assumptions, see [Render Hosted Prototype Deployment](docs/deployment-render.md).
