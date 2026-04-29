# Android v2 Library Export and Import Spec

Date: 2026-04-25

## Export ZIP Layout

```text
android-v2-library-export.zip
  manifest.json
  library/library.json
  audio/rows/{text_id}/{row_id}/{asset_key}.mp3
  audio/texts/{text_id}/{asset_key}.mp3
  metadata/missing_audio.json
  metadata/provider_events.json
```

## `manifest.json`

```json
{
  "export_schema_version": 1,
  "app_id": "com.sindromradiospb.ttsprototypev2",
  "created_at": "2026-04-25T00:00:00Z",
  "partial_backup": true,
  "text_count": 1,
  "row_count": 2,
  "audio_count": 1,
  "missing_audio_count": 1,
  "contains_secrets": false,
  "library_json_path": "library/library.json",
  "missing_audio_path": "metadata/missing_audio.json"
}
```

`contains_secrets` must always be `false`. Export must fail if a code path tries to include credentials.

## `library/library.json`

```json
{
  "schema_version": 1,
  "texts": [
    {
      "text_id": "txt_001",
      "text_key": "sha256:...",
      "title": "Psalm sample",
      "level": "A2",
      "tags": ["hebrew", "practice"],
      "source_label": "https://example.com/source",
      "topic": "lyrics",
      "source_text": "שלום עולם",
      "source_meta": {"origin": "manual"},
      "table_model_meta": {
        "provider": "gcp_translate",
        "actual_provider": "gcp_translate",
        "model": "google-cloud-translate",
        "from_cache": false,
        "niqqud_degraded": true,
        "generated_at": "2026-04-25T00:00:00Z"
      },
      "rows": [
        {
          "row_id": "row_001",
          "order_index": 0,
          "hebrew_plain": "שלום עולם",
          "hebrew_niqqud": "",
          "translit": "shalom olam",
          "translit_ru": "шалом олам",
          "russian": "мир",
          "edit_meta": null,
          "audio_asset_key": "asset_row_001"
        }
      ],
      "text_audio_asset_key": null,
      "created_at": "2026-04-25T00:00:00Z",
      "updated_at": "2026-04-25T00:00:00Z"
    }
  ],
  "audio_assets": [
    {
      "asset_key": "asset_row_001",
      "relative_export_path": "audio/rows/txt_001/row_001/asset_row_001.mp3",
      "mime_type": "audio/mpeg",
      "provider_id": "google_online_tts",
      "duration_ms": 1200,
      "size_bytes": 42000,
      "provenance": {
        "requested_provider_id": "google_online_tts",
        "actual_provider_id": "google_online_tts",
        "generated_at": "2026-04-25T00:00:00Z"
      }
    }
  ]
}
```

## Missing Audio Manifest

```json
{
  "missing_audio": [
    {
      "owner_type": "row",
      "text_id": "txt_001",
      "row_id": "row_002",
      "asset_key": "asset_missing",
      "reason": "file_missing_in_app_storage"
    }
  ]
}
```

Missing audio sets `partial_backup=true`. It does not fail export unless the user selected a strict mode in a future release.

## Export Transaction Strategy

1. Open read transaction and create immutable snapshot of texts, rows, audio metadata, and provider events.
2. Close DB transaction.
3. Copy files from app storage into ZIP.
4. Record missing files into `metadata/missing_audio.json`.
5. Write `manifest.json` last.
6. Record `export_history` result.

Interrupted export leaves no completed manifest in the destination. UI reports interrupted export and allows retry.

## M6 Implementation Status

M6 implements repository-level ZIP export through `LibraryZipExportRepository`.

Implemented:

- writes `manifest.json`;
- writes `library/library.json`;
- includes Library v3 text metadata fields `source_label` and `topic` when present;
- writes `metadata/missing_audio.json`;
- copies available row/text audio entries under their app-owned relative `audio/...` paths;
- writes `export_history` with text, row, audio, missing-audio, partial-backup, schema, status, and error metadata;
- rejects unsafe audio export paths before writing ZIP entries.

Current limits:

- SAF destination creation and share sheet UI are not wired yet;
- import remains future work;
- provider event export is still deferred until provider call logging is populated.

