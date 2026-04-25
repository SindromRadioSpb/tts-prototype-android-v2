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

## Allowed Translation Providers

| Provider ID | Purpose | Runtime notes | Production risk |
|-------------|---------|---------------|-----------------|
| `google_translate_free` | Best-effort no-key translation equivalent to current web `google-free`. | Must use timeout, visible degraded/unreliable label, fake provider tests. | Stability of unofficial endpoint is unresolved. |
| `gcp_translate` | Google Cloud Translation with explicit credentials. | Must surface quota, billing, invalid-key, unauthorized errors without fallback. | Device credential strategy unresolved. |
| `gemini_legacy` | Legacy online path for compatibility and optional experimentation. | Must be optional and never required for core Classic Mode. | Key handling and cost behavior unresolved. |

Current runtime wiring:

- `google_translate_free`: implemented as best-effort HTTP adapter using `translate.googleapis.com/translate_a/single`.
- `gcp_translate`: allowlisted but returns `MissingConfiguration` until M9 secure settings exist.
- `gemini_legacy`: allowlisted but returns `MissingConfiguration` until M9 secure settings and cost-warning UI exist.

## Allowed TTS Providers

| Provider ID | Purpose | Runtime notes | Production risk |
|-------------|---------|---------------|-----------------|
| `google_online_tts` | Online Google TTS with local audio ownership after response is saved. | Writes returned audio to app storage before recording `audio_assets`. | Credential strategy and network failure UX. |
| `system_or_browser_fallback_low_quality` | Android platform TextToSpeech fallback. | Label in UI as low quality; no browser dependency in native Android. | Voice availability varies by device. |

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
- Add fake TTS provider returning deterministic test audio metadata without network.
- Contract tests must run in CI without real keys or network.

## Real Wiring Order

1. Fake providers and contract tests. Completed for translation in M3.
2. `google_translate_free` behind policy allowlist. Completed in M3 as best-effort HTTP adapter.
3. `gcp_translate` with secure key status checks. Deferred to M9.
4. `system_or_browser_fallback_low_quality`.
5. `google_online_tts`.
6. `gemini_legacy` only after key strategy and UI cost warnings are documented.

## Open Decisions

| Decision | Current default | Owner / next action |
|----------|-----------------|---------------------|
| GCP key vs service account JSON on device | Prefer restricted API key or brokered OAuth; do not embed raw service account JSON on device. | Resolve in M9 before enabling `gcp_translate`. |
| Gemini key handling | Same encrypted settings path as GCP; masked status only. | Resolve in M9 before `gemini_legacy`. |
| niqqud provider strategy | Store niqqud as optional/degraded; do not depend on desktop sidecar. | Decide in M3 after Android-safe options review. |
| Google Free production stability | Treat as best-effort with visible degraded/unofficial label. | Validate with provider tests and UX copy in M3. |

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
