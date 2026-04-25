# ADR-0002: Local-First Room Storage

Date: 2026-04-25
Status: Accepted

## Context

Android v2 must not depend on cloud app storage or a Node server as required runtime. Users must own saved library data on device and export it.

## Decision

Use Room/SQLite for local library storage, with app-controlled audio files and exportable metadata.

## Consequences

- Repository transactions own save/update/edit/reorder behavior.
- Export reads local snapshots.
- Cloud services may generate translation/TTS but cannot be required for library ownership.

## Alternatives considered

- Cloud database: rejected by local-first constraint.
- Server-backed library: rejected because Node server cannot be required runtime.

## Follow-up

Implement M1 Room schema and migration tests.
