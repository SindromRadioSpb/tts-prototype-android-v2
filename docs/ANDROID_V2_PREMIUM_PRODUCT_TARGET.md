# Android v2 Premium Product Target

Date: 2026-04-25

## Product Vision

Android v2 is a native, local-first Hebrew/Russian learning app for generating, listening to, editing, saving, and exporting Hebrew study material. Premium means stable ownership of user data, clear provider behavior, readable Hebrew, reliable audio/export flows, and a polished mobile workflow. Premium does not mean adding every experimental provider from the web prototype.

## User Value

- A learner can paste Hebrew text and receive a structured study card/table with original Hebrew, optional niqqud, SBL Academic transliteration, Russian phonetic transliteration when available, and Russian translation.
- A learner can generate or play audio at row and text level, with clear labels for provider and quality.
- A learner owns the local library and can export it with audio-aware metadata.
- Provider failures are visible and actionable; quota, billing, and invalid-key errors are never hidden by silent fallback.

## Primary Workflows

1. Classic Mode generation: enter Hebrew text, choose approved providers, generate rows, review visible provenance and degraded states.
2. Study and playback: play row/text audio, see stale/missing/low-quality indicators, regenerate where supported.
3. Save to library: save generated material locally, reopen it after app restart, preserve row IDs and order.
4. Edit and curate: edit row fields, reset edited fields, reorder rows, delete rows, add rows.
5. Export ownership: create a ZIP containing library JSON, audio files that exist, missing-audio manifest, and privacy-safe metadata.

## Classic Mode

Classic Mode is the production workflow. It gets priority over IDE Mode for architecture, QA, release readiness, and UI polish. It must be mobile-first: row cards replace compressed web tables on phones, touch targets are at least 48dp, long content scrolls, Hebrew remains readable right-to-left, and keyboard/insets behavior is verified.

## IDE Mode

IDE Mode is experimental. It may reuse shared domain models and provider abstractions, but it must not introduce dependencies, state coupling, or UI assumptions that destabilize Classic Mode. IDE Mode must be clearly labeled experimental in UI and documentation.

## Local-First Expectations

- Library data persists on device through Room/SQLite.
- Audio generated on device or returned by allowed online providers is copied into app-controlled storage before it is considered owned.
- Exports do not require cloud storage or a server.
- App startup and library browsing work without network once data exists locally.

## Privacy Expectations

- API keys are not stored in Room, logs, exports, crash text, or Git.
- Keys use Android Keystore-backed encrypted storage.
- Export files include study data and audio metadata, not credentials.
- Provider call logs must avoid raw secrets and must not store full service account JSON.

## Performance and Reliability Bar

- No infinite loading states.
- Long Hebrew input remains scrollable and editable.
- Provider calls use explicit timeout and retry policy.
- Export of a library item does not fail solely because one audio file is missing; it becomes a partial export with a missing-audio manifest.
- Database writes for save/update/edit/reorder run in explicit transactions.

## First Premium Milestone Non-Goals

The first premium milestone does not include a WebView bridge, Railway runtime dependency, cloud app storage, Node server dependency, Python sidecar dependency, local MADLAD/HY-MT runtime, real Room implementation beyond the planned milestone, real provider network code, real TTS implementation, or a production release build.

## Related Docs

- [Architecture](ANDROID_V2_ARCHITECTURE.md)
- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)
