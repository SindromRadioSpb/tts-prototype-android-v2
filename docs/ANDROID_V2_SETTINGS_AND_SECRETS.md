# Android v2 Settings and Secrets

Date: 2026-04-25

## Storage Rules

- API keys and credentials use Android Keystore-backed encrypted preferences.
- Raw secrets are not stored in Room.
- Raw secrets are not included in exports.
- Raw secrets are not logged.
- Raw secrets are not committed to Git.

## Provider Key Status

UI may store/show:

- provider ID;
- configured/not configured;
- masked suffix such as `...abcd`;
- last validation timestamp;
- last validation error category.

UI must not show full key, full service account JSON, or full credential file path.

## Key Operations

- Add/update key: overwrite encrypted value and update masked status.
- Delete key: remove encrypted value and provider status.
- Validate key: run provider-specific lightweight check with timeout.
- Export: always exclude keys and credential status unless status is needed as non-secret diagnostic metadata.

## Debug and Release

- Debug builds may use fake providers and fake keys.
- Release builds must not ship hardcoded keys.
- CI must not require real API keys.

## Manual QA Checklist

- [ ] Add key and verify masked status only.
- [ ] Delete key and verify provider becomes unconfigured.
- [ ] Trigger invalid-key provider error and verify no fallback.
- [ ] Export library and inspect JSON for absence of key material.
- [ ] Run secret scan before commit.

## Related Docs

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Release Readiness](ANDROID_V2_RELEASE_READINESS.md)
