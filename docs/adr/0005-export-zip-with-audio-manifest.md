# ADR-0005: Export ZIP With Audio Manifest

Date: 2026-04-25
Status: Accepted

## Context

The Android app must preserve user ownership of library data and audio. Export must not silently lose audio.

## Decision

Use a ZIP export containing `manifest.json`, `library/library.json`, audio files, and `metadata/missing_audio.json`.

## Consequences

- Missing audio produces a partial export, not silent omission.
- Export schema versioning is independent of Room schema versioning.
- Import can later support Android ZIP and old web JSON compatibility.

## Alternatives considered

- JSON-only export: rejected because audio ownership is a product requirement.
- Fail on any missing audio: rejected because one missing file should not block backup.

## Follow-up

Implement M6 export writer and partial export tests.
