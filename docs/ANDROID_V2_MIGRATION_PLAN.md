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
M9 follow-up keyed provider smoke adapters are implemented in commit `0f3990c`: stored single-line credentials now feed GCP Translate, Gemini, and Google Online TTS HTTP adapters.
P015 implements real JSON credential attachment and provider auth: Settings can attach/validate JSON through Android SAF, `gcp_translate` and `google_online_tts` support service-account OAuth JWT bearer calls, and `gemini_legacy` supports the provider-specific JSON API-key wrapper.
Classic Mode / Library v3 mobile parity based on v4 screenshots is now specified in `docs/ui/ANDROID_V2_CLASSIC_MODE_MOBILE_V4_UI_SPEC.md`.
P014 implements the first native v4 parity pass in commit `844239b`: Classic-owned `📚 Библиотека`, Library v3 modal, filters/dropdowns/tag chips, stacked action cards without clipping, text-level metadata editor, and Room schema version 2 metadata fields.

Completed:

- Native Android skeleton with Kotlin, Jetpack Compose, Gradle Wrapper `8.14.3`.
- `compileSdk=36`, `targetSdk=36`, `minSdk=26`.
- Classic Mode shell and experimental IDE Mode shell.
- Initial domain contracts for library rows/texts, provider policy, and export manifest.
- Initial docs for discovery, provider policy, export, Classic UI, and migration.
- Verification at M0: `.\gradlew.bat test`, `.\gradlew.bat assembleDebug`, `.\gradlew.bat lint`, `git diff --cached --check`.

Current documentation package status:

- Premium documentation control plane was added in docs-only commit `00d1d10 docs(android): add premium migration documentation control plane`.
- Next recommended implementation milestone is M12/M13 real-device evidence hardening: capture emulator/device screenshots for Classic portrait, Library v3 filters/dropdowns/cards, metadata editor with keyboard, Settings JSON attachment, real GCP/Gemini/Google TTS smoke, and audio playback UI wiring.

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
v4 UI parity spec -> M7, M8, M9, M12, M13
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
| M7 - Library UI and saved text lifecycle | Medium | Browse, open, archive, delete, save/update library texts, and converge on Library v3 modal/screen parity. | Repository tests, Compose UI tests, v4 screenshot evidence. | Keep current Library tab usable while implementing Classic-owned Library v3 entry. | Completed in `4396158`; P014 replaces the rough separate Library tab with Classic-owned Library v3 modal behavior. |
| M8 - Editing/reorder/reset behavior | Medium | Edit row fields plus text-level metadata editor parity where scoped. | `LibraryViewModelTest`, repository regression tests, metadata editor UI evidence. | Disable mutation actions if persistence invariant breaks. | Completed in `e17cd9e`; P014 adds text-level metadata editor parity for title/level/tags/source/topic. |
| M9 - API key/settings/security | High | Add encrypted settings, masked key status, update/delete flows, keyed providers, and migrate target UX to JSON credential attachment/validation. | Settings repository/ViewModel tests, no-secret export/log tests, keyed adapter contract tests, SAF credential UI tests. | Disable only failing keyed provider while preserving visible errors and no silent fallback. | Completed in `7e3a082`; keyed adapter follow-up complete; P015 adds JSON attachment/validation and service-account bearer auth. |
| M10 - IDE Mode experimental integration | Medium | Keep IDE Mode separate and experimental with shared models only. | Navigation tests, no Classic dependency regression. | Hide IDE entry if it destabilizes Classic. | `feat(ide): define experimental workspace shell`. |
| M11 - Import/compatibility layer | Medium | Import Android ZIP and compatible old web JSON where possible. | Import fixture tests and partial import tests. | Import remains read-only preview until safe. | `feat(import): add library compatibility import`. |
| M12 - Premium UI/UX polish | Medium | Implement and verify v4 screenshot-based Classic/Library mobile parity, accessibility, RTL, insets, long text, no clipped actions. | UI DoD evidence, screenshot/manual smoke, accessibility smoke tests. | Keep functional UI if visual polish causes regressions. | P014 implements the first code pass; manual evidence and Compose UI tests remain required. |
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
- v4 screenshot parity screenshot evidence. P014 implements the first native modal/card/filter pass, but manual evidence remains required.

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
- Manual keyboard evidence for v4 text-level metadata editor parity. P014 implements the editor fields and Room metadata persistence.

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

- Storing raw service account JSON in Android.
- Device/emulator screenshot evidence for Settings.
- Formal provider validation button/check per credential.

## M9 Keyed Provider Follow-up Status

Implemented in patch P012:

