# ADR 0002: PostgreSQL And Redis

## Status

Accepted

## Context

The system needs both durable relational consistency and low-latency shared state.

Required database capabilities:

- relational constraints for projects, issues, workflows, sprints, and memberships
- optimistic locking for issue updates
- advisory transaction locks for sprint start/completion
- full-text search across issues and comments
- partial indexes, including one active sprint per project
- schema migrations and deterministic seed data

Required low-latency capabilities:

- fixed-window rate limiting
- board response cache
- WebSocket replay buffers
- presence/session state

## Decision Drivers

- correctness for core project-management data
- strong local development story
- avoid extra search/message-broker infrastructure in v1
- use mature infrastructure that reviewers can run with Docker Compose

## Considered Options

### PostgreSQL + Redis

PostgreSQL is the source of truth. Redis holds cache and ephemeral/shared state.

Pros:

- PostgreSQL covers relational data, search, locks, and migrations
- Redis covers low-latency counters, cache, replay, and presence
- both are easy to run locally
- production migration path is straightforward

Cons:

- two backing services to operate
- Redis data is not source of truth

### PostgreSQL Only

Use PostgreSQL for everything.

Pros:

- fewer services
- transactional consistency everywhere

Cons:

- less suitable for high-frequency counters/presence
- replay and cache workloads would add pressure to the primary database

### PostgreSQL + Elasticsearch/OpenSearch + Message Broker

Add dedicated search and messaging infrastructure.

Pros:

- stronger scaling options for search and events

Cons:

- overbuilt for local evaluation
- more operational complexity
- more failure modes for a take-home package

## Decision

Use PostgreSQL as the source-of-truth database and Redis for shared low-latency state.

## Consequences

PostgreSQL owns:

- normalized entities
- board read model
- full-text search
- activity and outbox rows
- advisory locks
- constraints and indexes

Redis owns:

- board cache
- rate-limit counters
- WebSocket replay buffers
- presence state

## Implementation References

- `docker-compose.yml`
- `src/main/resources/db/migration`
- `src/main/java/com/dealshare/projectmanagement/infrastructure/persistence`
- `BoardReadModelService`
- `RateLimitFilter`
- `RealTimeReplayService`
- `RealTimePresenceService`

## Revisit When

- full-text search needs relevance tuning beyond PostgreSQL
- replay volume requires a real stream platform
- Redis memory growth requires cluster or managed service changes

