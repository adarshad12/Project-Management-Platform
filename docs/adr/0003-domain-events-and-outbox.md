# ADR 0003: Domain Events And Outbox

## Status

Accepted

## Context

Issue, sprint, and collaboration mutations need secondary effects:

- activity feed rows
- notification records
- WebSocket events
- replay buffer entries
- future external integrations

These effects should not make source-of-truth writes unreliable. For example, a notification delivery failure must not prevent moving an issue on the board.

## Decision Drivers

- preserve durable event history
- keep source mutations reliable
- allow future asynchronous processing
- avoid requiring Kafka/RabbitMQ for local evaluation
- support real-time UX while keeping PostgreSQL as recovery source

## Considered Options

### Activity Log + Domain Outbox In PostgreSQL

Write event rows in the same application flow as the source mutation.

Pros:

- durable audit trail
- easy local setup
- can be processed asynchronously later
- source data and event records share the same database

Cons:

- current implementation still publishes real-time messages synchronously
- a dispatcher is needed for full outbox processing

### Direct Synchronous Side Effects Only

Call notification and WebSocket delivery directly without durable event rows.

Pros:

- less schema and code

Cons:

- side-effect failures can lose event history
- no durable retry path
- harder to audit

### External Message Broker Immediately

Publish events to Kafka, RabbitMQ, or similar infrastructure.

Pros:

- strong async event processing model
- consumer groups and replay options

Cons:

- too much infrastructure for local review
- source write and publish consistency needs extra handling

## Decision

Write `activity_log` and `domain_event_outbox` rows during mutations. Queue notification records before delivery attempts. Publish WebSocket events best effort and store recent replay events in Redis.

## Consequences

Positive:

- activity feed is durable
- notification delivery failure does not break board operations
- local outbox worker processes unprocessed rows and marks `processed_at`
- local setup remains PostgreSQL + Redis only

Negative:

- current outbox is persisted but not dispatched by a background worker
- real-time delivery is best effort
- external service integration remains future work

## Implementation References

- `ActivityLogEntity`
- `DomainEventOutboxEntity`
- `NotificationEntity`
- `IssueService.recordEvent`
- `SprintService.recordSprintEvent`
- `CollaborationService.recordIssueEvent`
- `RealTimeEventPublisher`

## Revisit When

- external notification delivery is added
- events need guaranteed delivery outside the API process
- consumers need independent scaling or replay beyond Redis buffers
