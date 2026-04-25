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

M9 keyed adapter follow-up status:

- `gcp_translate` is implemented as a Google Cloud Translation Basic v2 JSON POST adapter using a stored single-line API key.
- `gemini_legacy` is implemented as a Gemini `gemini-2.0-flash` JSON POST adapter using a stored single-line API key.
- `google_online_tts` is implemented as a Google Cloud Text-to-Speech `text:synthesize` JSON POST adapter using a stored single-line API key.
- Missing credentials still produce `MissingConfiguration` and do not fallback automatically.
- Raw service account JSON and private-key material remain blocked by Settings validation and must not be embedded in the APK.

P013 credential UI target:

- current single-line credential storage is an interim smoke path;
- production Settings UX must attach JSON through Android Storage Access Framework;
- provider adapters should continue to receive sanitized credential values from `ProviderSettingsRepository`, not file paths or raw UI state;
- validation should reject malformed JSON, wrong provider type, missing required fields, and private-key material that would be unsafe on-device.

P014 UI status:

- Settings exposes the target JSON credential controls (`Прикрепить JSON`, `Проверить`, `Удалить ключ`) beside provider status.
- JSON attachment and health-check buttons are visibly disabled/pending; no provider adapter reads external file paths or raw JSON UI state.
- The existing adapters still receive sanitized values from `ProviderSettingsRepository`.
- The next provider/settings patch must add SAF file picking, provider-specific JSON parser tests, validation error mapping, and health-check wiring before enabling those buttons.

## Allowed Translation Providers

| Provider ID | Purpose | Runtime notes | Production risk |
|-------------|---------|---------------|-----------------|
| `google_translate_free` | Best-effort no-key translation equivalent to current web `google-free`. | Must use timeout, visible degraded/unreliable label, fake provider tests. | Stability of unofficial endpoint is unresolved. |
| `gcp_translate` | Google Cloud Translation with explicit credentials. | Must surface quota, billing, invalid-key, unauthorized errors without fallback. | Device credential strategy unresolved. |
| `gemini_legacy` | Legacy online path for compatibility and optional experimentation. | Must be optional and never required for core Classic Mode. | Key handling and cost behavior unresolved. |

Current runtime wiring:

- `google_translate_free`: implemented as best-effort HTTP adapter using `translate.googleapis.com/translate_a/single`.
- `gcp_translate`: implemented as keyed Google Cloud Translation Basic v2 adapter. It requires a stored restricted API key and maps HTTP failures into provider categories without fallback.
- `gemini_legacy`: implemented as keyed Gemini adapter for Hebrew-to-Russian translation. It requires a stored Gemini API key and maps HTTP failures into provider categories without fallback.

## Allowed TTS Providers

| Provider ID | Purpose | Runtime notes | Production risk |
|-------------|---------|---------------|-----------------|
| `google_online_tts` | Online Google TTS with local audio ownership after response is saved. | Writes returned audio to app storage before recording `audio_assets`. | Credential strategy and network failure UX. |
| `system_or_browser_fallback_low_quality` | Android platform TextToSpeech fallback. | Label in UI as low quality; no browser dependency in native Android. | Voice availability varies by device. |

Current runtime wiring:

- `google_online_tts`: implemented as keyed Google Cloud Text-to-Speech adapter. It requires a stored restricted API key, decodes returned MP3 bytes, and writes them to app cache before returning `TtsResponse`.
- `system_or_browser_fallback_low_quality`: implemented as Android platform `TextToSpeech.synthesizeToFile` adapter writing a temporary WAV under app cache.

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
3. `gcp_translate` with secure key status checks. Keyed network adapter is implemented; manual real-key smoke remains required.
4. `system_or_browser_fallback_low_quality`. Completed in M4 as Android platform adapter; device smoke still required.
5. `google_online_tts`. Keyed network adapter is implemented; service account JSON remains blocked and manual real-key smoke remains required.
6. `gemini_legacy`. Keyed network adapter is implemented; cost-warning UX remains a release-hardening item.

## Open Decisions

| Decision | Current default | Owner / next action |
|----------|-----------------|---------------------|
| GCP credential JSON on device | Target UX attaches provider JSON through SAF, validates it, and stores extracted safe fields encrypted. Service-account private keys remain blocked unless a future ADR approves brokered auth. | Define final JSON schema and implement attachment/validation UI. |
| Gemini key handling | Target UX uses provider-specific JSON wrapper, for example `{ "provider": "gemini_legacy", "api_key": "..." }`, unless an ADR supersedes it. | Add wrapper validation and cost-warning UX before release. |
| Google Online TTS credentials | Target UX attaches provider JSON through SAF; embedding service-account JSON in APK remains blocked. | Verify API-key TTS viability or design brokered auth before release. |
| niqqud provider strategy | Store niqqud as optional/degraded; do not depend on desktop sidecar. | Decide in M3 after Android-safe options review. |
| Google Free production stability | Treat as best-effort with visible degraded/unofficial label. | Validate with provider tests and UX copy in M3. |

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
