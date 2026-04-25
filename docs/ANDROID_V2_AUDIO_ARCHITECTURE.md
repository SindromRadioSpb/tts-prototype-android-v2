# Android v2 Audio Architecture

Date: 2026-04-25

## Scope

Audio architecture covers row-level TTS, full-text TTS, local file ownership, playback, stale-state tracking, missing-audio handling, and export inclusion.

## Audio Types

- Row audio: speech for one `library_rows` item.
- Text audio: speech for the full `library_texts.source_text` or generated full table text.
- Fallback audio: Android platform TextToSpeech output, labeled low quality.

## Local Storage

- Generated or downloaded audio is not owned until copied into app-controlled storage.
- Target relative paths:
  - `audio/rows/{text_id}/{row_id}/{asset_key}.mp3`
  - `audio/texts/{text_id}/{asset_key}.mp3`
- `audio_assets.relative_path` stores app-internal relative path, not absolute external path.
- Export copies from app storage into ZIP paths, not from provider temp files.

## Deterministic Asset Key

Asset key format:

```text
sha256(provider_id + "|" + voice_name + "|" + language + "|" + normalized_text + "|" + profile_json)
```

The key prevents duplicate generation for the same provider/profile/text combination. It must not include API keys.

## Stale Audio Semantics

Row audio becomes stale when:

- `hebrew_plain` changes;
- `hebrew_niqqud` changes;
- row is reset to a different source text;
- TTS profile changes.

Text audio becomes stale when:

- source text changes;
- row order/content changes and text audio depends on generated rows;
- TTS profile changes.

Stale audio remains playable if file exists, but UI labels it stale and offers regeneration when provider is configured.

## Playback Behavior

- Only one audio item plays at a time.
- Playback state belongs to audio layer/ViewModel state, not Compose rows.
- Missing file produces `MissingAudio` error state and clears default link only after repository confirms the file is absent.

## Provider Output Handling

- `google_online_tts` output must be written to a temporary app file, validated for non-zero size and expected MIME type, then moved into final app storage.
- Android TextToSpeech fallback must be labeled `system_or_browser_fallback_low_quality` for compatibility with existing contract naming.

M4 implementation status:

- `FakeTtsProvider` returns deterministic metadata for tests and CI without creating real audio.
- `google_online_tts` is allowlisted but returns `MissingConfiguration` until M9 secure settings exists.
- `AndroidPlatformTtsProvider` uses Android `TextToSpeech.synthesizeToFile` and writes temporary WAV output under app cache.
- M5 must move/record accepted audio files into final app storage and Room `audio_assets`; M4 does not yet persist audio metadata.

M5 implementation status:

- `AudioStorageRepository` copies provider output from `TtsResponse.localFilePath` into app-owned storage.
- Row audio target path is `audio/rows/{text_id}/{row_id}/{asset_key}.{ext}` under app `filesDir`.
- `asset_key` must match `[A-Za-z0-9._-]+`; unsafe provider keys are rejected before any file copy to prevent path traversal through audio filenames.
- The adopted file is recorded in `audio_assets`; the row default is recorded in `row_audio`.
- Replacing row default audio first clears the previous `row_audio.is_default` flag for that row.
- Missing playback files are marked with `audio_assets.is_missing=true` only after `resolvePlayableAudio` checks the file path.
- `AndroidAudioPlaybackController` owns foreground playback via Android `MediaPlayer`; only one item is played by one controller instance at a time.

## Metadata

Store:

- provider ID;
- voice name;
- language;
- duration when available;
- size bytes;
- MIME type;
- content hash when available;
- generated timestamp;
- provenance JSON.

M4 `TtsResponse` carries `durationMs` and `sizeBytes` fields when available. Duration is usually unknown for Android platform TTS until playback/metadata inspection is added.
M5 also requires `TtsResponse.localFilePath` for adoption. This field is transient runtime metadata and must never be exported as an absolute path.

## Export Inclusion

Exports include existing audio files. Missing files are recorded in `metadata/missing_audio.json` and set `partial_backup=true`; missing one file does not fail the whole export.

## Privacy

Audio may contain user-entered Hebrew text. Export UI must warn that audio files are included and may contain personal study content.

## Related Docs

- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
