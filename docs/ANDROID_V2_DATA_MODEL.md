# Android v2 Data Model and Room Schema Spec

Date: 2026-04-25

This is the implementation spec and current Room baseline for M1.

## Implementation Status

Implemented in Android code:

- Room database: `AppDatabase`, schema version `1`.
- Entities: `LibraryTextEntity`, `LibraryRowEntity`, `AudioAssetEntity`, `RowAudioEntity`, `TextAudioEntity`, `ExportHistoryEntity`, `ProviderCallLogEntity`.
- DAO: `LibraryDao`.
- Repository: `RoomLibraryRepository`.
- Exported schema: `app/schemas/com.sindromradiospb.ttsprototypev2.data.db.AppDatabase/1.json`.

Not implemented yet:

- Export ZIP writer.
- Import compatibility layer.
- Provider event writes from real providers.
- Audio file cleanup for orphaned assets.

## Entity List

| Table | Stable ID | Purpose |
|-------|-----------|---------|
| `library_texts` | `text_id` string UUID | Saved source text plus table-level metadata. |
| `library_rows` | `row_id` string UUID | Ordered Hebrew/Russian study rows for one text. |
| `audio_assets` | `asset_key` deterministic string | Locally owned audio file metadata. |
| `row_audio` | composite `row_id`, `asset_key` | Row-level audio links and default selection. |
| `text_audio` | composite `text_id`, `asset_key` | Full-text audio links and default selection. |
| `export_history` | `export_id` string UUID | Export attempts, result path metadata, partial flag. |
| `provider_call_log` | `event_id` string UUID | Privacy-safe provider provenance and error history. |

Secrets are stored outside Room in Android Keystore-backed encrypted preferences. Room may store masked key status such as `configured=true`, provider ID, and last validation timestamp, but never raw key material.

## Fields

### `library_texts`

- `text_id TEXT PRIMARY KEY`
- `text_key TEXT NOT NULL UNIQUE`
- `title TEXT NOT NULL`
- `level TEXT NULL`
- `tags_json TEXT NOT NULL DEFAULT '[]'`
- `source_text TEXT NOT NULL`
- `source_meta_json TEXT NULL`
- `table_model_meta_json TEXT NULL`
- `tts_profile_json TEXT NULL`
- `is_archived INTEGER NOT NULL DEFAULT 0`
- `created_at TEXT NOT NULL`
- `updated_at TEXT NOT NULL`
- `last_opened_at TEXT NULL`
- `schema_version INTEGER NOT NULL DEFAULT 1`

Indexes: unique `text_key`; index `updated_at`; index `is_archived, updated_at`.

### `library_rows`

- `row_id TEXT PRIMARY KEY`
- `text_id TEXT NOT NULL REFERENCES library_texts(text_id) ON DELETE CASCADE`
- `order_index INTEGER NOT NULL`
- `hebrew_plain TEXT NOT NULL`
- `hebrew_niqqud TEXT NOT NULL DEFAULT ''`
- `translit TEXT NOT NULL DEFAULT ''`
- `translit_ru TEXT NOT NULL DEFAULT ''`
- `russian TEXT NOT NULL DEFAULT ''`
- `row_hash TEXT NULL`
- `edit_meta_json TEXT NULL`
- `source_meta_json TEXT NULL`
- `created_at TEXT NOT NULL`
- `updated_at TEXT NOT NULL`

Indexes: unique `text_id, order_index`; index `text_id`; index `row_hash`.

### `audio_assets`

- `asset_key TEXT PRIMARY KEY`
- `file_name TEXT NOT NULL`
- `relative_path TEXT NOT NULL`
- `mime_type TEXT NOT NULL`
- `provider_id TEXT NOT NULL`
- `voice_name TEXT NULL`
- `language TEXT NOT NULL`
- `duration_ms INTEGER NULL`
- `size_bytes INTEGER NULL`
- `content_hash TEXT NULL`
- `created_at TEXT NOT NULL`
- `provenance_json TEXT NOT NULL`
- `is_missing INTEGER NOT NULL DEFAULT 0`

Indexes: `provider_id, created_at`; `content_hash`.

### `row_audio`

- `row_id TEXT NOT NULL REFERENCES library_rows(row_id) ON DELETE CASCADE`
- `asset_key TEXT NOT NULL REFERENCES audio_assets(asset_key) ON DELETE CASCADE`
- `is_default INTEGER NOT NULL DEFAULT 0`
- `is_stale INTEGER NOT NULL DEFAULT 0`
- `stale_reason TEXT NULL`
- `created_at TEXT NOT NULL`

