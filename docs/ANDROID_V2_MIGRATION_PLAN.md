# Android v2 Premium Migration Plan

Date: 2026-04-25

## Current Checkpoint

M0 foundation is complete and pushed to `origin/main`.
M1 local Room library storage is implemented at repository level in commit `60dd079 feat(library): add local room storage`.
M2 Classic Mode functional flow is implemented in commit `479d50f feat(classic): wire generation workflow shell`.
M3 translation provider layer is implemented in commit `bfa8613 feat(provider): add allowed translation providers`.
M4 TTS/audio provider contracts are implemented in commit `79d5b57 feat(audio): add tts provider contracts`.
M5 audio storage and playback layer is implemented in commit `c82a734`.
M6 export ZIP with audio is implemented in commit `4ac57c9`.
M7 library UI and saved text lifecycle is implemented in commit `4396158`.
M8 editing/reorder/reset behavior is implemented in commit `e17cd9e`.
M9 API key/settings/security is implemented in commit `7e3a082`.

Completed:

- Native Android skeleton with Kotlin, Jetpack Compose, Gradle Wrapper `8.14.3`.
- `compileSdk=36`, `targetSdk=36`, `minSdk=26`.
- Classic Mode shell and experimental IDE Mode shell.
- Initial domain contracts for library rows/texts, provider policy, and export manifest.
- Initial docs for discovery, provider policy, export, Classic UI, and migration.
- Verification at M0: `.\gradlew.bat test`, `.\gradlew.bat assembleDebug`, `.\gradlew.bat lint`, `git diff --cached --check`.

Current documentation package status:

- Premium documentation control plane was added in docs-only commit `00d1d10 docs(android): add premium migration documentation control plane`.
- Next implementation milestone after M9 is M10: IDE Mode experimental integration.

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
| M1 - Local Room library storage | High | Implement Room entities, DAOs, repository transactions for texts/rows/audio metadata. | Unit tests plus Room repository tests for save/load/update/reorder/reset. | Keep UI using in-memory sample state until repository is stable. | Completed: `60dd079 feat(library): add local room storage`. |
| M2 - Classic Mode functional flow | High | Wire Classic ViewModel to generated rows, save state, errors, and loading. | ViewModel tests; Compose smoke tests still pending. | Route back to M0 shell by reverting `feature/classic` and `AppRoot` wiring. | Completed: `479d50f feat(classic): wire generation workflow shell`. |
| M3 - Translation providers | High | Implement allowlisted translation providers and fake providers. | Provider contract tests, timeout/error mapping tests. | Keep provider registry but disable failing provider in UI/settings; fake tests remain. | Completed: `bfa8613 feat(provider): add allowed translation providers`. |
| M4 - TTS/audio providers | High | Implement `google_online_tts` and Android TextToSpeech fallback contracts. | Fake TTS tests; platform TTS smoke still pending on emulator/device. | Keep playback disabled with visible unsupported state. | Completed: `79d5b57 feat(audio): add tts provider contracts`. |
| M5 - Audio storage and playback | High | Store row/text audio files, play them, mark stale/missing states. | Audio repository tests, manual playback evidence. | Retain metadata but hide playback controls if playback fails. | Completed in `c82a734`; manual playback evidence remains a release hardening item. |
| M6 - Export ZIP with audio | High | Write export ZIP with manifest, library JSON, audio files, missing audio report. | Export snapshot tests and interrupted export tests. | Keep JSON-only export unavailable until ZIP writer is safe. | Completed in `4ac57c9`; SAF/share UI evidence remains future work. |
| M7 - Library UI and saved text lifecycle | Medium | Browse, open, archive, delete, and save/update library texts. | Repository tests and Compose UI tests. | Keep library screen behind navigation item until stable. | Completed in `4396158`; manual UI evidence remains future work. |
| M8 - Editing/reorder/reset behavior | Medium | Edit row fields, reset, reorder, delete, add rows while preserving metadata. | `LibraryViewModelTest`, repository regression tests, UI evidence later. | Disable row mutation actions if persistence invariant breaks. | Completed in `e17cd9e`; manual UI evidence remains future work. |
| M9 - API key/settings/security | High | Add encrypted settings, masked key status, update/delete flows. | Settings repository/ViewModel tests, no-secret export/log tests. | Keep real providers disabled until provider-specific auth is safe. | Completed in `7e3a082`; manual Keystore/UI evidence remains future work. |
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

