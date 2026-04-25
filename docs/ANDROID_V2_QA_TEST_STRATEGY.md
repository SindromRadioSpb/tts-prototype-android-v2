# Android v2 QA Test Strategy

Date: 2026-04-25

Testing validates behavior, not arbitrary coverage percentages.

## Mandatory Commands

Run for every code patch unless the patch is docs-only:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
git diff --cached --check
```

For docs-only patches, run:

```powershell
git diff --check
git status --short --branch
```

## Test Layers

- Unit tests: provider policy, domain model rules, asset key generation, error mapping.
- Room repository tests: save/load, duplicate handling, row edit/reset/reorder, stale audio, export snapshot.
- Provider contract tests: fake providers, timeout mapping, no-silent-fallback behavior.
- Integration tests: ViewModel plus fake repositories/providers.
- Compose UI tests: Classic Mode generation, row cards, editing sheet, library navigation.
- Instrumented tests: required when Android framework behavior is involved, such as TextToSpeech, Keystore, SAF export, Media playback, or IME/insets.
- Manual smoke tests: portrait/landscape, long Hebrew, keyboard, playback, export.
- Accessibility smoke tests: content descriptions, focus order, touch targets.

## Regression Scenarios

- Hebrew RTL remains readable after long input.
- Export succeeds with partial audio.
- Invalid key blocks fallback.
- Quota error is visible and not retried automatically.
- Editing Hebrew marks audio stale.
- Reorder preserves exactly one order position per row.
- App restart preserves library state.

## Current M1 Coverage

`RoomLibraryRepositoryTest` covers:

- save/load round trip;
- duplicate text conflict;
- update preserves row IDs by order;
- update preserves existing row audio links;
- load includes default row audio links;
- row patch edit metadata;
- Hebrew edit marks default row audio stale;
- reset selected row field;
- reorder exact-set validation and persisted order;
- delete compacts order;
- add row after selected row;
- archive hidden from default summaries;
- app restart safety through a file-backed Room database.

## Current M2 Coverage

`ClassicModeViewModelTest` covers:

- blank Hebrew source validation;
- fake row generation from multiline Hebrew input;
- selected translation/TTS provider state preservation;
- save action through the `LibraryRepository` port;
- local library summary propagation into UI state;
- duplicate save conflict message without creating a second saved text.

M2 does not yet include Compose UI tests or emulator evidence. Those become mandatory when row editing, playback, SAF export, IME/insets, or accessibility behavior changes.

## Current M3 Coverage

`TranslationProvidersTest` covers:

- deterministic fake translation provider rows;
- registry missing-provider behavior;
- missing-configuration provider without fallback;
- `google_translate_free` nested response parsing;
- HTTP quota mapping to `QuotaExceeded`;
- IO failure mapping to `NetworkUnavailable`.

`ClassicModeViewModelTest` now covers provider-backed generation and visible `MissingConfiguration` failure handling.

M3 tests do not call real network endpoints and do not require real provider credentials.

## Current M4 Coverage

`TtsProvidersTest` covers:

- deterministic fake TTS metadata;
- deterministic audio asset key stability;
- asset key changes when provider or text changes;
- missing-configuration TTS provider without fallback;
- registry missing-provider behavior.

M4 unit tests do not instantiate Android platform `TextToSpeech` and do not use Google TTS credentials. A device/emulator smoke test is still required before treating `system_or_browser_fallback_low_quality` as manually verified.

## CI Expectations

- CI must not require real provider credentials.
- CI uses fake providers.
- Release readiness requires a clean lint report or documented accepted warnings.

## Related Docs

- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)
- [Release Readiness](ANDROID_V2_RELEASE_READINESS.md)
