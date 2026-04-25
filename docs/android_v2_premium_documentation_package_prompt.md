# PROMPT FOR CODEX — Android v2 Premium Migration Documentation Package

You are operating as a senior software architect, Android product engineer, technical writer, and delivery lead.

Your task is NOT to implement Android product code yet.

Your task is to prepare a complete, production-grade documentation package that will be used to guide the migration of the current `tts-prototype-android` web product into a native Android v2 application until it reaches the level of a premium product.

The documentation package must become the project’s living control plane.

It must not be static paperwork.

It must be updated continuously and proactively during future development without waiting for manual reminders from the developer.

---

# 0. Current Project Context

Source web repository:

```text
E:\projects\tts-prototype-android
```

Source remote:

```text
https://github.com/SindromRadioSpb/tts-prototype-android
```

Target Android v2 repository:

```text
E:\projects\tts-prototype-android-v2
```

Target remote:

```text
https://github.com/SindromRadioSpb/tts-prototype-android-v2
```

The Android v2 repository has already been initialized and pushed.

Current Android v2 state:

- Native Android project created.
- Kotlin + Jetpack Compose selected.
- Gradle Wrapper `8.14.3` connected.
- `compileSdk=36`
- `targetSdk=36`
- `minSdk=26`
- Classic Mode UI shell exists.
- Experimental IDE Mode shell exists.
- Initial domain contracts exist:
  - library rows/texts;
  - provider policy;
  - export manifest.
- Initial documentation exists:
  - `docs/DISCOVERY_BEHAVIORAL_INVENTORY.md`
  - `docs/ANDROID_V2_PROVIDER_POLICY.md`
  - `docs/ANDROID_V2_LIBRARY_EXPORT_SPEC.md`
  - `docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md`
  - `docs/ANDROID_V2_MIGRATION_PLAN.md`

