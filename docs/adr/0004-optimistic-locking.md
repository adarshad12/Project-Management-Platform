# ADR 0004: Optimistic Locking

## Status

Accepted

## Context

Multiple users may edit the same issue concurrently. Example:

- User A changes assignee.
- User B changes priority.
- Both read the same original version.

The API must prevent the second write from silently overwriting state based on stale data.

## Decision Drivers

- avoid silent lost updates
- keep normal issue edits fast
- provide clear client retry behavior
- avoid long-held database locks for user editing

## Considered Options

### Optimistic Version Checks

Clients send the issue `version`; stale requests return `409 Conflict`.

Pros:

- no long-lived locks
- straightforward API semantics
- clients can merge and retry
- works well for low-conflict collaborative editing

Cons:

- clients must track version
- retry/merge UX is required

### Pessimistic Locks For Editing

Lock issue rows while users edit.

Pros:

- prevents concurrent modifications up front

Cons:

- poor user experience
- hard to manage abandoned locks
- high operational complexity

### Last Write Wins

Allow later writes to overwrite earlier writes.

Pros:

- simplest implementation

Cons:

- silently loses user changes
- fails the required concurrent update scenario

## Decision

Use optimistic locking on issues:

- `issues.version` is mapped with JPA `@Version`
- update and transition requests require a `version`
- application service checks expected version before mutation
- stale writes return `409 Conflict` with the current version in the message

## Consequences

Positive:

- no silent lost updates
- normal writes remain lightweight
- concurrent tests validate one success and one conflict

Negative:

- clients must include version values
- conflict resolution is a client responsibility

## Implementation References

- `IssueEntity.version`
- `IssueService.validateVersion`
- `UpdateIssueRequest.version`
- `TransitionIssueRequest.version`
- `GlobalExceptionHandler.handleOptimisticLocking`
- `IssueServiceConcurrencyTest`
- `ProjectManagementIntegrationTest.simultaneousIssueUpdatesReturnConflictForStaleWriter`

## Revisit When

- issue editing becomes collaborative in real time
- clients need field-level merge assistance
- high conflict rates suggest a more specialized edit model

