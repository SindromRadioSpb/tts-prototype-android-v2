# ADR-0004: Classic Mode Primary, IDE Mode Experimental

Date: 2026-04-25
Status: Accepted

## Context

Classic Mode is the core Hebrew/Russian learning workflow. IDE Mode is useful for future advanced workflows but can create scope creep.

## Decision

Classic Mode is the primary production workflow. IDE Mode remains separate, visibly experimental, and must not delay Classic Mode.

## Consequences

- Classic Mode gets priority for persistence, providers, audio, export, QA, and polish.
- IDE Mode may reuse shared models but must not own shared architecture.
- IDE Mode can be hidden or reduced if it destabilizes Classic.

## Alternatives considered

- Equal priority modes: rejected because it would slow the premium core workflow.
- Remove IDE Mode entirely: rejected because future advanced workflows are valuable.

## Follow-up

Implement IDE functionality only at M10 or later.