Tests/build already passed at the first checkpoint:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
git diff --cached --check
```

The source repo must not be modified.

The target repo is the working repo for Android v2.

---

# 1. Product Direction

The project is migrating from a web application to a native Android application.

The Android app must become a premium mobile product.

The app must include:

## 1.1 Classic Mode

Classic Mode is the primary production mode.

It is the core product workflow.

It must eventually support:

- Hebrew text input;
- table/card generation;
- Hebrew original;
- Hebrew with niqqud where available;
- SBL Academic transliteration;
- Russian phonetic transliteration where available;
- Russian translation;
- TTS/audio generation and playback;
- saving results into a local library;
- editing generated rows;
- row-level and text-level audio metadata;
- export of library data together with audio;
- mobile-first UI suitable for Android phones.

## 1.2 IDE Mode

IDE Mode must be included as an experimental mode.

It is not the main product workflow.

It must:

- remain separate from Classic Mode;
- be clearly marked as experimental;
- not delay Classic Mode;
- not destabilize shared app architecture;
- be documented as future-evolving functionality.

---

# 2. Android v2 Non-Negotiable Constraints

Android v2 must NOT depend on:

- Railway;
- cloud app storage;
- Node server as required runtime;
- localhost desktop services;
- Python sidecars;
- browser-only APIs;
- Web/WASM runtime assumptions;
- desktop-only workflows.

Android v2 must be:

- local-first;
- mobile-first;
- export-capable;
- user-data-safe;
- provider-policy-controlled;
- testable;
- maintainable by a small team / solo developer.

---

# 3. Allowed Provider Policy

The Android v2 provider policy is strict.

## 3.1 Allowed translation providers

Only these translation providers are allowed in Android v2 runtime:

```text
google_translate_free
gcp_translate
gemini_legacy
```

## 3.2 Disallowed translation providers

Do not include runtime support for:

```text
madlad
ai-local sidecar
HY-MT sidecar
Railway routes
desktop localhost translation services
browser/server-only provider chains
any provider not explicitly approved
```

## 3.3 Allowed TTS providers

Only these TTS providers are allowed in Android v2 runtime:

```text
google_online_tts
system_or_browser_fallback_low_quality
```

For native Android, interpret `system_or_browser_fallback_low_quality` as Android platform/system TextToSpeech fallback.

## 3.4 Disallowed TTS providers

Do not include runtime support for:

```text
Railway-dependent TTS
Hebrew Local Piper as required runtime
sherpa-onnx Web/WASM runtime
localhost sidecar TTS
desktop-only TTS flows
noncommercial experimental Hebrew TTS as production default
```

Disallowed providers may be documented as reference-only historical context, but must not become runtime dependencies.

---

# 4. Main Objective Of This Task

Create a full documentation package that will guide implementation of Android v2 from the current skeleton to a premium Android product.

This package must cover:

- architecture;
- domain model;
- provider implementation;
- local library;
- audio storage;
- export/import;
- Classic Mode UX;
- IDE Mode experimental strategy;
- data safety;
- security and API keys;
- quality and testing;
- observability;
- release readiness;
- premium UX standards;
- implementation roadmap;
- patch register;
- decision register;
- documentation maintenance policy.

The output must be committed to the Android v2 repo.

Do not implement product code in this task unless a tiny documentation helper file/script is clearly justified.

This is primarily a documentation/control-plane task.

---

# 5. Documentation Must Be Living Documentation

This is a critical requirement.

The documentation must be designed so that future Codex / Claude / developer implementation work updates it automatically and proactively.

Do not create documentation that becomes stale after the next patch.

The documentation package must define clear rules:

- every behavior-changing patch must update relevant docs in the same commit or adjacent commit;
- every architecture change must update architecture docs;
- every provider change must update provider policy and provider implementation docs;
- every data model change must update schema docs and export docs;
- every UI change must update UI specs and UI DoD evidence;
- every test strategy change must update QA docs;
- every milestone completion must update the roadmap and patch register;
- every known gap must be recorded in a risk/gap register;
- every important design decision must be recorded as an ADR;
- documentation updates must not wait for a manual user request.

If future implementation reveals that a current document is wrong, incomplete, or outdated, the agent must update it immediately as part of the same patch.

If the agent changes code and does not update affected docs, that patch is incomplete.

---

# 6. Required Documentation Package

Create or update the following documentation files.

Use exact paths.

## 6.1 Master documentation index

Create:

```text
docs/README.md
```

Purpose:

- entry point for all Android v2 documentation;
- explain which document answers which question;
- group docs by:
  - product;
  - architecture;
  - providers;
  - data;
  - UI/UX;
  - implementation;
  - QA;
  - release;
  - governance;
  - decisions.

Must include a “start here” reading order.

---

## 6.2 Premium product target

Create:

```text
docs/ANDROID_V2_PREMIUM_PRODUCT_TARGET.md
```

Purpose:

Define what “premium Android product” means for this project.

Include:

- product vision;
- user value;
- primary user workflows;
- Classic Mode as main workflow;
- IDE Mode as experimental workflow;
- Hebrew/Russian learning use case;
- offline/local-first expectations;
- audio ownership;
- export ownership;
- privacy expectations;
- UX quality bar;
- performance/reliability expectations;
- what is explicitly not part of the first premium milestone.

This document must prevent scope drift.

---

## 6.3 Architecture overview

Create:

```text
docs/ANDROID_V2_ARCHITECTURE.md
```

Purpose:

Define the target Android architecture.

Include:

- module/package structure;
- app layers:
  - UI;
  - ViewModel/state;
  - domain;
  - data repositories;
  - Room database;
  - provider layer;
  - export layer;
  - audio layer;
  - settings/secrets layer;
- dependency direction;
- where business logic is allowed;
- where business logic is forbidden;
- provider boundaries;
- local-first data flow;
- error-handling flow;
- future extensibility.

Preferred architecture unless current repo contradicts it:

```text
Kotlin
Jetpack Compose
MVVM or unidirectional state flow
Coroutines + Flow
Room / SQLite
Retrofit or Ktor
kotlinx.serialization
Android Keystore + encrypted preferences
Media3 / Android audio APIs
JUnit + AndroidX tests
```

Do not blindly introduce Hilt if manual DI is sufficient at the current scale. Document the decision.

---

## 6.4 Migration roadmap

Update or replace:

```text
docs/ANDROID_V2_MIGRATION_PLAN.md
```

Purpose:

Turn the current migration plan into a detailed premium implementation roadmap.

Must include:

- current checkpoint status;
- completed work;
- remaining milestones;
- dependency graph;
- risk level per milestone;
- validation requirements per milestone;
- rollback strategy per milestone;
- exact patch sequence.

Use this roadmap structure unless a better one is justified:

```text
M0 — Foundation checkpoint
M1 — Local Room library storage
M2 — Classic Mode functional flow
M3 — Translation providers
M4 — TTS/audio providers
M5 — Audio storage and playback
M6 — Export ZIP with audio
M7 — Library UI and saved text lifecycle
M8 — Editing/reorder/reset behavior
M9 — API key/settings/security
M10 — IDE Mode experimental integration
M11 — Import/compatibility layer
M12 — Premium UI/UX polish
M13 — QA hardening
M14 — Release readiness
```

For every milestone include:

- goal;
- scope;
- out of scope;
- files likely affected;
- implementation notes;
- tests;
- UI DoD evidence;
- documentation updates required;
- commit expectations.

---

## 6.5 Patch register

Create:

```text
docs/ANDROID_V2_PATCH_REGISTER.md
```

Purpose:

Operational register for implementation.

This document must be updated after every future patch.

Include table columns:

```text
Patch ID
Milestone
Status
Goal
Main files
Tests required
Docs required
Risks
Commit hash
Date
Notes
```

Initial entries must include already completed checkpoint commits:

```text
3800af6 chore(android): bootstrap v2 migration skeleton
fd819de docs(android): update migration checkpoint status
```

If exact commit status needs verification, state that verification is required.

---

## 6.6 Requirements traceability matrix

Create:

```text
docs/ANDROID_V2_REQUIREMENTS_TRACEABILITY.md
```

Purpose:

Trace every major requirement to docs, code, and tests.

Include requirements such as:

- Classic Mode is primary.
- IDE Mode is experimental.
- Android v2 must not depend on Railway.
- Android v2 must not depend on cloud app storage.
- Allowed translation providers only.
- Allowed TTS providers only.
- Local library must persist on device.
- Library export must include audio-aware metadata.
- API keys must not be exported or logged.
- Hebrew RTL must be readable.
- Long content must scroll.
- Export must not fail because one audio file is missing.
- Provider failures must be visible.
- No silent fallback for quota/billing/invalid-key errors.

Columns:

```text
Requirement ID
Requirement
Source / rationale
Design doc
Implementation area
Test coverage
Status
Notes
```

---

## 6.7 Data model and Room schema spec

Create:

```text
docs/ANDROID_V2_DATA_MODEL.md
```

Purpose:

Define Android v2 local data model before implementation.

Include:

- Room entity list;
- fields;
- indexes;
- foreign keys;
- cascade behavior;
- stable IDs;
- row ordering;
- edit metadata;
- audio metadata;
- stale audio semantics;
- provider provenance;
- schema versioning;
- migration rules;
- mapping from source web schema to Android schema;
- explicitly excluded source tables.

Must cover at least:

```text
library_texts
library_rows
audio_assets
row_audio
text_audio
export_history
provider_events or provider_call_log if justified
settings/secrets metadata if stored outside Room
```

Also include:

- transaction boundaries;
- save/update behavior;
- duplicate text handling;
- row edit/reset behavior;
- reorder behavior;
- delete behavior;
- archive behavior if supported.

---

## 6.8 Local library implementation plan

Create:

```text
docs/ANDROID_V2_LOCAL_LIBRARY_PLAN.md
```

Purpose:

Define the implementation plan for the local Android library.

Include:

- repository interfaces;
- DAO responsibilities;
- transaction rules;
- save/load/update/edit/reset/delete/reorder flows;
- UI state implications;
- testing plan;
- migration from current skeleton models;
- failure behavior;
- concurrency assumptions;
- app restart safety.

This should be immediately actionable for Milestone 1.

---

## 6.9 Provider implementation plan

Create or update:

```text
docs/ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md
```

Purpose:

Define exactly how allowed providers will be implemented later.

Include:

- common provider interfaces;
- request/response models;
- error categories;
- retry strategy;
- timeout strategy;
- no-silent-fallback rule;
- provenance persistence;
- user-visible degraded states;
- fake providers for tests;
- real provider wiring order.

For translation:

```text
google_translate_free
gcp_translate
gemini_legacy
```

For TTS:

```text
google_online_tts
system_or_browser_fallback_low_quality
```

Explicitly document disallowed providers and why they are excluded.

Also document open decisions:

- GCP key vs service account JSON on device;
- Gemini key handling;
- niqqud provider strategy;
- whether Google Free implementation is stable enough for production.

---

## 6.10 Provider policy

Update:

```text
docs/ANDROID_V2_PROVIDER_POLICY.md
```

Purpose:

Make it stricter and implementation-facing.

Include:

- allowed provider IDs;
- blocked provider IDs;
- provider addition process;
- code guard requirements;
- tests required to add a provider;
- security requirements;
- provenance requirements;
- error mapping requirements;
- provider UI display requirements.

Also include a checklist:

```text
Before adding a provider:
[ ] Added to policy
[ ] Added to code allowlist
[ ] Added fake implementation
[ ] Added contract tests
[ ] Added user-visible error states
[ ] Added docs
[ ] Confirmed no Railway/localhost dependency
[ ] Confirmed no secret logging
```

---

## 6.11 Audio architecture

Create:

```text
docs/ANDROID_V2_AUDIO_ARCHITECTURE.md
```

Purpose:

Define audio generation, storage, playback, stale-state, and export behavior.

Include:

- full-text audio;
- row-level audio;
- local audio file storage;
- deterministic asset key;
- stale audio when text/niqqud changes;
- playback behavior;
- missing audio behavior;
- low-quality fallback labeling;
- Google Online TTS output handling;
- duration/size metadata;
- export inclusion;
- privacy concerns.

---

## 6.12 Export/import specification

Update:

```text
docs/ANDROID_V2_LIBRARY_EXPORT_SPEC.md
```

Purpose:

Make export/import spec implementation-ready.

Must include:

- ZIP layout;
- manifest schema;
- library JSON schema;
- audio folder layout;
- metadata folder layout;
- missing audio manifest;
- partial backup flag;
- schema versioning;
- privacy warning;
- Android Storage Access Framework / share sheet behavior;
- export transaction/snapshot strategy;
- interrupted export behavior;
- future import strategy;
- compatibility with old web JSON export.

Also include sample JSON structures.

Use real field names, not vague placeholders.

---

## 6.13 Classic Mode UI/UX master spec

Update:

```text
docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md
```

Purpose:

Make Classic Mode UI implementation-ready.

Include:

- phone portrait layout;
- phone landscape behavior;
- source input area;
- provider controls;
- generation button;
- table/card output;
- row card design;
- Hebrew/niqqud display rules;
- transliteration display rules;
- Russian translation display rules;
- TTS controls;
- save/library/export actions;
- loading states;
- empty states;
- error states;
- editing sheet/dialog;
- row reset;
- row reorder;
- stale audio indicator;
- accessibility;
- RTL support;
- keyboard behavior;
- Android insets;
- touch target rules;
- UI DoD evidence checklist.

Classic Mode must feel like a native premium Android workflow, not a compressed web table.

---

## 6.14 IDE Mode experimental strategy

Create:

```text
docs/ANDROID_V2_IDE_MODE_EXPERIMENTAL_STRATEGY.md
```

Purpose:

Control IDE Mode scope.

Include:

- why IDE Mode exists;
- what is included now;
- what is intentionally deferred;
- how IDE Mode reuses shared models;
- how it must not destabilize Classic Mode;
- UI labeling requirements;
- future roadmap;
- risks;
- test expectations.

---

## 6.15 Settings and secrets plan

Create:

```text
docs/ANDROID_V2_SETTINGS_AND_SECRETS.md
```

Purpose:

Define API-key and settings handling.

Include:

- Android Keystore;
- encrypted preferences;
- key masking;
- key update/delete;
- provider-specific key status;
- no secrets in logs;
- no secrets in exports;
- no secrets in Git;
- debug/release behavior;
- fake keys for tests;
- manual QA checklist.

---

## 6.16 Error handling and degraded states

Create:

```text
docs/ANDROID_V2_ERROR_HANDLING.md
```

Purpose:

Define user-visible and internal error strategy.

Include:

- error taxonomy;
- provider error mapping;
- network failures;
- invalid key;
- quota exceeded;
- billing required;
- unsupported language;
- invalid response;
- missing audio;
- export failure;
- database failure;
- retry strategy;
- no infinite loading;
- no silent fallback;
- degraded state badges;
- user-facing message examples.

---

## 6.17 Testing and QA strategy

Create:

```text
docs/ANDROID_V2_QA_TEST_STRATEGY.md
```

Purpose:

Define validation strategy.

Include:

- unit tests;
- Room repository tests;
- provider contract tests;
- fake provider tests;
- integration tests;
- Compose UI tests;
- manual smoke tests;
- accessibility smoke tests;
- export/import tests;
- audio tests;
- regression tests;
- CI expectations.

Do not target arbitrary 100% coverage.

Tests must validate behavior.

Include mandatory test commands:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
git diff --cached --check
```

