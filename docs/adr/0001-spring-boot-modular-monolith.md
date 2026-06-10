# ADR 0001: Spring Boot Modular Monolith

## Status

Accepted

## Context

The project needs to demonstrate a broad backend surface in a take-home-sized package:

- issue workflow and board APIs
- sprint lifecycle and carry-over
- comments, mentions, notifications, and activity feed
- WebSocket sync and replay
- RBAC and rate limiting
- CQRS/read model behavior
- repeatable local setup and tests

Splitting these capabilities into separate services would add network contracts, deployment complexity, service discovery, distributed tracing, and cross-service transaction problems before the domain boundaries have enough traffic evidence to justify that cost.

## Decision Drivers

- keep local review simple
- keep transaction boundaries explicit
- support fast implementation across multiple product phases
- avoid premature distributed-system complexity
- preserve clear capability boundaries for later extraction

## Considered Options

### Modular Monolith With Spring Boot

Capabilities live in separate Java packages and share one deployable API process.

Pros:

- easiest local setup
- single database transaction per workflow
- simpler tests and debugging
- fits the assignment scope
- packages can become future service boundaries

Cons:

- package discipline is required
- all capabilities scale together at first

### Microservices

Separate issue, sprint, collaboration, notification, and real-time services.

Pros:

- independent deployment and scaling
- stronger runtime isolation

Cons:

- too much operational overhead for this scope
- distributed transactions or eventual consistency would be required earlier
- harder for reviewers to run locally

### Single Flat Application

All code in broad controller/service/repository packages.

Pros:

- fastest initial scaffolding

Cons:

- weak boundaries
- harder to evolve or extract capabilities
- higher risk of unrelated changes touching shared files

## Decision

Use a Spring Boot modular monolith organized by capability packages:

- `issue`
- `sprint`
- `collaboration`
- `realtime`
- `security`
- `infrastructure.persistence`
- `common`
- `config`

## Consequences

Positive:

- one Dockerized API is easy to run and review
- PostgreSQL transactions can protect issue and sprint invariants
- test suite can exercise full flows without distributed dependencies
- capability packages keep the codebase navigable

Negative:

- a failure in one module can affect the process
- independent runtime scaling is deferred
- module boundaries are conventions, not separate deployment units

## Implementation References

- `src/main/java/com/dealshare/projectmanagement/issue`
- `src/main/java/com/dealshare/projectmanagement/sprint`
- `src/main/java/com/dealshare/projectmanagement/collaboration`
- `src/main/java/com/dealshare/projectmanagement/realtime`
- `src/main/java/com/dealshare/projectmanagement/security`

## Revisit When

- traffic patterns show one capability needs independent scaling
- team ownership requires independent deployability
- external integrations need isolated failure domains

