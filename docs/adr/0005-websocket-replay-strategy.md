# ADR 0005: WebSocket Replay Strategy

## Status

Accepted

## Context

Board clients need live updates for issue and sprint changes. Clients can disconnect, switch networks, refresh the page, or miss events while the server keeps processing mutations.

The design needs:

- live board and issue event delivery
- reconnect support
- a simple local implementation
- compatibility with multiple API instances later

## Decision Drivers

- low-latency board updates
- simple browser/client protocol
- bounded missed-event recovery
- no separate broker required for local review
- Redis-backed state for future multi-instance operation

## Considered Options

### STOMP Topics + Redis Replay Buffers

Publish live events to STOMP topics and append recent events to Redis lists. Reconnecting clients send `lastSeenEventId` and receive missed events.

Pros:

- simple client model
- works with current Spring WebSocket stack
- Redis buffers are shared across API instances
- tests can verify live delivery and replay

Cons:

- replay is bounded to recent events
- Redis is not the durable source of truth
- current in-memory broker needs sticky sessions or external relay for multi-instance WebSockets

### Polling Only

Clients periodically reload the board.

Pros:

- simplest backend
- no persistent socket connection

Cons:

- higher read traffic
- delayed updates
- poor collaborative UX

### External Message Broker

Use Kafka, RabbitMQ, or managed pub/sub for live and replay behavior.

Pros:

- robust messaging primitives
- stronger replay semantics

Cons:

- more infrastructure than needed for local submission
- more complex setup and tests

## Decision

Use Spring WebSocket/STOMP for live delivery and Redis lists for bounded replay.

Live topics:

- `/topic/projects/{projectId}/board`
- `/topic/issues/{issueId}`

Replay:

- client sends `/app/realtime/replay`
- server responds to `/user/queue/replay`
- request includes `projectId` or `issueId` and optional `lastSeenEventId`

## Consequences

Positive:

- live board updates are pushed immediately
- reconnect can recover missed events
- Redis-backed replay works across API instances
- local setup remains lightweight

Negative:

- event buffers are bounded and temporary
- durable catch-up beyond buffer requires activity/outbox query
- simple broker is not the final production choice for large WebSocket fanout

## Implementation References

- `WebSocketConfig`
- `RealTimeEventPublisher`
- `RealTimeReplayService`
- `RealTimeController`
- `ProjectManagementWebSocketIntegrationTest`

## Revisit When

- board event volume grows beyond simple broker capacity
- replay retention must be durable and long-lived
- API runs multiple WebSocket instances without sticky sessions

