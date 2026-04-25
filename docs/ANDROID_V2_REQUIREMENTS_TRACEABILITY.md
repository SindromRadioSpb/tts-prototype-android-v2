# Android v2 Requirements Traceability

Date: 2026-04-25

| Requirement ID | Requirement | Source / rationale | Design doc | Implementation area | Test coverage | Status | Notes |
|----------------|-------------|--------------------|------------|---------------------|---------------|--------|-------|
| REQ-001 | Classic Mode is primary. | Product direction and premium workflow. | [Product Target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md), [Classic UI](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md) | `feature.classic`, `ui` | `ClassicModeViewModelTest`; future Compose UI tests | Partial | M3 wires Classic generation to translation provider registry; TTS remains M4/M5. |
| REQ-002 | IDE Mode is experimental. | Must not delay Classic Mode. | [IDE Strategy](ANDROID_V2_IDE_MODE_EXPERIMENTAL_STRATEGY.md), ADR-0004 | `feature.ide` | Navigation tests, no Classic regression | Partial | M0 shell exists. |
| REQ-003 | No Railway runtime dependency. | Android v2 non-negotiable. | [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md), ADR-0003 | Provider adapters, network config | Provider policy tests and dependency review | Partial | M3 translation providers use no Railway, localhost, sidecar, or cloud app storage runtime. |
| REQ-004 | No cloud app storage dependency. | Local-first ownership. | [Architecture](ANDROID_V2_ARCHITECTURE.md), [Data Model](ANDROID_V2_DATA_MODEL.md) | Room, app storage, export | Repository/export tests | Designed | Cloud provider APIs may be used only for translation/TTS, not app storage. |
| REQ-005 | Allowed translation providers only. | Runtime allowlist. | [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md) | `core.provider`, provider adapters | `ProviderPolicyTest`; `TranslationProvidersTest` | Partial | M3 registry enforces allowlist; GCP/Gemini remain missing-configuration until M9. |
| REQ-006 | Allowed TTS providers only. | Runtime allowlist. | [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md), [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md) | TTS adapters, audio layer | Provider allowlist and audio tests | Partial | M0 allowlist exists. |
| REQ-007 | Local library persists on device. | Premium data ownership. | [Data Model](ANDROID_V2_DATA_MODEL.md), [Local Library Plan](ANDROID_V2_LOCAL_LIBRARY_PLAN.md) | Room database and repository | `RoomLibraryRepositoryTest`; `ClassicModeViewModelTest` | Partial | M1 repository and schema are implemented; M2 saves generated rows from Classic Mode; M7 will add full lifecycle UI. |
| REQ-008 | Library export includes audio-aware metadata. | No silent audio loss. | [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md), ADR-0005 | Export layer, audio repository | Export ZIP tests | Designed | M6. |
| REQ-009 | API keys are not exported or logged. | Security and privacy. | [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md) | Encrypted settings, logging | Secret scan and security tests | Designed | M9. |
| REQ-010 | Hebrew RTL is readable. | Core learning UX. | [Classic UI](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md) | Compose text layout | UI evidence and accessibility smoke | Partial | M2 keeps right-aligned source and row Hebrew text; manual evidence is still pending. |
| REQ-011 | Long content scrolls. | Mobile usability. | [Classic UI](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md), [UI Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md) | Compose layout | Compose UI/manual tests | Partial | M0 shell uses scrolling; formal evidence pending. |
| REQ-012 | Export does not fail because one audio file is missing. | Ownership and backup reliability. | [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md), [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md) | Export snapshot writer | Missing-audio export tests | Designed | M6. |
| REQ-013 | Provider failures are visible. | No hidden provider switching. | [Error Handling](ANDROID_V2_ERROR_HANDLING.md), [Provider Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md) | ViewModel state, provider adapters | `TranslationProvidersTest`; `ClassicModeViewModelTest` | Partial | M3 surfaces missing configuration and mapped provider failures; TTS errors remain M4/M5. |
| REQ-014 | No silent fallback for quota, billing, invalid-key. | Prevent misleading output and cost surprises. | [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md), [Error Handling](ANDROID_V2_ERROR_HANDLING.md) | Provider adapters | `TranslationProvidersTest`; future provider contract tests | Partial | M3 maps quota and missing configuration without fallback; billing/invalid-key need keyed providers in M9. |
| REQ-015 | Documentation updates are mandatory. | Living control plane. | [Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md), ADR-0006 | Patch process | `git diff --check`, patch register review | Active | Applies immediately. |
| REQ-016 | Fake generation must not be mistaken for provider output. | M2 shipped before provider registry. | [Classic UI](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md), [Provider Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md) | `feature.classic`, `ui`, `data.provider.translation` | `ClassicModeViewModelTest`; `TranslationProvidersTest` | Partial | M3 replaces M2 fake shell with provider provenance; fake providers are test-only unless explicitly injected. |

## Related Docs

- [Premium Product Target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md)
- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