Also define when instrumented tests are required.

---

## 6.18 UI DoD evidence

Create:

```text
docs/ANDROID_V2_UI_DOD_EVIDENCE.md
```

Purpose:

Track visual/manual UI evidence per milestone.

Include sections for:

- Classic Mode portrait;
- Classic Mode landscape;
- long Hebrew text;
- keyboard behavior;
- generated rows;
- row editing;
- TTS controls;
- library screen;
- export flow;
- error states;
- IDE Mode experimental screen.

For each section include:

```text
Status
Date
Device/emulator
Build variant
Evidence path or written result
Known issues
Follow-up patch
```

This document must be updated whenever UI changes.

---

## 6.19 Release readiness plan

Create:

```text
docs/ANDROID_V2_RELEASE_READINESS.md
```

Purpose:

Define what must be true before Android v2 can be considered releasable.

Include:

- debug/release build separation;
- signing plan;
- versioning;
- ProGuard/R8 considerations;
- privacy/security checks;
- export verification;
- local data backup expectations;
- provider configuration;
- Play Store readiness if relevant;
- manual QA checklist;
- known non-goals;
- release blocker list.

---

## 6.20 Risk and gap register

Create:

```text
docs/ANDROID_V2_RISK_AND_GAP_REGISTER.md
```

Purpose:

