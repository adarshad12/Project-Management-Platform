# ADR 0006: Local Docker Deployment

## Status

Accepted

## Context

The package needs to be pushed to GitHub and reviewed locally. Reviewers should be able to start the API and dependencies without manually installing PostgreSQL or Redis.

The runtime needs:

- PostgreSQL
- Redis
- Spring Boot API
- Flyway migration execution
- seeded demo users/project/issues
- stable Swagger URL

## Decision Drivers

- reproducible local review
- one-command startup
- no hosted infrastructure dependency
- easy teardown and reset
- same service shape as tests/load validation

## Considered Options

### Docker Compose

Provide PostgreSQL, Redis, and API services in `docker-compose.yml`.

Pros:

- one command starts the stack
- stable ports and environment variables
- easy volume reset
- reviewers do not need local databases

Cons:

- Docker Desktop resource limits can affect local performance
- first image build downloads Maven dependencies

### Manual Local Services

Ask reviewers to install PostgreSQL and Redis locally.

Pros:

- less Docker build overhead

Cons:

- more setup variance
- more environment-specific failure modes
- slower review path

### Hosted Only

Deploy a hosted prototype and document the URL.

Pros:

- fastest reviewer click path

Cons:

- outside current local-first scope
- requires secrets and infrastructure management
- less transparent for code review

## Decision

Use Docker Compose as the primary local deployment path.

Services:

- `pm-postgres`
- `pm-redis`
- `pm-api`

Primary command:

```bash
docker compose up --build
```

## Consequences

Positive:

- deterministic local startup
- Flyway applies migrations and seed data automatically
- Swagger is available at `http://localhost:8080/swagger-ui.html`
- database reset is one command: `docker compose down -v`

Negative:

- first build can be slower
- local performance depends on Docker resources
- GitHub reviewers need Docker available

## Implementation References

- `docker-compose.yml`
- `Dockerfile`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration`
- `README.md`

## Revisit When

- hosted submission is required
- production deployment target is selected
- separate managed PostgreSQL/Redis services replace local containers

