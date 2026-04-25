# Android v2 Risk and Gap Register

Date: 2026-04-25

| ID | Area | Risk / gap | Severity | Likelihood | Impact | Mitigation | Owner / next action | Status | Updated date |
|----|------|------------|----------|------------|--------|------------|---------------------|--------|--------------|
| RISK-001 | Security | GCP credential strategy on device is unresolved. | High | Medium | Key leakage or unusable provider. | Prefer restricted API key; avoid raw service account JSON; use encrypted settings. | Resolve in M9 before `gcp_translate`. | Open | 2026-04-25 |
| RISK-002 | Security | Gemini key strategy is unresolved. | High | Medium | Secret leakage or unexpected cost. | Store only encrypted, mask status, add explicit UI warning. | Resolve in M9 before `gemini_legacy`. | Open | 2026-04-25 |
| RISK-003 | Language | niqqud strategy is unresolved on Android without sidecars. | Medium | High | Rows may lack niqqud or show inconsistent quality. | Treat niqqud as optional/degraded; document provider before implementation. | Decide in M3. | Open | 2026-04-25 |
| RISK-004 | Language | Transliteration parity with source JS may drift. | Medium | Medium | Learners see inconsistent transliteration. | Capture fixtures from source behavior and test Kotlin implementation. | Add fixtures in M3/M13. | Open | 2026-04-25 |
| RISK-005 | Export | Audio export reliability may fail under missing files or SAF interruption. | High | Medium | User loses backup confidence. | Snapshot export, missing-audio manifest, partial backup flag. | Implement in M6. | Open | 2026-04-25 |
| RISK-006 | Import | Old web JSON import compatibility is not yet specified at field level. | Medium | Medium | Existing users cannot migrate data. | Build compatibility mapping and fixture tests. | Implement in M11. | Open | 2026-04-25 |
| RISK-007 | Android storage | SAF and scoped storage restrictions can break export paths. | High | Medium | Export cannot write where user expects. | Use SAF/create document and share sheet; avoid raw external paths. | Validate in M6. | Open | 2026-04-25 |
| RISK-008 | Provider UX | Quota/billing errors may be mistaken for transient failures. | High | Medium | Users retry uselessly or incur confusion. | Dedicated error categories and no silent fallback. | Validate in M3/M4. | Open | 2026-04-25 |
| RISK-009 | Product | IDE Mode scope creep can delay Classic Mode. | Medium | High | Production workflow slows down. | Keep IDE experimental and separate. | Review at M10 only. | Open | 2026-04-25 |
| RISK-010 | Process | Documentation can become stale after code patches. | High | Medium | Future agents implement against wrong contracts. | Maintenance policy, patch register, ADR rule. | Enforce every patch. | Active | 2026-04-25 |
| RISK-011 | Security | Secret leakage in logs, exports, screenshots, or Git. | High | Medium | User credentials exposed. | Secret scan, encrypted settings, export exclusion. | Enforce in M9 and release checks. | Open | 2026-04-25 |

## Related Docs

- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