## M2 Implementation Status

Implemented in commit `479d50f`:

- `ClassicModeViewModel` owns source text, selected allowlisted providers, generated row state, saving state, and user-visible messages.
- Initial generation used a clearly labeled M2 fake shell; M3 replaces this path with the translation provider registry.
- `MainActivity` uses manual DI through `TtsPrototypeApplication`; no Hilt is introduced.
- Classic UI now generates rows, shows fake/degraded state, saves generated rows into the local Room library, and displays saved summary count.
- `LibraryRepository` port was added so ViewModel tests use deterministic fake storage while runtime still uses `RoomLibraryRepository`.

Still out of scope for M2:

- Real translation providers.
- Real niqqud generation.
- Real TTS/audio playback.
- Row editing/reorder/reset UI.
- Export/import UI.
- Secure provider key settings.

Docs required:

- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)
- [Patch Register](ANDROID_V2_PATCH_REGISTER.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)

## M3 Implementation Status

Implemented in commit `bfa8613`:

- `TranslationProviderRegistry` enforces the Android v2 allowlist before runtime provider lookup.
- `FakeTranslationProvider` provides deterministic CI-safe rows for tests and ViewModel injection.
- `GoogleTranslateFreeProvider` implements the no-key best-effort HTTP path with timeout, response parsing, provenance, and error mapping.
- `MissingConfigurationTranslationProvider` makes `gcp_translate` and `gemini_legacy` visible as allowed but blocked until M9 secure settings.
- Classic Mode now calls the selected translation provider and surfaces provider failures without silent fallback.

Still out of scope for M3:

- Secure key storage and validation UI for GCP/Gemini.
- Service account JSON loading on device.
- Gemini network adapter.
- niqqud generation.
- SBL Academic and Russian phonetic transliteration parity.
- TTS/audio generation and playback.

Docs required:

- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)

## M4 Implementation Status

Implemented in commit `79d5b57`:

- `TtsProviderRegistry` enforces the Android v2 TTS allowlist before runtime provider lookup.
- `FakeTtsProvider` returns deterministic metadata and asset keys for CI without audio engine or network.
- `MissingConfigurationTtsProvider` keeps `google_online_tts` allowlisted but blocked until M9 secure settings.
- `AndroidPlatformTtsProvider` wraps Android `TextToSpeech.synthesizeToFile` for `system_or_browser_fallback_low_quality`.
- Deterministic audio asset keys are generated from provider/profile/text without including secrets.
- `TtsPrototypeApplication` exposes the Android TTS registry for later M5/M7 UI wiring.

Still out of scope for M4:

- Playback UI and Media3/Android playback state.
- Moving generated files into final `audio_assets` storage.
- Row/text audio metadata persistence from TTS results.
- Google Online TTS network adapter with credentials.
- Service account JSON handling in the APK.
- Emulator/device TTS smoke evidence.

Docs required:

- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)

## M5 Implementation Status

Implemented in patch P007, commit `c82a734`:

- `AudioStorageRepository` adopts provider TTS output into app-controlled storage under `filesDir/audio/rows/{textId}/{rowId}/`.
- Adopted row audio is recorded in Room `audio_assets` and linked as the row default in `row_audio`.
- Default row audio replacement clears the prior row default before inserting the new link.
- `resolvePlayableAudio` validates the file exists before playback and marks `audio_assets.is_missing=true` if the file is absent.
- `AndroidAudioPlaybackController` wraps Android `MediaPlayer` behind an interface and returns a failure for missing files before constructing a player.
- `TtsResponse` now carries `localFilePath`, `durationMs`, and `sizeBytes` metadata needed by storage adoption.

Still out of scope for M5:

