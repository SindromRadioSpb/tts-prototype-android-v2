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
- Row notes save, trim, render after reload, and delete on blank note.
- Classic table column visibility cannot hide all columns and width resizing remains clamped.
- Auto-next playback starts from the selected row and stops visibly on user action or playback failure.

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

M2 does not include Compose UI tests or emulator evidence. Row editing now has M8 ViewModel coverage, but Compose screenshot/manual evidence remains mandatory before release hardening.

## Current P018 Coverage

`ClassicModeViewModelTest` covers:

- visible table columns cannot all be hidden;
- table column width resizing is clamped and reset restores defaults;
- auto-next start without rows shows an explicit disabled-flow message.

P018 does not yet include Compose UI tests for the rendered table or real device playback evidence for auto-next row advancement.

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

## Current M5 Coverage

`AudioStorageRepositoryTest` covers:

- adopting provider row audio into app-owned storage;
- inserting `audio_assets` metadata and default `row_audio` link;
- marking missing files after playback resolution checks;
- rejecting missing/empty provider output before adoption;
- rejecting unsafe provider asset keys before they can become file paths;
- returning playback failure for a missing file without starting `MediaPlayer`.

M5 tests do not play real audio on a device. Manual emulator/device playback evidence is still required before release.

## Current M6 Coverage

`LibraryZipExportRepositoryTest` covers:

- writing `manifest.json`, `library/library.json`, `metadata/missing_audio.json`, and available audio files into ZIP;
- `contains_secrets=false`;
- successful export history recording;
- missing audio producing `partial_backup=true` instead of export failure;
- unsafe audio relative paths being reported as missing and not written to ZIP.

M6 tests do not cover SAF/share-sheet UI because that UI is not implemented yet.

## Current M11 Legacy Import Coverage

`RoomLibraryRepositoryTest` covers:

- importing source-prototype `exportType=linguist-pro-library` JSON;
- mapping `text_key`, title, level, tags, source URL, topic, archive/date fields, and rows into Room;
- preserving Hebrew/Russian mixed text in imported row/card data;
- importing legacy row/text `audio_asset_key` values as missing audio placeholders;
- skipping duplicate imports by `text_key`.

P016 does not cover Android ZIP import yet. Manual SAF import evidence with a real exported JSON file is still required.

## Current M7 Coverage

`LibraryViewModelTest` covers:

- opening a saved text from the library and marking `last_opened_at`;
- keeping the selected archived text open so restore is available;
- toggling archived summaries;
- restoring and deleting selected texts.

`RoomLibraryRepositoryTest` covers hard delete removing the text from summaries and load paths.

M7 does not yet include Compose screenshot tests or device manual evidence.

## Current M8 Coverage

`LibraryViewModelTest` covers:

- opening a saved text and editing a row draft;
- saving only changed row fields through the repository mutation path;
- resetting edited row fields from `edit_meta`;
- explicit row move up through persisted order;
- deleting a row and compacting the selected text view;
- adding a row after an existing row;
- rejecting a new row with all fields blank.

M8 does not include Compose screenshot tests, drag-and-drop tests, or device manual evidence. Explicit button reorder is the supported UI for this milestone.

## Current M9 Coverage

`ProviderSettingsRepositoryTest` covers:

- saving a provider credential and exposing only masked status;
- deleting a provider credential;
- rejecting raw service account JSON/private-key material;
- rejecting multiline credentials.

`SettingsViewModelTest` covers:

- draft update and save flow;
- status refresh after save/delete;
- validation message when unsafe credential material is rejected.

M9 tests do not instantiate Android Keystore directly and do not use real provider credentials. Device/emulator evidence is still required to verify actual Android Keystore persistence and Settings screen layout before release.

## Current M9 Keyed Adapter Coverage

`TranslationProvidersTest` also covers:

- `gcp_translate` reading a stored credential and sending JSON POST to the Google Cloud Translation endpoint;
- `gcp_translate` missing-credential behavior without fallback;
- `gemini_legacy` reading a stored credential and parsing Gemini candidate text.

`TtsProvidersTest` also covers:

- `google_online_tts` reading a stored credential;
- decoding a base64 MP3 response;
- writing the synthesized audio file to app cache;
- missing-credential behavior without fallback.

`ClassicModeViewModelTest` also covers:

- Classic source-level `Speak` invoking the selected TTS provider;
- visible missing-configuration TTS failure without fallback.

These tests use stub HTTP clients and fake stores. They intentionally do not call real Google endpoints and do not require real provider credentials.

## Current P014 Classic Mobile v4 UI Coverage

`ClassicModeViewModelTest` covers:

- opening a saved library text into Classic Mode;
- preserving saved text ID/source text after open;
- visible message for the resume path when row progress metadata is not yet available.

`LibraryViewModelTest` covers:

- starting text-level metadata editing from a selected text or summary;
- saving `TITLE*`, `LEVEL`, comma/space-separated `TAGS`, `SOURCE`, and `TEMA`;
- rejecting blank metadata title;
- archive/delete lifecycle still works from the Library v3 card actions.

`RoomLibraryRepositoryTest` covers:

- persisting Library v3 text metadata fields `source_label` and `topic`;
- updating metadata without regenerating rows.

`AppDatabaseMigrationTest` covers:

- Room schema version 2 migration preserving existing v1 text rows and adding nullable metadata columns.

`LibraryZipExportRepositoryTest` covers:

- exporting `source_label` and `topic` in `library/library.json`.

P014 does not include Compose UI tests or emulator screenshots. Manual evidence remains required for the Classic main screen, Library v3 dropdowns, action-card clipping, metadata editor with IME open, and Settings JSON credential target controls.

## Current P017 Classic Main Screen and Row Actions Coverage

`ClassicModeViewModelTest` covers:

- metadata-first save through `Обновить`;
- updating the current saved card instead of creating duplicate rows;
- row note editor gating for unsaved rows and repository save/delete behavior through fakes.

`RoomLibraryRepositoryTest` covers:

- source-prototype `sentence_notes` semantics: trimmed markdown note, one note per text/row, note returned with loaded rows, blank note deletes.

`AppDatabaseMigrationTest` covers:

- Room schema version 3 migration creating `sentence_notes` with the unique `text_id, sentence_id` index.

P017 still requires manual emulator/device evidence for audible row playback, cache marker transitions, note editor keyboard behavior, and real-key provider synthesis.

## CI Expectations

- CI must not require real provider credentials.
- CI uses fake providers.
- CI must not require Android Keystore hardware-backed storage; production Keystore behavior is covered by manual/device validation.
- Release readiness requires a clean lint report or documented accepted warnings.

## Related Docs

- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)
- [Release Readiness](ANDROID_V2_RELEASE_READINESS.md)