Track risks, blind spots, unresolved decisions, and mitigations.

Columns:

```text
ID
Area
Risk / gap
Severity
Likelihood
Impact
Mitigation
Owner / next action
Status
Updated date
```

Must include at least:

- GCP credential strategy on device;
- Gemini key strategy;
- niqqud strategy;
- transliteration parity;
- audio export reliability;
- old web JSON import compatibility;
- Android storage restrictions;
- provider quota UX;
- IDE Mode scope creep;
- stale documentation risk;
- secret leakage risk.

---

## 6.21 ADR directory

Create directory:

```text
docs/adr
```

Create index:

```text
docs/adr/README.md
```

Create initial ADRs:

```text
docs/adr/0001-native-android-not-webview.md
docs/adr/0002-local-first-room-storage.md
docs/adr/0003-provider-allowlist-android-v2.md
docs/adr/0004-classic-mode-primary-ide-mode-experimental.md
docs/adr/0005-export-zip-with-audio-manifest.md
docs/adr/0006-documentation-as-living-control-plane.md
```

ADR format:

```text
# ADR-XXXX: Title

Date:
Status: Accepted / Proposed / Superseded

## Context

## Decision

## Consequences

## Alternatives considered

## Follow-up
```

---

## 6.22 Documentation maintenance policy

