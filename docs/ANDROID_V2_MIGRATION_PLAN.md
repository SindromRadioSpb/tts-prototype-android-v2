# Android V2 Migration Plan

Date: 2026-04-25
Source repository: `E:\projects\tts-prototype-android`
Target repository: `E:\projects\tts-prototype-android-v2`

## Goal

Build a new native Android application that preserves the production behavior of the source TTS/translation prototype while intentionally excluding experimental browser-only IDE features from the stable Classic workflow.

The Android app must prioritize:

- Hebrew/Russian sentence table generation.
- Provider provenance and explicit degraded states.
- Library save/edit/reorder/import/export workflows.
- Audio ownership and export portability.
- Offline-first architecture where Android can reasonably own data locally.

## Confirmed Source Behavior

The behavioral inventory is captured in `docs/DISCOVERY_BEHAVIORAL_INVENTORY.md`.

Critical confirmed contracts:

- `server.js:2623` - premium table generation endpoint uses provider selection and defaults to `madlad`.
- `db/premium/pipeline.js:146` - provider result, niqqud result, transliteration, and source metadata are assembled into table rows.
- `server.js:1509` - generic TTS endpoint currently uses Google Cloud TTS and links generated audio into the v3 library cache.
- `server.js:1460` - Hebrew local sidecar TTS endpoint supports offline-ish local neural TTS.
- `server.js:3828-4243` - library list/save/update/get/edit/reorder/reset/delete/add endpoints define the core library behavior.
- `server.js:5393`, `server.js:5559`, `server.js:6907`, `server.js:6949` - DOCX, Anki CSV, full JSON export, and JSON import are existing export/import surfaces.
- `migrations/002_v3_library.sql`, `004_v3_audio_assets.sql`, `018_sentence_edits.sql` - source schema for texts, sentences, audio assets, sentence audio links, and edit metadata.

## Android Constraints

- `compileSdk`: 36, confirmed from local SDK platform `D:\Android\SDK\platforms\android-36.1`.
- `targetSdk`: 36.
- `minSdk`: 26. There was no existing Android config in the target repo, so this is a conservative v1 baseline for modern storage, networking, and Compose support.
- UI toolkit: Jetpack Compose.
- Gradle config: Kotlin DSL.
- Language: Kotlin.
- JVM target: 17.

## Scope

In scope for the initial v2 foundation:

- Native Android project skeleton.
- Classic mode UI shell that exposes the stable user workflow.
- Experimental IDE mode shell, visibly separated from Classic.
- Provider policy model with allowlist enforcement for Android v2.
- Library/export domain models that preserve source metadata.
- Documentation for discovery, provider policy, export layout, UI behavior, and staged migration.

Out of scope for the first checkpoint:

- Full Room database implementation.
- Real provider network implementations.
- Audio file generation and playback pipeline.
- DOCX/Anki writers.
- Migration from existing source SQLite DB into Android Room.
- Experimental IDE mode parity.

## Architecture Decision

The Android app starts with explicit domain contracts before provider implementations. This keeps provider identity, provenance, degraded states, and export ownership stable while later Room/network/audio work can be added behind these contracts without reshaping the UI or export format.

Source browser UI state is not ported directly. Classic mode is rebuilt as a native workflow, and IDE mode is kept as an isolated experimental surface.

## Patch Series

### PATCH-01: Android Foundation

Files created:

- `.gitignore`
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

Reasoning:

- Establish a buildable Compose application with no source repo coupling.
- Keep Gradle versions centralized and Kotlin DSL only.

Tests:

