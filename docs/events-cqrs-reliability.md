# Events, CQRS, And Reliability

This document is the combined reliability summary. For focused design details, see:

- [Event-Driven Flow](event-driven-flow.md)
- [CQRS And Board Read Model](cqrs-read-model.md)

## Event Flow

Mutations record durable activity and outbox rows:

```text
REST mutation
  |
  v
Application service
  |
  +-- source table write
  +-- activity_log insert
  +-- domain_event_outbox insert
  +-- board read model refresh
  +-- Redis cache eviction
  +-- WebSocket/STOMP publish
  +-- Redis replay buffer append
```

## Activity Feed

`GET /api/v1/projects/{projectId}/activity` reads `activity_log`.

Supported filters:

- cursor
- actor
- issue
- event type
- date range
- limit

## Domain Outbox

`domain_event_outbox` stores event payloads for durable processing.

Current state:

- Event rows are written for issue and sprint events.
- Real-time publication is synchronous and best effort.
- The local outbox dispatcher processes unprocessed rows and marks them with `processed_at`; an external broker can replace this worker later.

## CQRS Board Read Model

The board endpoint uses `board_issue_read_model`.

Why:

- board reads are frequent
- board shape is denormalized
- read model avoids repeated joins on hot paths
- Redis can cache the final response briefly

Refresh behavior:

- issue create/update/transition refreshes the affected issue projection
- sprint operations refresh affected issues
- cache key `board:project:{projectId}` is evicted on changes
- if read model is empty, service falls back to canonical issue query

Metrics:

- `board.read.latency`
- `board.read_model.refresh.latency`

## Real-Time Sync

WebSocket/STOMP endpoint:

- `/ws`

Topics:

- `/topic/projects/{projectId}/board`
- `/topic/issues/{issueId}`
- `/topic/projects/{projectId}/presence`
- `/topic/issues/{issueId}/presence`

Replay:

- client sends `/app/realtime/replay`
- response goes to `/user/queue/replay`
- Redis replay lists store recent project and issue events

Event types:

- `issue_created`
- `issue_updated`
- `issue_moved`
- `comment_added`
- `sprint_updated`

## Reliability Patterns

Optimistic locking:

- issue clients must send `version`
- stale writes return `409 Conflict`

Advisory locks:

- sprint start and completion use PostgreSQL advisory transaction locks
- concurrent completion serializes; the second request observes completed state and returns `409`

Notification resilience:

- notification rows are queued before delivery attempt
- delivery failure does not fail board operations
- circuit breaker opens after repeated delivery failures

Graceful shutdown:

- Spring Boot graceful shutdown is enabled
- shutdown timeout is configurable