- `createAndroidTranslationProviderRegistry` now receives `ProviderSettingsRepository` from `TtsPrototypeApplication`.
- `gcp_translate` reads the stored `gcp_translate` credential and calls Google Cloud Translation Basic v2 with JSON POST.
- `gemini_legacy` reads the stored `gemini_legacy` credential and calls Gemini `gemini-2.0-flash` for Hebrew-to-Russian translation.
- `google_online_tts` reads the stored `google_online_tts` credential, calls Google Cloud Text-to-Speech `text:synthesize`, decodes returned MP3 bytes, and writes them under app cache for smoke validation.
- Classic Mode `Speak` now invokes the selected TTS provider for source-level Hebrew synthesis and shows either the generated local file name or the mapped provider error.
- Unit tests cover credential usage, missing-credential no-fallback behavior, GCP/Gemini parsing, and Google TTS audio-file creation.

Still out of scope for P012:

- Copying service account JSON/private keys into Android. The Settings validation still rejects that material.
- Persisting the Classic `Speak` result into `audio_assets`, `row_audio`, or `text_audio`.
- Audible playback from the Classic `Speak` button.
- Real endpoint validation in CI. CI remains fake/stubbed and must not require provider credentials.
- Manual Android Keystore, emulator/device, quota/billing, and real network smoke evidence.

## P013 UI Specification Status

Specified in docs-only patch P013:

- v4 screenshot inventory copied into `docs/ui/v4`.
- Library v3 target flow belongs to Classic Mode through `📚 Библиотека`, not IDE Mode.
- Library v3 filter order, dropdown behavior, tag chips, saved-text cards, action buttons, and metadata editor are now implementation contracts.
- Android must preserve action order and visual hierarchy while fixing source web clipping/overflow.
- Settings target UX for credentials is JSON file attachment and validation through Android Storage Access Framework; current single-line key input is interim.

Implementation required after P013:

- Capture emulator/device evidence for the Classic-owned Library v3 modal/screen.
- Move Library v3 filtering/sorting from UI-local derived state into repository/query layer if the list grows beyond current in-memory summary scale.
- Verify text-level metadata editor with IME open and mixed Hebrew/Russian/Latin values.
- Add JSON credential attach/validate/delete UI and update provider settings implementation accordingly. Implemented in P015; manual SAF evidence remains pending.

## P014 Classic Mobile v4 UI Implementation Status

Implemented in commit `844239b`.

Implemented in this patch:

- Classic Mode main screen now follows the v4 mobile hierarchy more closely: `Лимиты и квоты`, `Classic Mode`, a visible `📚 Библиотека` entry point, source input, provider settings, generated row cards, save action, and source-level `🔊 Озвучить`.
- Library v3 is opened from Classic Mode as a modal/screen instead of a separate top-level mode.
- Library v3 includes header actions `Экспорт Библиотеки`, `Импорт Библиотеки`, `Обновить`, and `Закрыть`.
- Library v3 implements the required filter order: search, level, tags mode, search scope, sort, `Сохранить текущую таблицу`, tag chips, and `Сбросить`.
- Saved text cards include position, title, level/topic badges, source/source copy placeholder, timestamps, tag chips, and stacked `Открыть`, `Продолжить`, `Изменить`, `В архив`, `Удалить` actions so narrow phones do not clip actions.
- `Изменить` opens a text-level metadata editor for `TITLE*`, `LEVEL`, `TAGS`, `SOURCE`, and `TEMA`.
- Room schema version 2 adds nullable `library_texts.source_label` and `library_texts.topic`; export JSON includes these fields.
- `ClassicModeViewModel.openLibraryText` loads saved rows into Classic Mode and marks the text opened.
- Settings now displays the target JSON credential actions (`Прикрепить JSON`, `Проверить`, `Удалить ключ`) while keeping the previous single-line credential field explicitly marked as interim.

Still out of scope after P014:

- Manual emulator/device screenshots for Classic portrait, Library v3 filters/dropdowns/cards, metadata editor with keyboard, and Settings JSON credential target controls.
- Manual SAF JSON attachment evidence with real provider keys.
- Real SAF export/import UI wiring for Library header buttons.
- ClipboardManager integration for the source copy button.
- Compose UI tests for dropdown expansion, action-card clipping, and metadata validation.
- Reconciliation of the previous M8 row-level edit controls with the v4 Classic-owned Library flow; repository/ViewModel row editing remains covered, but UI evidence must confirm the final access path.

## P015 JSON Credential Provider Status

Implemented in this patch:

- Settings `Прикрепить JSON` opens Android Storage Access Framework for provider credential files and discards the external file path after reading.
- `ProviderSettingsRepository` validates and stores provider JSON in encrypted app-private settings.
- `gcp_translate` and `google_online_tts` accept Google service-account JSON and use OAuth JWT bearer requests for real Google API calls.
- `gemini_legacy` accepts the provider wrapper `{ "provider": "gemini_legacy", "api_key": "..." }`.
- Legacy single-line key input remains available only for smoke/backward compatibility.
- Unit tests cover JSON parser/repository/ViewModel behavior and provider bearer-header wiring.

Still out of scope after P015:

- Real endpoint smoke with the user's actual attached JSON keys on emulator/device.
- Network health-check button that calls each provider without generating user content.
- Playback/adoption UI for the Classic source-level TTS result.

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
