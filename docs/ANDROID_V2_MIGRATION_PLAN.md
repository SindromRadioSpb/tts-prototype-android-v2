# Android v2 Premium Migration Plan

Date: 2026-04-25

## Current Checkpoint

M0 foundation is complete and pushed to `origin/main`.
M1 local Room library storage is implemented at repository level and awaiting final commit/push for this patch.

Completed:

- Native Android skeleton with Kotlin, Jetpack Compose, Gradle Wrapper `8.14.3`.
- `compileSdk=36`, `targetSdk=36`, `minSdk=26`.
- Classic Mode shell and experimental IDE Mode shell.
- Initial domain contracts for library rows/texts, provider policy, and export manifest.
- Initial docs for discovery, provider policy, export, Classic UI, and migration.
- Verification at M0: `.\gradlew.bat test`, `.\gradlew.bat assembleDebug`, `.\gradlew.bat lint`, `git diff --cached --check`.

Current documentation package status:

- Premium documentation control plane was added in docs-only commit `00d1d10 docs(android): add premium migration documentation control plane`.
- Next implementation milestone after M1 is M2: Classic Mode functional flow.

## Dependency Graph

```text
M0 -> documentation control plane
documentation control plane -> M1
M1 -> M2, M6, M7, M8, M11
M2 -> M3, M4, M7, M12
M3 -> M2, M9, M13
M4 -> M5, M9, M13
M5 -> M6, M7, M12
M6 -> M11, M14
M7 -> M8, M12
M8 -> M6, M13
M9 -> M3, M4, M14
M10 -> M13, but must not block M1-M9
M12 -> M13
M13 -> M14
```

## Milestone Roadmap

| Milestone | Risk | Goal | Validation | Rollback | Commit expectation |
|-----------|------|------|------------|----------|--------------------|
| M0 - Foundation checkpoint | Low | Keep buildable native shell and initial contracts. | Existing Gradle test/build/lint. | Revert foundation commits only if skeleton is unusable. | Completed: `3800af6`, `fd819de`. |
| M1 - Local Room library storage | High | Implement Room entities, DAOs, repository transactions for texts/rows/audio metadata. | Unit tests plus Room repository tests for save/load/update/reorder/reset. | Keep UI using in-memory sample state until repository is stable. | In progress: `feat(library): add local room storage`. |
| M2 - Classic Mode functional flow | High | Wire Classic ViewModel to generated rows, save state, errors, and loading. | ViewModel tests and Compose smoke tests. | Feature flag or route back to M0 shell. | `feat(classic): wire generation workflow shell`. |
| M3 - Translation providers | High | Implement allowlisted translation providers and fake providers. | Provider contract tests, timeout/error mapping tests. | Disable real provider in settings, keep fake provider tests. | `feat(provider): add allowed translation providers`. |
| M4 - TTS/audio providers | High | Implement `google_online_tts` and Android TextToSpeech fallback contracts. | Fake TTS tests, platform TTS smoke on emulator/device. | Keep playback disabled with visible unsupported state. | `feat(audio): add tts provider contracts`. |
| M5 - Audio storage and playback | High | Store row/text audio files, play them, mark stale/missing states. | Audio repository tests, manual playback evidence. | Retain metadata but hide playback controls if playback fails. | `feat(audio): add local playback and asset storage`. |
| M6 - Export ZIP with audio | High | Write export ZIP with manifest, library JSON, audio files, missing audio report. | Export snapshot tests and interrupted export tests. | Keep JSON-only export unavailable until ZIP writer is safe. | `feat(export): add audio-aware zip export`. |
| M7 - Library UI and saved text lifecycle | Medium | Browse, open, archive, delete, and save/update library texts. | Repository tests and Compose UI tests. | Keep library screen behind navigation item until stable. | `feat(library): add saved text lifecycle ui`. |
| M8 - Editing/reorder/reset behavior | Medium | Edit row fields, reset, reorder, delete, add rows while preserving metadata. | Regression tests from source behavior and UI evidence. | Disable row mutation actions if persistence invariant breaks. | `feat(library): add row editing workflow`. |
| M9 - API key/settings/security | High | Add encrypted settings, masked key status, update/delete flows. | Security tests, no-secret export/log tests. | Keep real providers disabled until key storage is correct. | `feat(settings): add secure provider configuration`. |
| M10 - IDE Mode experimental integration | Medium | Keep IDE Mode separate and experimental with shared models only. | Navigation tests, no Classic dependency regression. | Hide IDE entry if it destabilizes Classic. | `feat(ide): define experimental workspace shell`. |
| M11 - Import/compatibility layer | Medium | Import Android ZIP and compatible old web JSON where possible. | Import fixture tests and partial import tests. | Import remains read-only preview until safe. | `feat(import): add library compatibility import`. |
| M12 - Premium UI/UX polish | Medium | Improve native phone UX, accessibility, RTL, insets, long text. | UI DoD evidence and accessibility smoke tests. | Keep functional UI if polish causes regressions. | `feat(ui): polish classic mode premium workflow`. |
| M13 - QA hardening | High | Broaden regression suite and stabilize CI expectations. | Full required commands plus targeted instrumented tests. | Fix failing behavior, not tests, unless test is wrong. | `test(android): harden migration regression suite`. |
| M14 - Release readiness | High | Prepare release build, signing plan, privacy checks, release blocker list. | Release checklist and manual QA sign-off. | Do not release until blockers are closed. | `chore(release): prepare android v2 release readiness`. |

## Milestone Detail Requirements

Each milestone patch must include:

- Goal and scoped behavior in the PR/commit notes.
- Files likely affected in the patch register.
- Tests run and test gaps.
- UI DoD evidence if UI changes.
- Documentation updates in the same commit or adjacent docs commit.
- Risk register updates for new or changed risks.

## M1 Implementation Status

Implemented in this patch:

- Room dependencies and KSP configured.
- Room schema version 1 exported under `app/schemas`.
- `library_texts`, `library_rows`, `audio_assets`, `row_audio`, `text_audio`, `export_history`, `provider_call_log`.
- `RoomLibraryRepository` with save/load/update/edit/reset/reorder/delete/add/archive/opened and row audio stale behavior.
- Robolectric Room repository tests for the M1 regression set.

Still out of scope for M1:

- UI wiring.
- Provider network calls.
- Audio playback.
- Export ZIP writer.

## M1 Implementation Notes

Scope:

- Room database, entities, DAOs, repository mapping for `library_texts`, `library_rows`, `audio_assets`, `row_audio`, `text_audio`, `export_history`, and `provider_call_log` if implemented.
- Transaction boundaries for save/update/edit/reorder/delete/archive.
- Tests for duplicate text handling, row order stability, edit metadata, stale audio invalidation.

Out of scope:

- Real provider network calls.
- Audio playback.
- Export ZIP writer.
- UI redesign.

Docs required:

- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [Local Library Plan](ANDROID_V2_LOCAL_LIBRARY_PLAN.md)
- [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md)
- [Patch Register](ANDROID_V2_PATCH_REGISTER.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)

## Blocking Questions

No blocker prevents M2. Open decisions tracked in [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md):

- GCP credential format on device.
- Gemini key strategy.
- niqqud provider strategy.
- old web JSON import compatibility.

## Related Docs

- [Patch Register](ANDROID_V2_PATCH_REGISTER.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)
- [Documentation Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md)
