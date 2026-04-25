# Android v2 Provider Implementation Plan

Date: 2026-04-25

## Common Provider Interface

Providers must implement the existing contract direction in `core.provider`:

- typed request model;
- typed response model;
- `ProviderProvenance`;
- `ProviderErrorCategory`;
- `Result` or equivalent sealed result that never throws raw HTTP/platform exceptions into UI state.

Provider adapters return domain data only. They do not write Room directly and do not control fallback UI.

M3 implementation status:

- `TranslationProviderRegistry` rejects providers outside `AndroidV2ProviderPolicy`.
- `ProviderException` carries `ProviderErrorCategory`, provider ID, and user-visible message.
- Classic Mode consumes `TranslationProvider` through the registry and does not perform hidden fallback.

M4 implementation status:

- `TtsProviderRegistry` rejects TTS providers outside `AndroidV2ProviderPolicy`.
- `FakeTtsProvider` provides deterministic CI-safe TTS metadata without audio engine or network.
- `AndroidPlatformTtsProvider` wraps native Android `TextToSpeech` for `system_or_browser_fallback_low_quality`.
- `google_online_tts` has a keyed Google Cloud Text-to-Speech adapter that reads the M9 secure credential store, writes synthesized MP3 output to app cache, and returns domain metadata without persisting secrets.

P019 credential/provider status:

- `gcp_translate` is implemented as a Google Cloud Translation adapter: legacy single-line keys use Basic v2 JSON POST, while attached service-account JSON uses Cloud Translation v3 REST with OAuth JWT bearer auth.
- `gemini_legacy` is implemented as a Gemini `gemini-flash-latest` JSON POST adapter using a pasted single-line API key as the primary credential path, with the older provider-specific JSON wrapper still accepted for compatibility.
- `google_online_tts` is implemented as a Google Cloud Text-to-Speech `text:synthesize` JSON POST adapter using either a legacy stored single-line key or an attached service-account JSON credential with OAuth JWT bearer auth.
- Missing credentials still produce `MissingConfiguration` and do not fallback automatically.
- Raw service account JSON/private-key material remains blocked from manual single-line entry, but the SAF JSON attachment path validates and stores provider credentials in encrypted app-private storage.

P013 credential UI target:

- current single-line credential storage is an interim smoke path;
- production Settings UX must attach JSON through Android Storage Access Framework;
- provider adapters should continue to receive sanitized credential values from `ProviderSettingsRepository`, not file paths or raw UI state;
- validation should reject malformed JSON, wrong provider type, missing required fields, and private-key material that would be unsafe on-device.

P015 UI status:

- Settings exposes the target JSON credential controls (`Прикрепить JSON`, `Проверить`, `Удалить ключ`) beside provider status.
- `Прикрепить JSON` is enabled through Android Storage Access Framework and stores no external file path.
- `Проверить` performs local provider-specific schema validation; real network health is still verified by using the selected provider.
- Provider adapters receive credential material only from `ProviderSettingsRepository`.

## Allowed Translation Providers

| Provider ID | Purpose | Runtime notes | Production risk |
|-------------|---------|---------------|-----------------|
| `google_translate_free` | Best-effort no-key translation equivalent to current web `google-free`. | Must use timeout, visible degraded/unreliable label, fake provider tests. | Stability of unofficial endpoint is unresolved. |
| `gcp_translate` | Google Cloud Translation with explicit credentials. | Must surface quota, billing, invalid-key, unauthorized errors without fallback. | Device credential strategy unresolved. |
| `gemini_legacy` | Legacy online path for compatibility and optional experimentation. | Must be optional and never required for core Classic Mode. | Key handling and cost behavior unresolved. |

Current runtime wiring:

- `google_translate_free`: implemented as a source-prototype-compatible best-effort adapter using `translate.googleapis.com/translate_a/single`, `client=gtx`, legacy Hebrew source code `sl=iw`, browser User-Agent, and a newline batch request before individual fallback.
- `gcp_translate`: implemented as Google Cloud Translation adapter. It supports v3 service-account JSON bearer auth and legacy v2 restricted API keys, and maps HTTP failures into provider categories without fallback.
- `gemini_legacy`: implemented as keyed Gemini adapter for full Classic table generation. It asks Gemini for strict JSON with segments, Hebrew, niqqud, SBL transliteration, and Russian translation, sets JSON response mode, then maps rows into the Android table model without hidden fallback.

