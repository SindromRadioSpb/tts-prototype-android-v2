# Android v2 Patch Register

Date: 2026-04-25

This register must be updated after every patch. It is operational, not archival: it tells the next implementer what changed, what tests were required, and which docs moved.

| Patch ID | Milestone | Status | Goal | Main files | Tests required | Docs required | Risks | Commit hash | Date | Notes |
|----------|-----------|--------|------|------------|----------------|---------------|-------|-------------|------|-------|
| P000 | M0 | Completed | Bootstrap Android v2 skeleton, provider/export contracts, Classic/IDE shell. | `app/`, Gradle files, initial docs | `.\gradlew.bat test`; `.\gradlew.bat assembleDebug`; `.\gradlew.bat lint`; `git diff --cached --check` | Initial migration docs | Foundation may drift without control-plane docs | `3800af6` | 2026-04-25 | Verified in target git log. |
| P001 | M0 | Completed | Update M0 checkpoint status after push. | `docs/ANDROID_V2_MIGRATION_PLAN.md` | `git diff --check` | Migration plan | Status drift if omitted | `fd819de` | 2026-04-25 | Verified in target git log. |
| P002 | Documentation control plane | Completed | Add premium migration documentation package and living-doc rules. | `docs/`, `docs/adr/` | `git diff --check`; `git status`; docs secret scan | All control-plane docs | Stale docs, disconnected requirements, unresolved provider/security decisions | `00d1d10` | 2026-04-25 | Status recorded by adjacent docs checkpoint commit. |
| P003 | M1 | Completed | Add Room local library storage, repository transactions, schema baseline, and M1 regression tests. | `app/build.gradle.kts`, `build.gradle.kts`, `app/src/main/java/.../data`, `app/src/test/java/.../data`, `app/schemas` | `.\gradlew.bat test`; `.\gradlew.bat assembleDebug`; `.\gradlew.bat lint`; `git diff --cached --check` | Migration plan, data model, local library plan, QA strategy, traceability, patch register | Room/KSP compatibility, repository invariants, stale audio behavior | `60dd079` | 2026-04-25 | M1 repository layer complete; M2 should wire Classic Mode state/UI. |
| P004 | M2 | Completed | Wire Classic Mode to ViewModel, fake generation shell, local Room save action, and library summaries. | `MainActivity.kt`, `TtsPrototypeApplication.kt`, `feature/classic`, `ui/AppRoot.kt`, `data/repository`, tests | `.\gradlew.bat test`; `.\gradlew.bat assembleDebug`; `.\gradlew.bat lint`; `git diff --check`; secret scan | Migration plan, Classic UI spec, QA strategy, traceability, UI evidence, risk register, settings/secrets | Fake output must stay visibly non-production; real provider keys must not enter repo | `479d50f` | 2026-04-25 | M2 complete; next implementation milestone is M3 translation providers. |
| P005 | M3 | Completed | Add translation provider registry, fake providers, `google_translate_free` HTTP adapter, missing-configuration adapters for keyed providers, and Classic provider error wiring. | `core/provider`, `data/provider/translation`, `feature/classic`, provider tests | `.\gradlew.bat test`; `.\gradlew.bat assembleDebug`; `.\gradlew.bat lint`; `git diff --check`; secret scan | Migration plan, provider plan/policy, error handling, QA strategy, traceability, risk register, Classic UI spec | `google_translate_free` is unofficial/best-effort; GCP/Gemini remain disabled until M9 secure settings | `bfa8613` | 2026-04-25 | M3 complete; next implementation milestone is M4 TTS/audio providers. |

## Update Rule

When a future patch lands, add one row with:

- exact commit hash;
- milestone;
- tests actually run;
- docs changed or reason docs did not need changes;
- risk/gap changes.

## Related Docs

- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Documentation Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)
