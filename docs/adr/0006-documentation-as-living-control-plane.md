# ADR-0006: Documentation as Living Control Plane

Date: 2026-04-25
Status: Accepted

## Context

Android v2 will be built incrementally by future agents and a small team. Stale docs would cause provider, storage, export, and UI drift.

## Decision

Documentation is part of implementation. Behavior-changing patches must update affected docs without waiting for manual reminders.

## Consequences

- Patch register, migration plan, and risk register are operational documents.
- Architecture decisions require ADR updates.
- UI, provider, data, export, security, QA, and release changes update their owning docs.

## Alternatives considered

- Static documentation package: rejected because it would become stale immediately.
- Update docs only on request: rejected because it makes correctness depend on manual reminders.

## Follow-up

Enforce [Documentation Maintenance Policy](../DOCUMENTATION_MAINTENANCE_POLICY.md) in every patch.
