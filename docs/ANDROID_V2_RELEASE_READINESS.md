# Android v2 Release Readiness

Date: 2026-04-25

## Release Gates

- Debug and release build types are separated.
- Release signing plan documented; no keystore committed.
- Version name/code policy defined before first external build.
- R8/ProGuard rules reviewed after provider/audio/export dependencies are added.
- No blocked provider dependencies.
- No Railway, Node runtime, localhost sidecar, Python sidecar, browser API, or Web/WASM runtime dependency.
- Export/import smoke verified.
- Local library persistence verified after app restart.
- API keys excluded from logs, Room, export, and Git.

## Signing Plan

- Debug uses Android debug signing.
- Release keystore is stored outside Git.
- Keystore path and passwords are supplied through local machine configuration or CI secrets, never committed.

## Manual QA Checklist

- [ ] Install release candidate on Android phone.
- [ ] Generate Classic Mode rows with fake or configured provider.
- [ ] Save and reopen library item after app restart.
- [ ] Play/regenerate row and text audio where configured.
- [ ] Export ZIP and inspect manifest/library/missing audio.
- [ ] Verify invalid-key and quota messages.
- [ ] Verify Hebrew RTL, long text, keyboard, landscape.
- [ ] Verify no secrets in export.

## Known Non-Goals Before First Release Candidate

- Desktop sidecars.
- Railway runtime.
- WebView bridge.
- Cloud app storage.
- Unapproved providers.
- IDE Mode parity with web prototype.

## Release Blocker List

- Any secret in Git, logs, Room, or export.
- Any blocked provider in runtime dependencies.
- Export that silently omits audio without manifest entry.
- Provider quota/billing/invalid-key fallback.
- Data loss in save/update/edit/reorder.
- Classic Mode unreadable on phone portrait.

## Related Docs

- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
- [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)
