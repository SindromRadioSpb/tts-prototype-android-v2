# Android v2 Settings and Secrets

Date: 2026-04-25

## Storage Rules

- API keys and credentials use Android Keystore-backed encrypted preferences.
- Target UX for provider credentials is JSON file attachment and validation through Android Storage Access Framework, not manual raw text entry.
- Raw secrets are not stored in Room.
- Raw secrets are not included in exports.
- Raw secrets are not logged.
- Raw secrets are not committed to Git.

M9 implementation:

- `AndroidKeystoreSecureKeyValueStore` encrypts values with AES/GCM using a key generated in `AndroidKeyStore`.
- Encrypted values are stored in app-private `SharedPreferences`, not Room.
- `ProviderSettingsRepository` exposes only configured/missing status and masked values to UI.
- `SettingsViewModel` never logs or exports raw values.

## Provider Key Status

UI may store/show:

- provider ID;
- configured/not configured;
- masked suffix such as `...abcd`;
- last validation timestamp;
- last validation error category.

UI must not show full key, full service account JSON, or full credential file path.

Current M9 status values:

- `gcp_translate`: currently configurable as a single-line restricted key and used by the keyed adapter. Target UI must change to JSON attachment/validation.
- `gemini_legacy`: currently configurable as a single-line API key and used by the keyed adapter. Target UI must change to provider-specific JSON wrapper unless an ADR supersedes it.
- `google_online_tts`: currently configurable as a single-line credential and used by the keyed adapter. Target UI must change to JSON attachment/validation; raw service account JSON/private keys still must not be embedded in the APK.

## Target JSON Credential Attachment Flow

Provider credentials must be supplied through Android file picker / Storage Access Framework.

```text
Settings
  -> Provider credentials
  -> Select provider
  -> Attach JSON file
  -> Validate
  -> Store securely
  -> Show masked status
```

The Settings screen must present:

- provider name;
- credential status: `not configured`, `attached but not validated`, `valid`, `invalid`, `expired/revoked if known`;
- `Прикрепить JSON`;
- `Проверить`;
- `Удалить ключ`.

Manual raw text API-key fields are interim and must not remain the primary UX.

Validation requirements:

- parse JSON;
- reject malformed or empty JSON;
- reject wrong provider type;
- validate required fields for selected provider;
- mask summary in UI;
- optionally run provider health check if network is available;
- never log secret fields or full file path;
- never store raw external file path as source of truth.

Proposed Gemini wrapper until source parity or ADR says otherwise:

```json
{
  "provider": "gemini_legacy",
  "api_key": "..."
}
```

## Key Operations

- Add/update key: overwrite encrypted value and update masked status.
- Attach JSON: read through SAF, validate, extract/store secret fields, and discard external path.
- Delete key: remove encrypted value and provider status.
- Validate key: run provider-specific lightweight check with timeout.
- Export: always exclude keys and credential status unless status is needed as non-secret diagnostic metadata.

M9 validation rules:

- blank values are rejected;
- multiline values are rejected;
- values longer than 4096 characters are rejected;
- strings shaped like Google service account private-key JSON or PEM private-key material are rejected.

## Debug and Release

- Debug builds may use fake providers and fake keys.
- Release builds must not ship hardcoded keys.
- CI must not require real API keys.
- Local service account JSON files may be used only as manual QA inputs during M3/M4/M9 work. They must stay outside the repository, must not be copied into `app/src`, and must not be embedded in `BuildConfig`, resources, logs, screenshots, or exports.

## Manual QA Checklist

- [x] Unit test: add key and verify masked status only.
- [x] Unit test: delete key and verify provider becomes unconfigured.
- [x] Unit test: reject raw service account JSON/private key material.
- [ ] Trigger invalid-key provider error and verify no fallback.
- [ ] Attach JSON credential through SAF and verify status changes without exposing path or secret.
- [ ] Export library and inspect JSON for absence of key material.
- [ ] Run secret scan before commit.

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Release Readiness](ANDROID_V2_RELEASE_READINESS.md)
- [Classic Mode Mobile v4 UI Spec](ui/ANDROID_V2_CLASSIC_MODE_MOBILE_V4_UI_SPEC.md)
