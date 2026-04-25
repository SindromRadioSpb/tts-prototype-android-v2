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

## Android Storage Behavior

- Use Android Storage Access Framework for user-selected destination.
- Use share sheet for "send export" workflow.
- Do not write raw external storage paths.
- Do not require cloud storage.

## Import Strategy

Future import supports:

- Android v2 ZIP import with schema version checks.
- Old web JSON import from `GET /api/library/export` shape where fields can be mapped.

Old web import compatibility:

- `texts` maps to `library_texts`.
- `sentences` maps to `library_rows`.
- audio references are imported as missing unless an audio bundle is present.
- unsupported web-only metadata is stored in `source_meta` or ignored with import report entry.

## Privacy Warning

Export UI must state: "This export includes library text and available audio files. It does not include API keys or provider credentials."

## Related Docs

- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
- [Release Readiness](ANDROID_V2_RELEASE_READINESS.md)
