# Project Management Platform Backend Implementation Plan

## Summary

Build a Java/Spring Boot backend for a Jira-like project management platform, targeting the full SDE-2 requirement set. Start with a production-shaped local Docker Compose prototype,
then extend to hosted deployment after the local system is stable.

Default stack:

- Java 17, Spring Boot 3
- PostgreSQL for relational storage
- Redis for caching, rate limiting, WebSocket replay/session state
- Spring Web, Spring Security, Spring Data JPA, Flyway, WebSocket/STOMP, Actuator
- OpenAPI/Swagger for API docs
- k6 for load testing
- Docker Compose for local runtime

## Phases

### Phase 1: Foundation & Local Runtime

- Scaffold Spring Boot monolith using hexagonal architecture:
  - domain: entities, value objects, domain events, workflow rules
  - application: command/query services, transactions, use cases
  - infrastructure: REST controllers, persistence adapters, Redis, WebSocket, metrics
- Add Docker Compose with:
  - API service
  - PostgreSQL
  - Redis
- Configure:
  - Flyway migrations
  - OpenAPI docs
  - structured JSON logging
  - correlation ID middleware
  - global typed error responses
  - health endpoints: /api/health/live, /api/health/ready
- Seed data for users, projects, statuses, workflows, sprints, and sample issues.

### Phase 2: Data Model & Persistence

- Create relational schema for:
  - workspaces, users, projects, project memberships
  - issues, issue relationships, labels, watchers
  - sprints, sprint issues
  - comments with threaded replies
  - custom field definitions and custom field values
  - activity log and domain event outbox
  - notifications
  - workflow statuses, transitions, rules, and transition actions
- Add indexes for:
  - project board queries
  - issue key lookup
  - sprint issue lookup
  - assignee/status filtering
  - activity feed pagination
  - full-text search on title, description, comments
- Add optimistic locking via version on mutable aggregates, especially issues.
- Enforce key constraints in DB:
  - valid project membership references
  - parent-child issue relationship integrity
  - unique issue keys per project
  - one active sprint per project unless explicitly designed otherwise.

### Phase 3: Core Issue APIs & Workflow Engine

- Implement REST APIs:
  - POST /api/projects/{projectId}/issues
  - GET /api/projects/{projectId}/board
  - PATCH /api/issues/{issueId}
  - POST /api/issues/{issueId}/transitions
  - GET /api/search
- Implement workflow engine:
  - configurable statuses per project
  - allowed transition validation
  - transition rule evaluation
  - automatic transition actions, starting with reviewer assignment on “In Review”
  - validation hooks, including blocked direct transitions like To Do -> Done
- Emit domain events for every mutation:
  - IssueCreated
  - IssueUpdated
  - IssueMoved
  - StatusChanged
- Write activity log entries from domain events.
- Use idempotency keys for mutation endpoints.

### Phase 4: Sprint Management

- Implement sprint APIs:
  - GET /api/projects/{projectId}/sprints
  - POST /api/projects/{projectId}/sprints
  - PATCH /api/sprints/{sprintId}
  - POST /api/sprints/{sprintId}/start
  - POST /api/sprints/{sprintId}/complete
- Use PostgreSQL advisory locks for sprint start and completion.
- Support:
  - moving issues between backlog and sprint
  - completing sprint with incomplete issue summary
  - selective carry-over into a target sprint or backlog
  - velocity calculation from completed story points
- Ensure sprint completion and carry-over happen atomically.

### Phase 5: Collaboration, Notifications & Activity Feed

- Implement comments:
  - GET /api/issues/{issueId}/comments
  - POST /api/issues/{issueId}/comments
  - threaded replies
  - mention extraction
- Implement watchers:
  - subscribe/unsubscribe users to issues
  - auto-watch reporter and assignee by default
- Implement notification generation for:
  - assignment changes
  - mentions
  - watched issue updates
  - status changes
- Implement activity feed:
  - GET /api/projects/{projectId}/activity
  - cursor pagination
  - filters by actor, issue, event type, and date range
- Add circuit breaker around notification delivery.
- If delivery fails, keep board operations successful and queue notifications for retry.

### Phase 6: Real-Time Sync

