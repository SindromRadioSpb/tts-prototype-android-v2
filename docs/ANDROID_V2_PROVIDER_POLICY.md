# Android v2 Provider Policy

Date: 2026-04-25

This policy is binding for runtime code, docs, tests, UI labels, and future provider additions.

## Allowed Translation Provider IDs

- `google_translate_free`
- `gcp_translate`
- `gemini_legacy`

## Blocked Translation Provider IDs

- `madlad`
- `ai-local sidecar`
- `HY-MT sidecar`
- Railway routes
- desktop localhost translation services
- browser/server-only provider chains
- any provider not explicitly added to this policy and the code allowlist

## Allowed TTS Provider IDs

- `google_online_tts`
- `system_or_browser_fallback_low_quality`

For native Android, `system_or_browser_fallback_low_quality` means Android platform TextToSpeech fallback.

## Blocked TTS Provider IDs

- Railway-dependent TTS
- Hebrew Local Piper as required runtime
- sherpa-onnx Web/WASM runtime
- localhost sidecar TTS
- desktop-only TTS flows
- noncommercial experimental Hebrew TTS as production default

## Code Guard Requirements

- `AndroidV2ProviderPolicy` must reject any provider not listed above.
- Provider IDs must use stable wire IDs from `core.model`.
- UI must not show blocked providers as selectable runtime choices.
- Tests must assert the allowlist exactly.

## Provenance Requirements

Every provider response persisted or shown in UI must include:

- requested provider ID;
- actual provider ID;
- model or voice when known;
- generated timestamp;
- from-cache flag when relevant;
- degraded/fallback reason when relevant.

## Error Mapping Requirements

Providers must map failures to:

- `MissingConfiguration`
- `NetworkUnavailable`
- `Timeout`
- `Unauthorized`
- `QuotaExceeded`
- `BillingRequired`
- `InvalidApiKey`
- `ProviderUnavailable`
- `InvalidResponse`
- `UnsupportedLanguage`
- `Unknown`

Quota, billing, invalid-key, and unauthorized errors must not silently fallback.
Missing configuration must not silently fallback either; the UI must ask the user to configure the provider or choose a different provider manually.

## Security Requirements

- No API keys, service account JSON, bearer tokens, or full credential paths in logs.
- No secrets in Room.
- No secrets in exports.
- No secrets in Git.
- Provider tests use fake providers unless explicitly marked manual.

## Provider UI Display Requirements

- Display provider label, quality/degraded state, and fallback state where visible to user.
- `system_or_browser_fallback_low_quality` must be labeled low quality.
- `google_translate_free` must be labeled best effort/unofficial until production stability is proven.
- Provider errors must include an actionable message and never leave infinite loading.

## Provider Addition Process

Before adding a provider:

- [ ] Added to policy.
- [ ] Added to code allowlist.
- [ ] Added fake implementation.
- [ ] Added contract tests.
- [ ] Added user-visible error states.
- [ ] Added docs.
- [ ] Confirmed no Railway/localhost dependency.
- [ ] Confirmed no secret logging.

## Related Docs

- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md)