## Allowed TTS Providers

| Provider ID | Purpose | Runtime notes | Production risk |
|-------------|---------|---------------|-----------------|
| `google_online_tts` | Online Google TTS with local audio ownership after response is saved. | Writes returned audio to app storage before recording `audio_assets`. | Credential strategy and network failure UX. |
| `system_or_browser_fallback_low_quality` | Android platform TextToSpeech fallback. | Label in UI as low quality; no browser dependency in native Android. | Voice availability varies by device. |

Current runtime wiring:

- `google_online_tts`: implemented as Google Cloud Text-to-Speech adapter. It supports service-account JSON bearer auth and legacy restricted API keys, decodes returned MP3 bytes, and writes them to app cache before returning `TtsResponse`.
- `system_or_browser_fallback_low_quality`: implemented as Android platform `TextToSpeech.synthesizeToFile` adapter writing a temporary WAV under app cache. The provider uses Android system voice/rate/pitch defaults; if `TextToSpeech` does not initialize, Android v2 reports that the emulator/device has no enabled system TTS engine or language data.

## Disallowed Providers

Runtime support is blocked for `madlad`, `ai-local sidecar`, `HY-MT sidecar`, Railway routes, desktop localhost services, Python sidecars, sherpa-onnx Web/WASM, Hebrew Local Piper as required runtime, and noncommercial experimental Hebrew TTS as production default.

These may appear in docs only as historical context or migration rationale.

## Timeout and Retry Strategy

- Translation request timeout: 20 seconds per provider call.
- TTS request timeout: 45 seconds per synthesis call.
- Retry once only for transient network/server errors.
- Do not retry `Unauthorized`, `InvalidApiKey`, `QuotaExceeded`, or `BillingRequired`.
- No infinite loading; ViewModel must enforce visible completion state.

## No-Silent-Fallback Rule

Fallback is allowed only when:

- error category is transient;
- user selected fallback-capable mode;
- UI records requested provider and actual provider;
- provenance stores fallback reason.

Fallback is forbidden for quota, billing, invalid-key, and unauthorized errors.

## Provenance Persistence

Persist for every generated row or audio asset:

- requested provider ID;
- actual provider ID;
- model/voice when known;
- generated timestamp;
- from-cache flag;
- fallback reason if any;
- degraded flag for missing niqqud or low-quality fallback.

## Fake Providers

- `FakeTranslationProvider` returns deterministic Hebrew/Russian rows for CI and ViewModel tests.
- `FakeTtsProvider` returns deterministic test audio metadata without network or platform TTS.
- Contract tests must run in CI without real keys or network.

## Real Wiring Order

1. Fake providers and contract tests. Completed for translation in M3.
2. `google_translate_free` behind policy allowlist. Completed in M3 as best-effort HTTP adapter.
3. `gcp_translate` with secure key status checks. JSON/service-account adapter is implemented; manual real-key smoke remains required.
4. `system_or_browser_fallback_low_quality`. Completed in M4 as Android platform adapter; device smoke still required.
5. `google_online_tts`. JSON/service-account network adapter is implemented; manual real-key smoke remains required.
6. `gemini_legacy`. JSON-wrapper keyed network adapter is implemented; cost-warning UX remains a release-hardening item.

## Open Decisions

| Decision | Current default | Owner / next action |
|----------|-----------------|---------------------|
| GCP credential JSON on device | Implemented through SAF, encrypted storage, and OAuth JWT bearer auth. | Collect emulator/device evidence with the user's real service account. |
| Gemini key handling | Implemented as primary single-line API-key paste, with JSON wrapper retained only for compatibility. | Add cost-warning UX before release and run real-key smoke. |
| Google Online TTS credentials | Implemented through SAF, encrypted storage, and OAuth JWT bearer auth. | Collect emulator/device evidence with the user's real service account. |
| niqqud provider strategy | Store niqqud as optional/degraded; do not depend on desktop sidecar. | Decide in M3 after Android-safe options review. |
| Google Free production stability | Treat as best-effort with visible degraded/unofficial label. | Validate with provider tests and UX copy in M3. |

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
