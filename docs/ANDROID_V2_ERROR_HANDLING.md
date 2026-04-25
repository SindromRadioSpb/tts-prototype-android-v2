# Android v2 Error Handling and Degraded States

Date: 2026-04-25

## Error Taxonomy

| Category | User-visible meaning | Retry |
|----------|----------------------|-------|
| `NetworkUnavailable` | Device is offline or cannot reach provider. | Yes, after network returns. |
| `Timeout` | Provider did not respond in time. | Yes, one manual retry. |
| `Unauthorized` | Provider rejected credentials or access. | No, fix settings. |
| `QuotaExceeded` | Provider quota is exhausted. | No automatic retry. |
| `BillingRequired` | Provider requires billing setup. | No automatic retry. |
| `InvalidApiKey` | Saved key is invalid. | No, update/delete key. |
| `ProviderUnavailable` | Provider service failed. | Manual retry later. |
| `InvalidResponse` | Provider returned unusable data. | Manual retry or change provider. |
| `UnsupportedLanguage` | Provider cannot handle requested language. | Change provider/input. |
| `MissingAudio` | DB references audio that is absent on disk. | Regenerate audio. |
| `ExportFailed` | Export could not complete. | Retry after fixing destination/storage. |
| `DatabaseFailure` | Local storage failed. | Retry; preserve visible state. |
| `Unknown` | Unexpected failure. | Manual retry; include diagnostic ID. |

## Degraded States

- `niqqud_missing`: Hebrew niqqud unavailable.
- `low_quality_tts`: Android platform fallback used.
- `fallback_provider_used`: requested and actual provider differ.
- `stale_audio`: audio exists but row/text changed.
- `partial_export`: export succeeded with missing audio.

## User-Facing Message Examples

- Invalid key: "Google Cloud key is invalid. Update the key in Settings. No fallback was used."
- Quota: "GCP quota is exhausted. Try later or switch provider manually."
- Missing audio: "Audio file is missing. The row text is safe; regenerate audio to restore playback."
- Partial export: "Export completed with 2 missing audio files. See missing_audio.json in the ZIP."

## Retry Strategy

- Retry once automatically only for transient network/server failures where provider policy permits.
- No automatic retry for key, quota, billing, or unauthorized errors.
- No infinite loading; every operation resolves to success, degraded success, cancelled, or failed.

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
