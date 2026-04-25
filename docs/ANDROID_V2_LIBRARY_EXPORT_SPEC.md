# Android v2 Library and Export Specification

Date: 2026-04-25

## Local Storage Model

Baseline: Room/SQLite on device, no cloud storage requirement.

Core tables:

- `library_texts`
  - `id`, `text_key`, `title`, `level`, `tags_json`, `source_text`, `source_meta_json`
  - `tts_profile_json`, `table_model_meta_json`
  - `is_archived`, `created_at`, `updated_at`, `last_opened_at`, `schema_version`
- `library_rows`
  - `id`, `text_id`, `order_index`
  - `he_plain`, `he_niqqud`, `translit`, `translit_ru`, `ru`
  - `row_hash`, `edit_meta_json`, `created_at`
- `audio_assets`
  - `id`, `asset_key`, `asset_type`, `relative_path`, `mime`, `duration_ms`, `size_bytes`
  - `tts_profile_json`, `provider_id`, `created_at`, `last_used_at`
- `row_audio`, `text_audio`
  - owner ID, audio ID, `is_default`

The schema intentionally mirrors the confirmed web subset in `migrations/002_v3_library.sql` and `migrations/004_v3_audio_assets.sql`, but excludes web-only dashboard/SRS/Anki/sidecar tables from the first Android runtime.

## Audio Storage

- Store generated audio under app-specific storage, for example `files/audio/<asset_key>.mp3`.
- `asset_key` is deterministic from text + normalized TTS profile + provider version.
- If a row is edited in Hebrew/niqqud fields, mark existing default audio stale.
- Audio playback reads from local file first, then provider if allowed and configured.

## Export Bundle

Preferred filename:

```text
library-export-YYYYMMDD-HHMM.zip
```

Structure:

```text
manifest.json
library.json
audio/
  item_<id>_<provider>.<mp3|wav>
metadata/
  app_version.json
  provider_provenance.json
```

## Manifest Schema

The Kotlin skeleton defines `LibraryExportManifest` in `core/export/ExportManifest.kt`.

Required fields:

- `exportType`: `tts-prototype-android-v2-library`
- `exportVersion`
- `appSchemaVersion`
- `exportedAt`
- `textCount`
- `rowCount`
- `audioFiles[]`
- `missingAudio[]`

Each audio file entry must include `assetKey`, relative path inside ZIP, MIME type, provider ID and available duration/size.

## Missing Audio Handling

- Do not fail the whole export when an audio file is missing.
- Add `missingAudio[]` manifest records with owner type, owner ID, expected `assetKey` and reason.
- Mark exported library items as partially backed up if any referenced audio is missing.

## Future Import Compatibility

- Import of Android v2 ZIP is a future extension unless cheap after export implementation.
- Import of old web `linguist-pro-library` JSON should be a separate compatibility patch with tests.
- No API keys are imported or exported.

## Privacy

- Export contains source texts, translations and audio; show explicit share/export confirmation.
- Do not include API keys, credential file names, device logs or provider raw error payloads.
