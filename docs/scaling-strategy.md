# Scaling Strategy

## Current Local Topology

```text
1 Spring Boot API instance
1 PostgreSQL database
1 Redis instance
```

This is enough for local evaluation and deterministic tests.

## Horizontal API Scaling

The API is mostly stateless:

- JWTs are self-contained.
- RBAC is loaded from PostgreSQL.
- Rate-limit counters are in Redis.
- Board cache is in Redis.
- WebSocket replay buffers are in Redis.
- Presence state is in Redis.

Multiple API instances can run behind a load balancer if they share PostgreSQL and Redis.

## Database Scaling

PostgreSQL remains the source of truth.

Recommended next steps:

- tune Hikari pool sizes per instance
- add read replicas for analytical or reporting queries
- keep write traffic on primary
- partition or archive old activity/outbox data if volume grows
- monitor slow queries and index usage

Current indexes support:

- board read model by project/status
- board read model by updated timestamp
- issue project/status
- issue sprint
- comments by issue and created time
- full-text search on issues and comments
- activity feed by project and created time

## Redis Scaling

Redis handles low-latency shared state:

- rate limiting
- board cache
- WebSocket replay buffers
- presence state

For production-like scaling:

- use managed Redis or Redis Cluster
- set memory limits and eviction policy
- bound replay buffer sizes per project and issue
- add metrics for cache hit/miss and replay sizes

## WebSocket Scaling

The current broker is Spring's in-memory simple broker. This is fine for local evaluation.

Production options:

- run sticky sessions for WebSocket clients
- or replace simple broker with external broker relay
- keep replay buffers in Redis so reconnect recovery is instance-independent

## Load Test Baseline

Recorded local board read load:

- 100 concurrent viewers
- 1 minute duration
- 6000 board iterations
- 0 failed requests
- p95 latency 38.12 ms

See:

- `load/results/board-viewers-100vus.md`

## Operational Metrics

Available through actuator/Prometheus:

- JVM and HTTP metrics
- board read latency
- board read-model refresh latency
- active WebSocket connections
- Hikari pool metrics

Useful URL:

- `http://localhost:8080/actuator/prometheus`

