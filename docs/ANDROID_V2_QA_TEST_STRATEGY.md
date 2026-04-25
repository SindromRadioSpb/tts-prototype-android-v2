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

## CI Expectations

- CI must not require real provider credentials.
- CI uses fake providers.
- Release readiness requires a clean lint report or documented accepted warnings.

## Related Docs

- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)
- [Release Readiness](ANDROID_V2_RELEASE_READINESS.md)