Create:

```text
docs/DOCUMENTATION_MAINTENANCE_POLICY.md
```

This is one of the most important files.

Purpose:

Define that documentation must be updated proactively without manual reminders.

Include:

## Required rule

Any future implementation agent must update docs whenever it changes:

- behavior;
- architecture;
- provider policy;
- data model;
- schema;
- export format;
- UI flow;
- error behavior;
- security/secrets behavior;
- test strategy;
- release process;
- milestone status.

## No-manual-reminder rule

The developer must not need to ask:

```text
"Update the documentation"
```

Documentation update is part of the task.

If docs are stale, the implementation is incomplete.

## Patch DoD rule

Every future patch must answer:

```text
Which docs did this patch affect?
Were they updated?
If not, why not?
```

## Commit rule

Documentation updates should be committed together with the behavior change or in a directly adjacent docs commit.

## Status rule

After each patch, update:

```text
docs/ANDROID_V2_PATCH_REGISTER.md
docs/ANDROID_V2_MIGRATION_PLAN.md
docs/ANDROID_V2_RISK_AND_GAP_REGISTER.md
```

when relevant.

## ADR rule

If a meaningful architecture decision is made, add or update an ADR.

## UI rule

If UI changes, update:

```text
docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md
docs/ANDROID_V2_UI_DOD_EVIDENCE.md
```

when relevant.

## Data rule

If schema/data/export changes, update:

```text
docs/ANDROID_V2_DATA_MODEL.md
docs/ANDROID_V2_LIBRARY_EXPORT_SPEC.md
```

## Provider rule

If provider behavior changes, update:

```text
docs/ANDROID_V2_PROVIDER_POLICY.md
docs/ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md
docs/ANDROID_V2_ERROR_HANDLING.md
```

## Final instruction

Future agents must treat this maintenance policy as binding.

---

## 6.23 Future implementation prompt template

Create:

```text
docs/PROMPT_TEMPLATE_FOR_FUTURE_PATCHES.md
```

Purpose:

Provide a reusable prompt template for future Codex implementation tasks.

It must include:

- repo path;
- current milestone;
- required docs to read first;
- required docs to update;
- tests to run;
- PowerShell-only command rule;
- no secrets rule;
- no Railway rule;
- provider allowlist rule;
- docs maintenance rule;
- patch output format.

This template must explicitly remind future agents:

```text
Do not wait for the developer to ask you to update docs.
If your patch changes behavior, update the relevant docs immediately.
```

---

# 7. Required Work Process

Follow this sequence.

## Step 1 — Inspect target repo

Work in:

```powershell
cd E:\projects\tts-prototype-android-v2
```

Inspect:

```powershell
git status
dir
dir docs
```

Read existing docs before creating new ones.

Do not overwrite valuable content blindly.

Merge and improve.

## Step 2 — Inspect source repo only as reference

Use source repo only for behavioral reference:

```text
E:\projects\tts-prototype-android
```

Do not modify it.

Use it only to verify current behavior and source contracts.

## Step 3 — Create documentation structure

Create/update all required docs listed above.

Keep docs internally consistent.

Avoid vague statements.

Write implementation-facing documentation.

## Step 4 — Cross-link docs

Every major doc must link to related docs.

`docs/README.md` must act as the navigation hub.

## Step 5 — Add traceability

Make sure requirements, architecture, implementation roadmap, tests, and risks are connected.

A future developer must be able to answer:

```text
Why is this requirement here?
Where is it implemented?
How is it tested?
Which doc owns it?
What is still missing?
```

## Step 6 — Update checkpoint status

Update migration plan and patch register to show:

- current state;
- completed foundation checkpoint;
- next recommended milestone;
- blocking questions;
- documentation package completion.

## Step 7 — Validate docs

Run at minimum:

```powershell
git diff --check
git status
```

If project build files are not modified, Gradle build is not mandatory, but if any Gradle/source file changes are made, run:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

## Step 8 — Commit and push

Commit documentation package with a meaningful commit message.

Suggested commit:

```text
docs(android): add premium migration documentation control plane
```

Push to:

```text
origin/main
```

---

# 8. Documentation Quality Requirements

The documentation must be:

- precise;
- implementation-facing;
- internally consistent;
- easy to navigate;
- specific to this project;
- not generic Android boilerplate;
- written for future execution;
- explicit about risks;
- explicit about non-goals;
- explicit about provider restrictions;
- explicit about local-first data ownership;
- explicit about export with audio;
- explicit about documentation maintenance.

Avoid empty corporate language.

Avoid vague statements like:

```text
Improve UX
Add tests
Handle errors
Use good architecture
```

Replace them with concrete rules, examples, and acceptance criteria.

---

# 9. Premium Product Quality Bar

The docs must define Android v2 as a premium product.

Premium means:

- stable core workflow;
- predictable behavior;
- no hidden provider switching;
- local user data ownership;
- reliable export;
- clear error states;
- polished mobile UX;
- readable Hebrew RTL;
- accessible touch targets;
- good keyboard behavior;
- no fragile web table squeezed into mobile;
- no silent loss of audio;
- no secret leakage;
- no Railway dependency;
- no desktop sidecar dependency;
- testable architecture;
- incremental delivery.

Premium does NOT mean adding many providers or experimental features.

Premium means correctness, stability, ownership, and polish.

---

# 10. Important Implementation Boundaries

In this task, do not implement:

- real Room database;
- real provider network code;
- real TTS;
- real export ZIP writer;
- large UI redesign;
- old library import;
- WebView bridge.

This task is documentation/control-plane only.

Tiny helper files are allowed only if they directly support documentation validation and do not create architecture churn.

---

# 11. Final Output Required

At the end, report:

1. Which docs were created.
2. Which docs were updated.
3. Which current gaps/risks remain.
4. Which milestone should be implemented next.
5. Which tests/checks were run.
6. Git commit hash.
7. Push status.

The final answer must be concise but complete.

---

# 12. Absolute Rules

- Use PowerShell-compatible commands only.
- Do not modify `E:\projects\tts-prototype-android`.
- Work in `E:\projects\tts-prototype-android-v2`.
- Do not introduce Railway dependency.
- Do not introduce cloud storage dependency.
- Do not introduce disallowed providers.
- Do not commit secrets.
- Do not use placeholders in documentation.
- Do not leave docs disconnected from each other.
- Do not create stale documentation.
- Do not wait for manual reminders to update docs in future patches.
- Treat documentation as part of implementation, not as an optional afterthought.