## P016 Legacy Web JSON Import Status

P016 implements the first compatibility importer for source prototype exports shaped like:

```json
{
  "exportType": "linguist-pro-library",
  "exportVersion": 1,
  "texts": [
    {
      "text": {
        "text_key": "...",
        "title": "...",
        "level": "alef+",
        "tags_json": "[\"song\"]",
        "source_text": "...",
        "source": "https://...",
        "topic": "lyrics"
      },
      "sentences": [
        {
          "order_index": 0,
          "he_plain": "...",
          "he_niqqud": "...",
          "translit": "...",
          "ru": "...",
          "audio_asset_key": "..."
        }
      ],
      "progress": null
    }
  ]
}
```

Implemented mapping:

- `texts[].text.text_key` is preserved and used for duplicate detection.
- `title`, `level`, `tags_json`/`tags`, `source_text`, `source`, `topic`, `created_at`, `updated_at`, `last_opened_at`, and `is_archived` map to `library_texts`.
- `sentences[]` maps to `library_rows` with Hebrew plain, niqqud, translit, Russian, row hash, and legacy `meta_json` stored as row source metadata.
- legacy `tts_profile_json`, `table_model_meta_json`, and `source_meta_json` are stored as raw JSON for export compatibility; domain decoding treats incompatible legacy provider IDs as optional metadata instead of crashing the UI.
- `audio_asset_key` links are imported as missing audio placeholders in `audio_assets`, `row_audio`, and `text_audio`, because the old JSON export does not include binary audio files.
- The UI `Импорт Библиотеки` button opens Android SAF for `.json` files and imports in safe `skip` mode.

Current limits:

- Old JSON import does not reconstruct actual audio files; subsequent Android ZIP export reports those links as missing audio.
- Progress is only partially represented through `last_opened_at`; Android v2 has no dedicated progress table yet.
- Android ZIP import is implemented separately by P029 for bundle files that contain `library/library.json` and flat `audio/{sha256}.mp3` entries.

## P029 Android ZIP Bundle Import Status

P029 wires Library v3 `Импорт ZIP (с аудио)` to Android SAF and `LibraryZipImportRepository`.

Implemented:

- reads ZIP through `ActivityResultContracts.OpenDocument`;
- requires `library/library.json`;
- accepts audio files under flat `audio/{sha256}.mp3` paths;
- stores imported audio under app-owned `filesDir/audio/{sha256}.mp3`;
- inserts `audio_assets` records for bundled audio;
- imports texts and rows into Room in `SKIP` duplicate mode;
- links row audio through `row_audio` when row `audio_asset_key` is present and bundled;
- links text-level audio through `text_audio` when `text_audio_asset_key` is present and bundled;
- accepts older exports where row `translit_ru` is missing by defaulting it to an empty string.

Current limits:

- nested Android export paths such as `audio/rows/{text_id}/{row_id}/{asset_key}.mp3` are not yet consumed by this importer;
- import result is surfaced as a summary message; detailed per-row import report UI remains future work;
- manual SAF import smoke with a real ZIP bundle is still required.

## P035 Imported Audio/Profile Provenance Requirements

Reference source bundle: `C:\Users\lletp\Downloads\library-bundle-top100maco-150verb-150pril.zip`.

Observed source-prototype bundle facts:

- ZIP layout is the flat web-compatible shape: `manifest.json`, `library/library.json`, `metadata/missing_audio.json`, and `audio/{sha256}.mp3`.
- `library/library.json.audio_assets[]` carries `voice_name`, `language`, `duration_ms`, `size_bytes`, `content_hash`, and `provenance.ttsProfile`.
- The observed bundle has one TTS profile across audio assets: `language=he-IL`, `voiceName=he-IL-Standard-A`, `speakingRate=0.9`, `pitch=2.5`.
- Some source-prototype `audio_assets[].provider_id` values may be `unknown`; Android must recover a user-facing profile from `provenance.ttsProfile` when possible instead of showing no profile.
- Some `texts[].table_model_meta` objects are missing Android-required `provider`; some contain legacy fields such as `promptId`, `model`, `cacheKey`, `fromCache`, and `generatedAt`.
- Some texts may have `table_model_meta=null`. Android must display this as unknown/legacy translation provenance, not as the currently selected translation provider.

