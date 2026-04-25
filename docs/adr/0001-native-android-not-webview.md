# ADR-0001: Native Android, Not WebView

Date: 2026-04-25
Status: Accepted

## Context

The source product is a web app. Android v2 must be local-first, mobile-first, export-capable, and free of browser-only runtime assumptions.

## Decision

Build Android v2 as a native Kotlin/Jetpack Compose app, not a WebView wrapper.

## Consequences

- UI must be rebuilt as native phone workflows.
- Browser APIs and Web/WASM assumptions are not runtime dependencies.
- Source behavior must be migrated through domain contracts and docs, not copied as web UI.

## Alternatives considered

- WebView wrapper: rejected because it preserves browser constraints and weakens local-first ownership.
- Hybrid bridge: rejected for the first premium milestone because it adds runtime complexity.

## Follow-up

Keep source repo as behavioral reference only.
