# Android v2 Patch Register

Date: 2026-04-25

This register must be updated after every patch. It is operational, not archival: it tells the next implementer what changed, what tests were required, and which docs moved.

| Patch ID | Milestone | Status | Goal | Main files | Tests required | Docs required | Risks | Commit hash | Date | Notes |
|----------|-----------|--------|------|------------|----------------|---------------|-------|-------------|------|-------|
| P000 | M0 | Completed | Bootstrap Android v2 skeleton, provider/export contracts, Classic/IDE shell. | `app/`, Gradle files, initial docs | `.\gradlew.bat test`; `.\gradlew.bat assembleDebug`; `.\gradlew.bat lint`; `git diff --cached --check` | Initial migration docs | Foundation may drift without control-plane docs | `3800af6` | 2026-04-25 | Verified in target git log. |
| P001 | M0 | Completed | Update M0 checkpoint status after push. | `docs/ANDROID_V2_MIGRATION_PLAN.md` | `git diff --check` | Migration plan | Status drift if omitted | `fd819de` | 2026-04-25 | Verified in target git log. |
| P002 | Documentation control plane | Completed | Add premium migration documentation package and living-doc rules. | `docs/`, `docs/adr/` | `git diff --check`; `git status`; docs secret scan | All control-plane docs | Stale docs, disconnected requirements, unresolved provider/security decisions | `00d1d10` | 2026-04-25 | Status recorded by adjacent docs checkpoint commit. |

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
