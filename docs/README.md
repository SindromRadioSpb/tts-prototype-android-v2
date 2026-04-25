# Android v2 Documentation Control Plane

Date: 2026-04-25

This directory is the living control plane for the Android v2 migration. Future implementation work must update the affected documents in the same commit or an adjacent documentation commit. A code patch that changes behavior but leaves the related docs stale is incomplete.

## Start Here

1. Read [Premium Product Target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md) to understand what "premium" means for this app.
2. Read [Architecture](ANDROID_V2_ARCHITECTURE.md) for package boundaries and dependency direction.
3. Read [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md) for the M0-M14 roadmap and current checkpoint.
4. Read [Data Model](ANDROID_V2_DATA_MODEL.md), [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md), and [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md) before touching persistence or providers.
5. Read [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md), [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md), and [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md) before user-facing work.
6. Read [QA Strategy](ANDROID_V2_QA_TEST_STRATEGY.md), [Release Readiness](ANDROID_V2_RELEASE_READINESS.md), and [Documentation Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md) before closing a patch.

## Documentation Map

| Area | Documents | Use when |
|------|-----------|----------|
| Product | [Premium Product Target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md), [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md) | Defining scope, non-goals, acceptance criteria. |
| Architecture | [Architecture](ANDROID_V2_ARCHITECTURE.md), [ADRs](adr/README.md) | Changing packages, dependencies, state flow, storage, provider boundaries. |
| Providers | [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md), [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md), [Error Handling](ANDROID_V2_ERROR_HANDLING.md) | Adding or changing translation/TTS behavior. |
| Data | [Data Model](ANDROID_V2_DATA_MODEL.md), [Local Library Plan](ANDROID_V2_LOCAL_LIBRARY_PLAN.md), [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md) | Room, repository, import/export, audio metadata. |
| UI/UX | [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md), [IDE Mode Strategy](ANDROID_V2_IDE_MODE_EXPERIMENTAL_STRATEGY.md), [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md) | Compose screens, navigation, visual polish, manual evidence. |
| Audio | [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md), [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md) | TTS generation, playback, stale audio, export inclusion. |
| QA | [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md), [Requirements Traceability](ANDROID_V2_REQUIREMENTS_TRACEABILITY.md) | Test design, regression coverage, required commands. |
| Release | [Release Readiness](ANDROID_V2_RELEASE_READINESS.md), [Settings and Secrets](ANDROID_V2_SETTINGS_AND_SECRETS.md) | Signing, privacy, API keys, Play Store readiness. |
| Governance | [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md), [Patch Register](ANDROID_V2_PATCH_REGISTER.md), [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md), [Documentation Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md) | Status, risks, patch closure, doc update rules. |
| Prompts | [Migration Prompt](android_v2_migration_prompt.md), [Premium Documentation Prompt](android_v2_premium_documentation_package_prompt.md), [Future Patch Template](PROMPT_TEMPLATE_FOR_FUTURE_PATCHES.md) | Reconstructing task intent or starting a future patch. |

## Current Checkpoint

- M0 foundation is complete and pushed.
- The next implementation milestone is M1: Local Room library storage.
- Product code must not depend on Railway, cloud app storage, Node as required runtime, localhost sidecars, Python sidecars, browser APIs, Web/WASM assumptions, or desktop-only workflows.
- Runtime providers are allowlisted only: `google_translate_free`, `gcp_translate`, `gemini_legacy`, `google_online_tts`, `system_or_browser_fallback_low_quality`.

## Patch Closure Rule

Every future patch must answer:

- Which docs did this patch affect?
- Were they updated?
- Which tests/checks ran?
- Which risks or gaps changed?
- Does the patch require an ADR?

Required status docs after relevant patches:

- [Patch Register](ANDROID_V2_PATCH_REGISTER.md)
- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)