- Add WebSocket endpoint for board and issue subscriptions.
- Broadcast event types:
  - issue_created
  - issue_updated
  - issue_moved
  - comment_added
  - sprint_updated
- Track presence:
  - users viewing a board
  - users viewing an issue
- Store recent event stream offsets in Redis.
- Support reconnect with missed event replay using last seen event ID.
- Track WebSocket connection count via metrics.

### Phase 7: Security, RBAC & Rate Limiting

- Add JWT-based auth for prototype users.
- Implement RBAC:
  - Admin
  - Project Lead
  - Member
  - Viewer
- Enforce project-level access in application services and repository queries.
- Add row-level access behavior at service/query level, with DB constraints where practical.
- Add input validation on all request DTOs.
- Add rate limiting per user and endpoint using Redis.
- Add audit logging for sensitive operations:
  - role changes
  - project deletion
  - workflow changes
  - sprint start/complete.

### Phase 8: CQRS, Performance & Reliability

- Separate command-side issue mutations from read-side board queries.
- Add optimized board read model backed by DB queries and Redis cache.
- Prevent N+1 queries using projections/fetch joins where appropriate.
- Add explain-plan documentation for the board query.
- Configure DB connection pooling.
- Add graceful shutdown:
  - stop accepting requests
  - drain in-flight HTTP requests
  - close WebSocket sessions cleanly
- Add metrics:
  - request latency percentiles
  - error rates
  - DB query timings
  - WebSocket connection count
  - cache hit/miss rate.

### Phase 9: Testing & Load Validation

- Unit tests:
  - workflow transition validation
  - optimistic locking conflict behavior
  - sprint carry-over rules
  - RBAC permission checks
- Integration tests:
  - issue CRUD
  - board query
  - sprint start/complete
  - comments and mentions
  - activity feed
  - search
- Concurrency tests:
  - simultaneous issue updates return 409 Conflict
  - concurrent WIP-limited moves do not exceed limit
  - sprint completion is protected by advisory lock
- WebSocket tests:
  - clients receive board updates
  - reconnect replays missed events
- k6 load test:
  - demonstrate 100 concurrent board viewers
  - include documented latency/error results.

### Phase 10: Documentation & Submission

- README:
  - setup instructions
  - Docker Compose commands
  - API overview
  - Swagger URL
  - seed user credentials
  - test and load test commands
- Design docs:
  - architecture overview
  - ERD
  - workflow engine design
  - event-driven flow
  - CQRS/read model strategy
  - scaling strategy
- ADRs:
  - Spring Boot modular monolith
  - PostgreSQL + Redis
  - domain events and outbox
  - optimistic locking
  - WebSocket replay strategy
  - local-first Docker deployment
- Later hosted phase:
  - deploy API, PostgreSQL, Redis
  - configure environment variables/secrets
  - publish hosted prototype URL
  - document operational assumptions.

## Public API & Interface Decisions

- REST API will be versioned under /api/v1.
- Errors will use a consistent shape:
  - code
  - message
  - details
  - correlationId
  - timestamp
- Mutating endpoints will accept Idempotency-Key.
- Issue update endpoints will require the client’s expected version.
- Cursor pagination will be used for board, activity, comments, and search result pagination where relevant.
- WebSocket clients will send last seen event ID during reconnect for replay.

## Test Scenarios

- Concurrent issue updates:
  - first update succeeds
  - second update with stale version returns 409 Conflict
- Workflow violation:
  - invalid transition returns 422 Unprocessable Entity
  - response includes allowed transitions
- Sprint completion:
  - incomplete issues are surfaced
  - selected issues carry over
  - velocity and activity log are updated
- Notification failure:
  - notification circuit breaker opens after repeated failures
  - issue movement still succeeds
  - notification is queued for retry
- Board load:
  - k6 verifies 100 concurrent board viewers with acceptable latency and error rate.

## Assumptions

- The implementation starts as a modular monolith, not separate microservices.
- Local Docker Compose is the first delivery target; hosted deployment is a later phase.
- PostgreSQL full-text search is sufficient for v1; Elasticsearch/OpenSearch is deferred.
- Redis is used for cache, rate limiting, WebSocket replay, and lightweight presence state.
- Auth is JWT-based with seeded demo users for the prototype.
- The project targets the full SDE-2 requirement set.
