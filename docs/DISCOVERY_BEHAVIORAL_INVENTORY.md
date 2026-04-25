# Android v2 Discovery Behavioral Inventory

Date: 2026-04-25

Source repository: `E:\projects\tts-prototype-android`
Target repository: `E:\projects\tts-prototype-android-v2`

## Repo Audit Report

Task: migrate the current Hebrew/Russian web TTS/translation learning product into a local-first native Android v2 app.

### Entry Points

- `server.js:1509` - `POST /api/tts` - Google Cloud TTS path with legacy cache and v3 library audio asset linking.
- `server.js:1460` - `POST /api/tts/hebrew-local` - Hebrew local sidecar TTS path; reference only for Android v2.
- `server.js:2391` - `POST /api/translate-table` - Gemini legacy table generation.
- `server.js:2623` - `POST /api/translate-table-v2` - premium translation pipeline dispatch.
- `server.js:2792` - `POST /api/niqqud` - single text niqqud/transliteration endpoint.
- `server.js:3828` - `GET /api/library/texts` - library list.
- `server.js:3847` - `POST /api/library/texts` - atomic save of text + rows.
- `server.js:3973` - `PUT /api/library/texts/:id` - update saved text and rows.
- `server.js:4134` - `GET /api/library/texts/:id/sentences` - saved row read with transliteration enrichment.
- `server.js:4182`, `4197`, `4215`, `4229`, `4243` - row reorder, edit, reset, delete, add.
- `server.js:6907` - `GET /api/library/export` - whole library JSON export.
- `server.js:6949` - `POST /api/library/import` - JSON import with skip/asNew modes.
- `server.js:5393`, `5559` - per-text DOCX and Anki CSV export.
- `public/index.html:5551-5810` - Classic Mode input, TTS settings, translation provider selector, save/export controls and table settings.
- `public/index.html:19030` - browser Classic Mode `translateTable()` workflow.
- `public/index.html:12820` - browser `playTTS()` workflow.
- `public/index.html:16027` - browser save-to-library flow.
- `public/index.html:3717` - IDE Mode CSS/workspace region, feature-flagged via `body.v3-ide-mode`.

### Data Layer

- `texts` - source text, title, tags, source metadata, TTS profile, table model metadata, archive/open timestamps. Confirmed in `migrations/002_v3_library.sql:4-20`.
- `sentences` - stable row entity: `he_plain`, `he_niqqud`, `translit`, `ru`, `row_hash`, `meta_json`, `order_index`. Confirmed in `migrations/002_v3_library.sql:21-39`.
- `audio_assets` - deduped audio files with `asset_key`, `relative_path`, `mime`, `duration_ms`, `size_bytes`, `tts_profile_json`. Confirmed in `migrations/004_v3_audio_assets.sql:4-16`.
- `sentence_audio`, `text_audio` - default audio links for rows and full text. Confirmed in `migrations/004_v3_audio_assets.sql:18-35`.
- `sentences.translit_ru`, `sentences.edit_meta_json` - manual edit tracking. Confirmed in `migrations/018_sentence_edits.sql:1-14`.
- SQLite is opened with FK enforcement and WAL mode at `db/sqlite.js:95-99`.
- Server writes library text + rows in a transaction at `db/libraryRepo.js:157-240`; updates preserve existing sentence IDs where possible at `db/libraryRepo.js:242-410`.

### User-Facing Feature Inventory

