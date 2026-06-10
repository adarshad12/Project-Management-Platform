# Workflow Engine

## Workflow Model

Workflows are project-scoped.

Each project has:

- ordered `workflow_statuses`
- allowed `workflow_transitions`
- optional WIP limits per status

Seed workflow:

```text
To Do -> In Progress -> In Review -> Done
           |
           v
         To Do
```

## Transition Validation

`POST /api/v1/issues/{issueId}/transitions` accepts:

```json
{
  "version": 0,
  "toStatus": "In Progress"
}
```

Validation order:

1. Load issue by UUID or issue key.
2. Validate optimistic `version`.
3. Resolve target workflow status by project and name.
4. Check a matching transition exists from current status to target status.
5. Run JSON-configured validation hooks.
6. Enforce WIP limit when target status has `wip_limit`.
7. Apply transition action if configured.
8. Save issue and publish activity/outbox/real-time events.

Invalid transitions return:

- HTTP status: `422 Unprocessable Entity`
- Error code: `WORKFLOW_VIOLATION`
- Message includes allowed transitions from the current status.

Stale versions return:

- HTTP status: `409 Conflict`
- Error code: `CONFLICT`
- Message includes the current version.

## WIP Limit Enforcement

WIP enforcement is designed for concurrent moves:

1. The target workflow status row is locked with `PESSIMISTIC_WRITE`.
2. The service counts issues currently in the target status.
3. If count is at or above the limit, the move is rejected with `409 Conflict`.
4. Otherwise, the issue is moved and saved.

This serializes competing moves into the same limited column and prevents the limit from being exceeded.

## Transition Actions

`workflow_transitions.action_config` stores JSON configuration.

Implemented action:

- `{"assignReviewer": true}` assigns the first project lead when moving into review.
- `{"assignProjectLead": true}` also assigns the first project lead.

Implemented validation hooks:

- `{"requireAssignee": true}` rejects transitions for unassigned issues.
- `{"requireStoryPoints": true}` rejects transitions without story points.

## Tested Scenarios

Automated tests cover:

- invalid transition returns `422`
- allowed transition list is included in error message
- stale update returns `409`
- concurrent WIP-limited moves do not exceed limit
