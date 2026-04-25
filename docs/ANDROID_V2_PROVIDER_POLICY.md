# Android v2 Provider Policy

Date: 2026-04-25

## Allowed Translation Providers

- `google_translate_free` - online, no stored key, best-effort path equivalent to the current `google-free` provider.
- `gcp_translate` - online, user-configured Google Cloud Translate credentials/key, explicit quota/billing/key errors.
- `gemini_legacy` - online legacy provider, isolated behind the same provider interface and never required for core Classic Mode.

## Disallowed Translation Providers

- Railway-dependent translation routes.
- `madlad` local sidecar and all `ai-local` localhost translation flows.
- HY-MT/local desktop sidecars.
- Any desktop/server-only provider chain not listed above.

## Allowed TTS Providers

- `google_online_tts` - online Google TTS with configurable key/credentials and persisted local audio output.
- `system_or_browser_fallback_low_quality` - Android platform/system speech fallback, clearly labeled low quality.

## Disallowed TTS Providers

- Railway-dependent TTS.
- Hebrew Local Piper as a required runtime provider.
- sherpa-onnx Web/WASM runtime.
- localhost sidecar TTS.
- Desktop-only or noncommercial experimental Hebrew TTS as a production default.

## Required Provider Interface Rules

- Every request has a typed request model and explicit provider ID.
- Every response includes provenance: requested provider, actual provider, model/version if known, generated timestamp.
- Fallback must be visible. No silent provider switching.
- Quota, billing and invalid-key errors must not be hidden by automatic fallback.
- No provider may log API keys, service account JSON, bearer tokens or full credential paths.

## Error Categories

Android v2 provider implementations must map failures to:

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

## API-Key Handling

- Store keys only in Android Keystore-backed encrypted storage.
- Show masked summaries only.
- Allow update/delete.
- Exclude keys from library exports.
- Tests must use fake providers; no CI test may require real API keys.

## Code Guard

The initial app skeleton includes `AndroidV2ProviderPolicy` in `app/src/main/java/.../core/provider/ProviderContracts.kt`. Any new provider must be added there and in this policy before runtime wiring.