| Feature | Current behavior | Android v2 status | Confidence |
|---|---|---|---|
| Classic Mode | Primary screen with source text, table generation, TTS settings, provider selector, save/export controls. Evidence: `public/index.html:5551-5810`. | Must be native primary workflow. | High |
| Hebrew input | Free text textarea, sample Hebrew default. Evidence: `public/index.html:5577`. | Native multiline Compose input, RTL-safe. | High |
| Table generation | `translateTable()` uses browser cache, then `/api/translate-table-v2` for `gcp`, `madlad`, `google-free`, or `/api/translate-table` for Gemini. Evidence: `public/index.html:19030-19115`. | Use allowed providers only: Google free, GCP, Gemini legacy. Exclude MADLAD runtime. | High |
| Row schema | Rows include Hebrew, niqqud, translit, translit_ru, Russian. Evidence: `db/premium/pipeline.js:299-310`. | Preserve as typed Kotlin row model. | High |
| Niqqud | Sidecar -> Dicta cloud -> degraded empty result. Evidence: `db/premium/niqqudGateway.js:5-21`, `62-97`. | Needs explicit Android decision; not in allowed provider list for production translation/TTS. Reference-only unless approved. | Medium |
| Transliteration | Computed locally from niqqud for SBL and Russian phonetic. Evidence: `db/premium/pipeline.js:273-287`. | Implement local deterministic Kotlin transliteration later; first skeleton stores fields. | High |
| Russian translation | Google free, GCP, Gemini legacy, plus disallowed MADLAD in old flow. Evidence: `db/premium/pipeline.js:35`, `95-145`; `server.js:2391-2602`. | Only `google_translate_free`, `gcp_translate`, `gemini_legacy`. | High |
| TTS playback | Main and row TTS use online Google TTS, sidecar Hebrew local, Web/WASM path, or browser system fallback. Evidence: `public/tts/settings.js:10-27`, `public/index.html:22517-22739`. | Only Google online TTS and Android system fallback low quality. | High |
| Audio persistence | Library-linked TTS stores `audio_assets` and returns `assetKey`. Evidence: `server.js:1619-1686`, `db/audioRepo.js:26-178`. | Must persist local files under app storage and export them. | High |
| Library | List/open/save/update/archive/delete texts; filters are partially in localStorage. Evidence: `public/index.html:13418-13446`, `15029-15098`, `16047-16248`. | Room/SQLite local-first. | High |
| Table editing | Row patch/reset/delete/add/reorder via endpoints. Evidence: `server.js:4170-4264`, `db/libraryRepo.js:1109`, `1113`, `1159`, `1196`, `1220`, `1266`. | Must support touch editing and preserve edit metadata. | High |
| Export | Whole library JSON, per-text DOCX and Anki CSV; current whole export does not embed audio files. Evidence: `server.js:6907-6941`, `5393-5554`, `5559-5600`. | ZIP bundle with JSON + audio files required. | High |
| IDE Mode | Experimental workspace, library/search/table/notes/audio/export panels. Evidence: `public/index.html:3717-3815`, `4512-4582`. | Include experimental shell, do not block Classic Mode. | High |
| Progress/dashboard | `progress`, recent rows/texts and dashboard state are server/localStorage mixed. Evidence: `public/index.html:13264-13416`. | Future local-first app state; not first critical path. | Medium |
| API-key flows | TTS key upload at `server.js:462`; GCP translate key handling at `server.js:2712-2720`. | Android Keystore + encrypted preferences; no export of keys. | High |

### External Integration Classification

| Integration | Current evidence | Android v2 classification |
|---|---|---|
| Google Translate Free | `db/premium/providers/googleFree.js:1-15`, `49-57`, `85-118` | Allowed online provider |
| GCP Translate | `db/premium/providers/gcp.js:3-18`, `97-133` | Allowed online provider |
| Gemini Legacy | `server.js:2391-2602` | Allowed legacy provider, isolated |
| Google Cloud TTS | `server.js:1509-1686` | Allowed online TTS |
| System/browser speech fallback | `public/index.html:12803-12818`, `22621-22629` | Allowed as Android system fallback, low quality |
| Railway routes | Mentioned by prompt; no Android runtime dependency allowed | Not allowed |
| MADLAD local sidecar | `db/premium/pipeline.js:53-74`, `server.js:2677-2684` | Not allowed runtime, reference only |
| ai-local FastAPI sidecar | `db/premium/pythonClient.js:8-10`, `46-57` | Not allowed runtime, reference only |
| Hebrew Local Piper sidecar | `server.js:1460-1507`, `public/index.html:22570-22618` | Not allowed runtime, reference only |
| sherpa-onnx Web WASM | `public/tts/providerPolicy.js:11-16` | Not allowed runtime, reference only |
| Browser localStorage/session behavior | `public/index.html:19040-19069`, `13436-13446` | Replace with local database/preferences |
| DOCX/Anki desktop exports | `server.js:5393-5600` | Reference only; Android v2 export is ZIP |