- `.\gradlew.bat test`
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat lint`

Commit:

- `chore(android): bootstrap v2 migration skeleton`

### PATCH-02: Domain Contracts and Provider Policy

Files created:

- `app/src/main/java/com/sindromradiospb/ttsprototypev2/core/model/LibraryModels.kt`
- `app/src/main/java/com/sindromradiospb/ttsprototypev2/core/provider/ProviderContracts.kt`
- `app/src/main/java/com/sindromradiospb/ttsprototypev2/core/export/ExportManifest.kt`

Reasoning:

- Preserve Android v2 provider policy before adding network code.
- Model table rows and export metadata with explicit provenance.
- Avoid unsupported providers in Android v2 by construction.

Tests:

- `app/src/test/java/com/sindromradiospb/ttsprototypev2/core/provider/ProviderPolicyTest.kt`

Commit:

- `feat(android): define v2 provider and export contracts`

### PATCH-03: Classic UI Shell and Documentation

Files created:

- `app/src/main/java/com/sindromradiospb/ttsprototypev2/MainActivity.kt`
- `app/src/main/java/com/sindromradiospb/ttsprototypev2/ui/AppRoot.kt`
- `app/src/main/java/com/sindromradiospb/ttsprototypev2/ui/theme/Theme.kt`
- `docs/DISCOVERY_BEHAVIORAL_INVENTORY.md`
- `docs/ANDROID_V2_PROVIDER_POLICY.md`
- `docs/ANDROID_V2_LIBRARY_EXPORT_SPEC.md`
- `docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md`
- `docs/ANDROID_V2_MIGRATION_PLAN.md`

Reasoning:

- Provide an executable native UI checkpoint before complex persistence/provider work.
- Document source behavior and Android boundaries while source evidence is fresh.

Tests:

- Compose app compile through `assembleDebug`.
- Unit tests for provider policy.

Commit:

- `docs(android): document v2 migration boundaries`

## Next Implementation Roadmap

### Milestone 1: Local Library Storage

- Add Room entities for texts, sentences, sentence edits, audio assets, sentence audio links, and export manifests.
- Add migrations from schema version 1 onward.
- Add repository methods matching source behavior:
  - save/update current table.
  - list text metadata.
  - load table with rows.
  - patch/reset/delete/add/reorder sentences.
- Add deterministic unit tests for ordering, edits, and reset behavior.

### Milestone 2: Provider Implementations

- Add `google_translate_free` and `gcp_translate` translation providers behind `TranslationProvider`.
- Add `google_online_tts` and Android `TextToSpeech` fallback providers behind `TtsProvider`.
- Persist provider provenance, latency, degraded state, and error category with each generated row/audio asset.
- Keep `MADLAD` and local sidecars out of Android v2 until explicitly re-approved; keep `Gemini Legacy` isolated and optional.

### Milestone 3: Export and Import

- Implement JSON export/import using `LibraryExportManifest`.
- Implement Anki CSV export.
- Implement DOCX export only after confirming an Android-safe DOCX writer dependency.
- Export generated audio into deterministic media folders and record missing audio explicitly.

### Milestone 4: Polish and Safety

- Add UI state restoration.
- Add progress/error surfaces for provider failures.
- Add network timeout/retry policy.
- Add accessibility labels and RTL checks.
- Add release signing documentation without storing secrets in the repository.

## Risk Mitigations

| Risk | Severity | Mitigation |
|------|----------|------------|
| Provider drift from source behavior | HIGH | Source behavior is documented with file/line evidence; Android contracts include provider provenance and allowlists. |
| Unsupported cloud/local sidecars on Android | HIGH | `MADLAD`, local Python sidecar, Phonikud/Piper, and Gemini are marked deferred or blocked until native feasibility is proven. |
| Loss of audio ownership during export | HIGH | Export manifest separates generated audio files, missing audio, and source metadata. |
| Library edit/reorder regressions | MEDIUM | Room milestone must reproduce source edit/reorder semantics and test order stability. |
| Experimental IDE mode destabilizes Classic | MEDIUM | IDE mode is isolated as a separate shell and out of scope for parity. |
| Secrets in Android repo | HIGH | No API keys are stored; provider implementations must read credentials from local/user configuration only. |

## Testing Strategy

Required before each implementation checkpoint:

- Unit tests for pure domain/provider policy behavior.
- Repository tests for Room once persistence is added.
- Instrumented tests for provider settings and library critical paths once UI actions are wired.
- `.\gradlew.bat test`
- `.\gradlew.bat assembleDebug`

## Rollback Notes

- PATCH-01 can be reverted without touching source repo state.
- PATCH-02 can be reverted independently while leaving the Android skeleton buildable.
- PATCH-03 UI/docs can be reverted without affecting provider contracts.
- The source repository is read-only for this migration and must not be modified.

## Definition of Done

- [x] Source behavior inventoried with file/line evidence.
- [x] Android provider policy documented.
- [x] Local library/export contract documented.
- [x] Native Android skeleton added.
- [x] Classic and IDE UI boundaries represented.
- [x] Gradle wrapper generated.
- [x] Unit tests passing: `.\gradlew.bat test`.
- [x] Debug APK build passing: `.\gradlew.bat assembleDebug`.
- [x] Android lint passing: `.\gradlew.bat lint`.
- [x] Git repository initialized with remote.
- [x] Initial commit created.
- [ ] Initial commit pushed if remote access is available.
