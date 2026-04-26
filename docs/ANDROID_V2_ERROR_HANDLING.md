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
- Invalid Gemini API key: "Gemini API key is invalid. Paste a valid single-line API key in Settings. No fallback was used."
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

P015 credential UI status:

- Settings enables `Прикрепить JSON` through Android Storage Access Framework and `Проверить` for offline provider-specific schema validation.
- GCP Translate and Google Online TTS service-account JSON must contain `type=service_account`, `project_id`, `private_key`, and `client_email`.
- Gemini JSON wrapper must contain `api_key` and, if present, `provider=gemini_legacy`.
- The legacy single-line credential path still uses existing validation errors for blank, multiline, oversized, or private-key/service-account-shaped values.

P019 provider error status:

- Google Free HTTP 429 remains `QuotaExceeded` and must not be hidden by fallback.
- Google Free network failures remain `NetworkUnavailable`; batch fallback must not convert offline failures into blank rows.
- Gemini malformed, fenced, or prose-only responses that do not contain strict JSON table rows are `InvalidResponse`.
- Gemini credential errors use the single-line API-key validation path, while malformed JSON errors apply only to the optional backward-compatible wrapper.
- Gemini HTTP 400 must be mapped by Google's error body: messages mentioning invalid API key remain `InvalidApiKey`; malformed payload/model/request messages are `InvalidResponse` and should include the sanitized Google error message.

P020 provider error status:

- `system_or_browser_fallback_low_quality` `ProviderUnavailable` means Android framework `TextToSpeech` failed to initialize on the current emulator/device. Treat this as a device capability/setup problem, not as hidden fallback to another provider.
- System fallback `UnsupportedLanguage` means the installed Android TTS engine does not support the selected source language data.
- Translation-provider changes after generation intentionally clear saved-text identity and rebuild the table; failures from the newly selected provider remain visible and do not fall back to the previous provider.

P021 provider error status:

- Settings `Проверить` is local format validation, not a Gemini network health check.
- Gemini runtime failures include the sanitized Google error body so the UI can distinguish invalid keys from request/model/payload errors.

P022 provider error status:

- Gemini successful HTTP responses whose candidate text is malformed, not an object with `rows`, or otherwise unparsable must be `InvalidResponse`, never generic `Unknown`.
- Gemini parsing accepts both `{ "rows": [...] }` and a top-level row array, and accepts Russian translation under `ru`, `russian`, or `translation`.

P023 provider error status:

- Gemini parsing now attempts a best-effort row recovery from partially malformed candidate JSON before failing. This covers common model mistakes such as a missing comma between row objects while preserving visible `InvalidResponse` for unrecoverable output.
- Visible Classic/Settings error message cards must use selectable text so users can copy diagnostics with touch or mouse.

P024 provider error status:

- Gemini malformed outer HTTP envelopes, missing candidate text, malformed candidate JSON, and invalid `segments`/`rows` payloads must surface as `InvalidResponse`, not `Unknown`.
- Gemini table generation follows the source prototype prompt/parser contract. A valid response must contain row data recoverable from `rows` and may use `segments` as canonical Hebrew source text.

P025 provider error status:

- Gemini loose row recovery regex patterns must escape literal closing braces so Android runtime does not surface `PatternSyntaxException` as a provider failure.

P026 provider error status:

- Gemini loose row recovery must scope object extraction to the `rows` array when `segments` is present. Segment-only objects must not become visible table rows with only the Hebrew column populated.

P027 provider error status:

- Gemini parser must try full JSON repair and strict `segments` + `rows` parsing before loose row extraction. Common model mistakes such as adjacent row objects without commas should preserve niqqud/translit/translation data instead of degrading to Hebrew-only rows.

P028 provider error status:

- Gemini JSON repair must not use regex for adjacent object or trailing comma repair. Android runtime must not surface `PatternSyntaxException` from repair logic, and braces inside Hebrew/Russian text fields must remain data, not structural delimiters.
- Real Gemini endpoint smoke must use user-supplied credentials from local environment/settings only; API keys must not appear in committed files, logs, patch register entries, or command transcripts.

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