Primary key: `row_id, asset_key`.

### `text_audio`

- `text_id TEXT NOT NULL REFERENCES library_texts(text_id) ON DELETE CASCADE`
- `asset_key TEXT NOT NULL REFERENCES audio_assets(asset_key) ON DELETE CASCADE`
- `is_default INTEGER NOT NULL DEFAULT 0`
- `is_stale INTEGER NOT NULL DEFAULT 0`
- `stale_reason TEXT NULL`
- `created_at TEXT NOT NULL`

Primary key: `text_id, asset_key`.

### `export_history`

- `export_id TEXT PRIMARY KEY`
- `created_at TEXT NOT NULL`
- `destination_uri_redacted TEXT NULL`
- `text_count INTEGER NOT NULL`
- `row_count INTEGER NOT NULL`
- `audio_count INTEGER NOT NULL`
- `missing_audio_count INTEGER NOT NULL`
- `partial_backup INTEGER NOT NULL`
- `schema_version INTEGER NOT NULL`
- `status TEXT NOT NULL`
- `error_category TEXT NULL`

### `provider_call_log`

- `event_id TEXT PRIMARY KEY`
- `created_at TEXT NOT NULL`
- `provider_id TEXT NOT NULL`
- `operation TEXT NOT NULL`
- `status TEXT NOT NULL`
- `error_category TEXT NULL`
- `latency_ms INTEGER NULL`
- `model TEXT NULL`
- `quota_sensitive INTEGER NOT NULL DEFAULT 0`
- `request_summary_json TEXT NULL`
- `response_summary_json TEXT NULL`

No raw input text longer than the configured diagnostic limit may be stored in provider logs; no secrets are stored.

## M5 Audio Metadata Status

M5 writes row audio through existing schema version 1 tables; no Room migration is required.

- `audio_assets.relative_path` stores the app-internal relative path, never an absolute path.
- `audio_assets.asset_key` is accepted for file naming only when it matches `[A-Za-z0-9._-]+`; unsafe provider output is treated as an invalid response.
- `audio_assets.is_missing` is set only after playback/storage validation confirms the file is absent.
- `row_audio.is_default` is cleared for prior default links before a new default row audio link is inserted.
- `row_audio.is_stale` remains separate from missing-file state; stale means text/profile changed, missing means file validation failed.

Text-level audio remains planned and uses the existing `text_audio` table later.

## Transaction Boundaries

- Save new generated text: insert `library_texts` and all `library_rows` in one transaction.
- Update existing generated text: update `library_texts`, preserve stable row IDs where row hashes/order permit, insert/delete changed rows in one transaction.
- Edit row: update row fields and edit metadata, mark linked default audio stale if Hebrew or niqqud changes, in one transaction.
- Reorder rows: update all affected `order_index` values in one transaction.
- Delete text: cascade rows and audio links; audio files become orphan candidates but are not deleted until cleanup confirms no links remain.
- Export: read snapshot in one read transaction, then copy files outside the DB transaction using immutable snapshot data.

## Behavior Rules

- `text_key` handles duplicate text detection. Duplicate save returns a conflict state to UI unless user chooses update.
- `order_index` is positional and can change; `row_id` is stable.
- `edit_meta_json` stores original field values needed for reset.
- Editing `hebrew_plain` or `hebrew_niqqud` marks default row audio stale.
- Archive sets `is_archived=1`; it does not delete rows or audio.

## Source Mapping

| Source web schema | Android v2 target |
|-------------------|-------------------|
| `texts` | `library_texts` |
| `sentences` | `library_rows` |
| `audio_assets` | `audio_assets` |
| `sentence_audio` | `row_audio` |
| `text_audio` | `text_audio` |
| server export JSON | Android ZIP `library/library.json` plus manifest |

Excluded from first Android Room schema:

- Web navigation/history/SRS/notes/search tables.
- Server-only cache tables.
- Python sidecar state.
- Desktop/browser localStorage state.

## Schema Versioning

- Room schema version starts at `1`.
- Every schema change must add a migration test and update this document.
- Export schema has its own `export_schema_version`, starting at `1`.

## Related Docs

- [Local Library Plan](ANDROID_V2_LOCAL_LIBRARY_PLAN.md)
- [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md)
- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