- Compose playback buttons wired to row cards.
- Text-level audio adoption.
- Media3 session/background playback.
- Device/emulator audio smoke evidence.
- Export ZIP writer.

Docs required:

- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)

## M6 Implementation Status

Implemented in patch P008, commit `4ac57c9`:

- `LibraryZipExportRepository` writes ZIP files with `manifest.json`, `library/library.json`, `metadata/missing_audio.json`, and available audio files.
- Export reads an immutable Room snapshot, then copies audio files outside the DB read transaction.
- Missing or unsafe audio files create `partial_backup=true` and `metadata/missing_audio.json` entries instead of failing the whole export.
- ZIP audio paths are validated to reject absolute paths, backslashes, drive prefixes, empty segments, `.` segments, and `..` segments.
- `export_history` records successful and failed export attempts without storing secrets.

Still out of scope for M6:

- Storage Access Framework UI and share sheet wiring.
- Android device manual evidence for interrupted SAF writes.
- Import implementation.

## M7 Implementation Status

Implemented in patch P009, commit `4396158`:

- `LibraryViewModel` observes active/archived Room summaries, opens selected texts, marks `last_opened_at`, archives/restores, and deletes selected texts.
- `AppRoot` adds a separate `Library` tab between Classic and IDE.
- The Library screen lists saved summaries, toggles archived visibility, opens selected text details, previews rows, and exposes archive/restore/delete actions.
- `RoomLibraryRepository.deleteText` deletes a text through Room with cascade cleanup of rows and link tables.
- Tests cover open/last-opened, archive/restore/delete lifecycle, and repository hard delete behavior.

Still out of scope for M7:

- Row editing/reorder/reset UI, handled by M8.
- SAF/share export UI wiring.
- Compose screenshot/manual device evidence.

## M8 Implementation Status

Implemented in patch P010, commit `e17cd9e`:

- `LibraryViewModel` exposes row draft state for editing and adding rows.
- Library tab row cards expose explicit `Edit`, `Reset`, `Up`, `Down`, `Add after`, and `Delete row` actions.
- Row edit form covers Hebrew original, Hebrew with niqqud, SBL transliteration, Russian phonetic transliteration, and Russian translation.
- Save only patches changed row fields; adding a row requires at least one non-blank field.
- Reset restores repository-tracked edited fields from `edit_meta`.
- Reorder uses exact row ID ordering through `RoomLibraryRepository.reorderRows`.
- `LibraryViewModelTest` covers edit/reset/reorder/delete/add and empty new-row validation.

Still out of scope for M8:

- Drag-and-drop reorder UI.
- Compose screenshot/manual device evidence.
- SAF/share export UI wiring.
- TTS playback controls on row cards.

## M9 Implementation Status

Implemented in patch P011, commit `7e3a082`:

- `ProviderCredentialId` defines the provider credentials currently allowed in Settings: `gcp_translate`, `gemini_legacy`, and `google_online_tts`.
- `AndroidKeystoreSecureKeyValueStore` stores credential values in `SharedPreferences` encrypted with an Android Keystore AES/GCM key.
- `ProviderSettingsRepository` exposes configured/missing status, masked values, update, delete, and lookup for future provider adapters.
- Credential validation rejects blank values, multiline values, oversized values, and raw service account JSON/private-key material.
- `SettingsViewModel` owns credential drafts and visible status messages.
- `AppRoot` adds a dedicated Settings tab with provider status, single-line credential input, save, and delete actions.
- Unit tests cover masking, delete, JSON rejection, multiline rejection, and ViewModel status updates.

Still out of scope for M9:

- Real GCP/Gemini/Google Online TTS network adapters using the stored credentials.
- Storing raw service account JSON in Android.
- Device/emulator screenshot evidence for Settings.
- Provider validation calls against real endpoints.

## Blocking Questions

No blocker prevents M10. Open decisions tracked in [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md):

- Provider-specific validation strategy for stored keys.
- niqqud provider strategy.
- old web JSON import compatibility.

## Related Docs

- [Patch Register](ANDROID_V2_PATCH_REGISTER.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)
- [Documentation Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md)
