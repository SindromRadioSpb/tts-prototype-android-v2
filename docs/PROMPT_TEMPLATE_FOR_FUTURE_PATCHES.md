# Prompt Template for Future Android v2 Patches

Use this template for future Codex implementation tasks.

```text
Work in:
E:\projects\tts-prototype-android-v2

Current milestone:
[M1/M2/...]

Read first:
- docs/README.md
- docs/ANDROID_V2_MIGRATION_PLAN.md
- docs/DOCUMENTATION_MAINTENANCE_POLICY.md
- [milestone-specific docs]

Task:
[specific implementation request]

Rules:
- Use PowerShell-compatible commands only.
- Do not modify E:\projects\tts-prototype-android.
- Do not introduce Railway dependency.
- Do not introduce cloud app storage dependency.
- Do not introduce Node server, localhost desktop service, Python sidecar, browser-only API, or Web/WASM runtime dependency.
- Runtime providers must stay within docs/ANDROID_V2_PROVIDER_POLICY.md.
- Do not commit secrets.
- Do not wait for the developer to ask you to update docs.
- If your patch changes behavior, update the relevant docs immediately.

Required docs to update:
- docs/ANDROID_V2_PATCH_REGISTER.md
- docs/ANDROID_V2_MIGRATION_PLAN.md when milestone status changes
- docs/ANDROID_V2_RISK_AND_GAP_REGISTER.md when risks change
- [specific affected docs]

Tests/checks:
- .\gradlew.bat test
- .\gradlew.bat assembleDebug
- .\gradlew.bat lint
- git diff --cached --check

Patch output:
1. Plan / scope boundaries.
2. Files changed.
3. Tests run.
4. Docs updated.
5. Risks/gaps changed.
6. Commit message.
```

## Related Docs

- [Documentation Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md)
- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