Required Android behavior:

1. ZIP import must preserve audio provenance deeply enough to answer:
   - whether bundled row audio exists locally;
   - which TTS voice/profile produced it when the bundle exposes that data;
   - whether the active Classic TTS settings match or differ from the imported audio profile.
2. ZIP import must preserve table generation provenance deeply enough to answer:
   - requested/actual translation provider when present;
   - model/prompt/cache metadata when provider is absent;
   - explicit `Unknown / legacy bundle` state when provider cannot be recovered.
3. Opening a Library text must not silently overwrite imported provenance with current app settings.
4. Row playback must prefer bundled linked audio when it exists and is valid. It must not re-synthesize only because current settings differ.
5. If the user intentionally wants current settings, the UI must expose an explicit action such as `Пересинтезировать с текущими настройками`.
6. If the user wants to align the screen with the imported bundle, the UI should expose `Применить профиль карточки` when a complete compatible TTS profile is available.
7. Export after import must preserve linked audio files and provenance so a web -> Android -> web or Android round-trip does not erase the user's locally owned TTS cache.

Premium UI requirements:

- Library import result should report texts, rows, audio files imported, row audio linked, text audio linked, missing audio, and metadata warnings.
- Library card should show compact badges:
  - `Аудио: локально` / `Аудио: частично` / `Аудио: нет`;
  - `TTS: Online TTS · he-IL-Standard-A · 0.90x · pitch +2.5` when recoverable;
  - `Перевод: Google Translate`, `Gemini`, or `Перевод: неизвестно (legacy)` depending on real metadata.
- Classic `Таблица` header for a Library-loaded text should show:
  - saved `TITLE`;
  - clickable/copyable `SOURCE`;
  - `Перевод карточки`;
  - `Озвучка карточки`;
  - cache coverage, for example `2440/2440 строк озвучены локально`.
- Classic voice settings should distinguish two concepts:
  - `Текущие настройки озвучки` for future synthesis;
  - `Озвучка карточки` for already imported cached audio.
- If these differ, show a non-destructive notice: `Импортированная озвучка отличается от текущих настроек. Воспроизведение использует локальное аудио, пересинтезировать можно вручную.`

Implementation requirements:

- Add a tolerant legacy table-provenance domain model instead of decoding only strict `TableModelMeta`.
- Add a tolerant audio-profile parser that reads Android `TtsProfile`, web `provenance.ttsProfile`, and legacy `audio_tts_profile_json`.
- Expose default row audio metadata through repository/domain rows, not only `audioAssetKey`.
- Add an import fixture test using a web-compatible ZIP containing `audio_assets[].provenance.ttsProfile`.
- Add ViewModel tests for:
  - opening imported text shows recovered card TTS profile;
  - active settings mismatch does not block linked cache playback;
  - missing provider in legacy `table_model_meta` renders as unknown/legacy instead of current provider.
- Add manual evidence with `library-bundle-top100maco-150verb-150pril.zip`.

## Android Storage Behavior

- Use Android Storage Access Framework for user-selected destination.
- Use share sheet for "send export" workflow.
- Do not write raw external storage paths.
- Do not require cloud storage.

## Import Strategy

Import support now includes:

- Old web JSON import from `GET /api/library/export` shape with safe duplicate skipping.
- ZIP bundle import from `library/library.json` + flat `audio/{sha256}.mp3` shape with safe duplicate skipping and row/text audio linking.

Future import still needs:

- schema version compatibility checks beyond permissive `ignoreUnknownKeys`;
- nested Android ZIP audio path support if export and import are expected to round-trip the repository-level M6 layout exactly.

Old web import compatibility:

- `texts` maps to `library_texts`.
- `sentences` maps to `library_rows`.
- audio references are imported as missing placeholders because the legacy JSON has no audio bundle.
- unsupported web-only metadata is stored in raw JSON columns where available or ignored with an import report entry.

## Privacy Warning

Export UI must state: "This export includes library text and available audio files. It does not include API keys or provider credentials."

## Related Docs

- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
- [Release Readiness](ANDROID_V2_RELEASE_READINESS.md)