### Implicit Contracts

- Stable `textId` and `sentenceId` are mandatory; `order_index` is only positional. Evidence: `docs/DB_SCHEMA.md:48-64`.
- Tags are canonicalized to a JSON array and limited/deduped in `db/libraryRepo.js:71-119`.
- `text_key` dedupes saved library items; duplicate creates return `409` at `server.js:3961-3966`.
- Editing Hebrew text invalidates default row audio by clearing `sentence_audio.is_default`; see `db/libraryRepo.js:1147-1155`.
- GCP quota errors do not auto-fallback; transient GCP can retry/fallback in web pipeline. Evidence: `db/premium/providers/gcp.js:11-18`, `db/premium/pipeline.js:118-139`. Android v2 must not hide quota/billing/key errors.

### Edge Cases

- Hebrew RTL and niqqud readability on small screens: confirmed CSS attention at `public/index.html:1338-1348`, native UI must preserve this.
- Provider failure: google-free rate limit maps to 429 at `server.js:2646-2652`; GCP quota maps to 402 at `server.js:2660-2668`; sidecar missing maps to 503 at `server.js:2677-2684`.
- Network offline/timeouts: google-free provider marks `network` and `timeout` at `db/premium/providers/googleFree.js:40-45`; GCP classifies timeout/network as transient at `db/premium/providers/gcp.js:75-80`.
- Long texts: current TTS chunks around Google input limits in `server.js:524` and TTS request handling; Android v2 must chunk before online TTS.
- Export with missing audio: current exports include audio URLs/asset keys but not file copies. Android v2 ZIP must include missing-audio manifest entries.
- App restart during generation: current web jobs are in-memory and reset on server restart at `server.js:1830`. Android v2 must persist safe library state before/after long jobs.
- Android storage: no shared filesystem assumption; use app-specific files plus Storage Access Framework/share sheet for export.

### Risks And Unknowns

| Risk | Severity | Cause | Mitigation |
|---|---|---|---|
| Carrying disallowed sidecars into Android runtime | High | Current default premium provider can be `madlad` and niqqud sidecar chain is central. | Provider policy in code/docs permits only approved provider IDs. |
| Audio data loss during export | High | Current whole-library JSON export does not embed audio files. | ZIP export spec requires manifest + copied audio. |
| API key leakage | High | Web uploads service account JSON to server data dir. | Android v2 uses Keystore/encrypted preferences and excludes keys from export/logs. |
| Transliteration mismatch | Medium | Current transliteration depends on JS library/profile versions. | Add contract tests with captured fixtures before implementing Kotlin transliteration. |
| IDE Mode scope creep | Medium | Existing IDE has many panels. | Keep experimental shell; only shared models now. |
| Room schema drift | Medium | Current SQLite schema has many web-only premium tables. | Android schema starts with required library/audio/export subset only. |

### Targeted Clarifications

Implementation can proceed with these assumptions:

- `minSdk=26`, `targetSdk=36`, `compileSdk=36` because local SDK has `android-36.1`.
- Import compatibility is future work; export must be stable first.
- API keys are device-local and must not be included in ZIP exports.
- Niqqud service for Android v2 is not approved as a production provider yet; store niqqud when returned by allowed providers or future approved module.

Potential blocking questions for later provider implementation:

- Should GCP use service-account JSON import on device, or API-key only through a constrained backendless REST path?
- Is Gemini legacy expected to use a user-provided Gemini API key stored on device?
- Must Android v2 support import of old `linguist-pro-library` JSON in PATCH-01, or can it wait until after ZIP export is implemented?

### Related Docs

- [Documentation index](README.md)
- [Premium product target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md)
- [Architecture](ANDROID_V2_ARCHITECTURE.md)
- [Data model](ANDROID_V2_DATA_MODEL.md)
- [Provider policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Export spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md)
- [Risk and gap register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)
