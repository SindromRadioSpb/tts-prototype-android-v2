# Android v2 Risk and Gap Register

Date: 2026-04-25

| ID | Area | Risk / gap | Severity | Likelihood | Impact | Mitigation | Owner / next action | Status | Updated date |
|----|------|------------|----------|------------|--------|------------|---------------------|--------|--------------|
| RISK-001 | Security | GCP credential strategy on device is unresolved. | High | Medium | Key leakage or unusable provider. | Prefer restricted API key or brokered OAuth; do not embed raw service account JSON in the app; use encrypted settings. | Resolve in M9 before enabling `gcp_translate`. | Open | 2026-04-25 |
| RISK-002 | Security | Gemini key strategy is unresolved. | High | Medium | Secret leakage or unexpected cost. | Store only encrypted, mask status, add explicit UI warning. | Resolve in M9 before `gemini_legacy`. | Open | 2026-04-25 |
| RISK-003 | Language | niqqud strategy is unresolved on Android without sidecars. | Medium | High | Rows may lack niqqud or show inconsistent quality. | Treat niqqud as optional/degraded; document provider before implementation. | Decide in M3. | Open | 2026-04-25 |
| RISK-004 | Language | Transliteration parity with source JS may drift. | Medium | Medium | Learners see inconsistent transliteration. | Capture fixtures from source behavior and test Kotlin implementation. | Add fixtures in M3/M13. | Open | 2026-04-25 |
| RISK-005 | Export | Audio export reliability may fail under missing files or SAF interruption. | High | Medium | User loses backup confidence. | Snapshot export, missing-audio manifest, partial backup flag. | M6 repository export implemented; SAF interruption still needs UI/device validation. | Mitigated in repository; open for UI | 2026-04-25 |
| RISK-006 | Import | Old web JSON import compatibility is not yet specified at field level. | Medium | Medium | Existing users cannot migrate data. | Build compatibility mapping and fixture tests. | Implement in M11. | Open | 2026-04-25 |
| RISK-007 | Android storage | SAF and scoped storage restrictions can break export paths. | High | Medium | Export cannot write where user expects. | Use SAF/create document and share sheet; avoid raw external paths. | Repository ZIP writer exists; SAF UI validation moves to M7/M12. | Open | 2026-04-25 |
| RISK-008 | Provider UX | Quota/billing errors may be mistaken for transient failures. | High | Medium | Users retry uselessly or incur confusion. | Dedicated error categories and no silent fallback. | Quota mapping validated in M3; billing/invalid-key remain for M9 keyed providers. | Active | 2026-04-25 |
| RISK-009 | Product | IDE Mode scope creep can delay Classic Mode. | Medium | High | Production workflow slows down. | Keep IDE experimental and separate. | Review at M10 only. | Open | 2026-04-25 |
| RISK-010 | Process | Documentation can become stale after code patches. | High | Medium | Future agents implement against wrong contracts. | Maintenance policy, patch register, ADR rule. | Enforce every patch. | Active | 2026-04-25 |
| RISK-011 | Security | Secret leakage in logs, exports, screenshots, or Git. | High | Medium | User credentials exposed. | Secret scan, encrypted settings, export exclusion. | Enforce in M9 and release checks. | Open | 2026-04-25 |
| RISK-012 | Testing | Robolectric does not currently run unit tests at app target SDK 36. | Low | Medium | Local Room tests fail if default Robolectric SDK follows target SDK. | Pin repository tests to SDK 35 until Robolectric supports SDK 36 in this project. | Revisit during dependency updates. | Mitigated | 2026-04-25 |
| RISK-013 | Product | Test fake translation output could be exposed as production output. | High | Low | User may trust non-production language output. | Runtime registry uses `google_translate_free` plus missing-configuration providers; fake providers are injected only in tests. | Keep fake providers out of production registry unless explicitly gated. | Mitigated | 2026-04-25 |
| RISK-014 | Provider | `google_translate_free` uses an unofficial best-effort endpoint that can change without notice. | Medium | High | Translation may fail or parse incorrectly. | Map invalid responses visibly; keep GCP/Gemini as future configured alternatives; add manual smoke evidence before release. | Reassess in M13 and before release. | Active | 2026-04-25 |
| RISK-015 | Audio | Android platform TTS availability and Hebrew voice quality vary by device. | Medium | High | Fallback audio may be unavailable or poor quality. | Label as low quality, map unsupported language, require emulator/device smoke before release. | Validate in M5/M13 manual QA. | Active | 2026-04-25 |
| RISK-016 | Security | Google Online TTS service account JSON cannot be safely embedded in an Android APK. | High | High | Credential extraction from APK or Git leak. | Keep provider missing-configuration until M9 secure settings/credential strategy is implemented. | Resolve in M9 before enabling Google Online TTS. | Open | 2026-04-25 |
| RISK-017 | Audio | `MediaPlayer` playback is not yet manually verified on device/emulator. | Medium | Medium | Audio controls may fail despite unit-level storage checks. | Keep UI playback controls disabled until M7/M12 evidence; add manual smoke before release. | Validate during M7/M12/M13. | Open | 2026-04-25 |
| RISK-018 | UI | Library lifecycle UI is covered by ViewModel tests but not by Compose screenshot/manual device evidence. | Medium | Medium | Layout or touch issues may survive unit validation. | Keep actions simple, avoid destructive automation beyond explicit button press, add emulator evidence in M12/M13. | Capture UI evidence after navigation polish. | Open | 2026-04-25 |

## Related Docs

- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
