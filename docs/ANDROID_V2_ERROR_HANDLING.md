# Android v2 Error Handling and Degraded States

Date: 2026-04-25

## Error Taxonomy

| Category | User-visible meaning | Retry |
|----------|----------------------|-------|
| `MissingConfiguration` | Provider is allowed but cannot run because required secure settings are absent. | No, configure provider or choose another manually. |
| `NetworkUnavailable` | Device is offline or cannot reach provider. | Yes, after network returns. |
| `Timeout` | Provider did not respond in time. | Yes, one manual retry. |
| `Unauthorized` | Provider rejected credentials or access. | No, fix settings. |
| `QuotaExceeded` | Provider quota is exhausted. | No automatic retry. |
| `BillingRequired` | Provider requires billing setup. | No automatic retry. |
| `InvalidApiKey` | Saved key is invalid. | No, update/delete key. |
| `InvalidCredentialFile` | Attached JSON credential file is malformed, wrong provider type, empty, or missing required fields. | No, attach a corrected JSON file. |
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
- Missing configuration: "GCP Translate is not configured. Add credentials in Settings or choose another provider. No fallback was used."
- Invalid credential file: "Credential JSON does not match gemini_legacy. Attach the correct JSON file. The secret was not stored."
- Quota: "GCP quota is exhausted. Try later or switch provider manually."
- Missing audio: "Audio file is missing. The row text is safe; regenerate audio to restore playback."
- Partial export: "Export completed with 2 missing audio files. See missing_audio.json in the ZIP."

## Retry Strategy

- Retry once automatically only for transient network/server failures where provider policy permits.
- No automatic retry for key, quota, billing, or unauthorized errors.
- No automatic retry for malformed credential JSON; user must attach a corrected file.
- No infinite loading; every operation resolves to success, degraded success, cancelled, or failed.

## Credential JSON Attachment Errors

Settings must distinguish:

- malformed JSON;
- empty file;
- wrong provider type;
- missing required field;
- rejected private-key/service-account material;
- health-check failed because network is unavailable;
- health-check failed because provider rejected credentials.

Secret values and external file paths must not appear in user-facing error details, logs, crash reports, exports, or screenshots intended for documentation.

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
