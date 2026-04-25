# Production-Oriented Migration Prompt: tts-prototype-android → Android App v2

You are operating as a senior software architect and Android product engineer tasked with migrating an existing web-based Hebrew/Russian TTS/translation learning system into a production-quality Android application.

Treat the current system as a behavioral contract, not merely as a codebase.

The goal is NOT to blindly rewrite everything.
The goal is to preserve the valuable product behavior of the existing web app while creating a clean, maintainable Android application with a controlled, testable migration path.

---

## 0. Current Project Context

The current development repository is:

```text
E:\projects\tts-prototype-android
```

Current remote:

```text
https://github.com/SindromRadioSpb/tts-prototype-android
```

A new Android application repository must be created at:

```text
E:\projects\tts-prototype-android-v2
```

It must be initialized as a Git repository and connected to:

```text
https://github.com/SindromRadioSpb/tts-prototype-android-v2
```

Use PowerShell-compatible commands and instructions only.

The existing project is a web-based prototype / application that includes:

- Classic Mode
- IDE Mode
- Hebrew text processing
- niqqud / transliteration pipeline
- Russian translation
- text table generation
- table editing
- text/audio library behavior
- TTS playback/provider selection
- provider fallback logic
- mobile browser UX workarounds and constraints
- experimental local/server-side providers that are NOT necessarily suitable for Android v2

The Android v2 app must become the new product direction.

---

## 1. Product Goal

Create a native Android mobile application based on the current development state.

The Android app must include:

### 1.1 Classic Mode

Classic Mode is the primary production mode.

It must be treated as the main user workflow.

Classic Mode should support, at minimum:

- input of Hebrew text;
- generation of structured rows/table-like output;
- Hebrew original text;
- niqqud text where available;
- transliteration display;
- Russian translation;
- TTS/audio playback where available;
- saving results into a local text/audio library;
- editing/correcting generated table data where applicable;
- exporting the text library, including audio-related data/metadata.

### 1.2 IDE Mode

IDE Mode must be included as an experimental mode.

It is not the primary production workflow.

IDE Mode may be incomplete, evolving, and subject to future redesign.

Requirements for IDE Mode:

- keep it available in the Android app;
- clearly mark it as experimental / under development;
- avoid blocking Classic Mode delivery because of IDE Mode;
- preserve known behavior where practical;
- do not over-engineer IDE Mode in the first Android migration unless required by shared infrastructure.

---

## 2. Android-Specific Non-Negotiable Constraints

This is an Android application, not a hosted web app.

The Android app must NOT be architecturally dependent on:

- Railway;
- external cloud-hosted application storage;
- browser-only runtime assumptions;
- localhost sidecars running on the developer PC;
- desktop/server-only workflows;
- Node server as a required runtime dependency for normal mobile use.

The app must work as a mobile application with local device persistence.

Network APIs may be used only for explicitly allowed online providers.

---

## 3. Allowed Translation Providers for Android v2

Because this is an Android application, translation providers must be restricted to the following:

### Allowed

1. Google Translate Free
   - free/basic Google Translate path;
   - must be treated as an online provider;
   - must include clear failure handling.

2. GCP Translate
   - Google Cloud Translate via API key;
   - user/API-key configurable;
   - must support error states such as invalid key, quota, billing, network failure.

3. Gemini Legacy
   - legacy provider only;
   - keep isolated behind a provider interface;
   - do not make it a hard dependency for core app behavior.

### Not allowed in Android v2 first implementation

Do NOT carry over provider complexity from the web prototype unless explicitly approved.

Do NOT include as Android production providers:

- Railway-dependent translation routes;
- local MADLAD sidecar;
- local HY-MT sidecar;
- desktop localhost translation services;
- browser/server-only provider chains;
- experimental providers not listed above.

If old provider code exists, inventory it, document it, and either exclude it from Android v2 or isolate it as non-runtime reference material.

---

## 4. Allowed TTS Providers for Android v2

TTS/audio providers must be restricted to:

### Allowed

1. Online TTS
   - Google TTS with API key;
   - must be configurable;
   - must handle quota, invalid key, billing/network errors;
   - must persist generated audio or audio references according to the local library design.

2. Browser fallback / low-quality fallback
   - low-quality fallback equivalent;
   - for Android native app, interpret this as a simple platform/system fallback if available;
   - must be clearly labeled as low quality;
   - must not be presented as premium neural TTS.

### Not allowed in Android v2 first implementation

Do NOT include:

- Railway-dependent TTS;
- local Piper server-side bridge as a required runtime dependency;
- sherpa-onnx browser WASM as a required runtime dependency;
- localhost sidecar TTS;
- desktop-only TTS flows;
- noncommercial experimental local Hebrew TTS as a default production provider.

If the old web project contains these providers, preserve the knowledge in documentation but do not make them part of Android v2 runtime unless explicitly approved later.

---

## 5. Local Library and Export Requirement

The Android application must include a local Library of texts.

The library must support audio-aware storage.

The library must be saved on the mobile device.

The library must not require Railway or cloud storage.

The library must be exportable.

Export requirements:

- export all saved text/library items;
- include enough metadata to reconstruct the learning table/result;
- include provider/provenance fields where available;
- include audio metadata;
- include generated audio files if stored locally, or an explicit manifest if audio cannot be embedded directly;
- use a portable format suitable for backup/transfer;
- do not lock the user into app-private data without export;
- define import compatibility as a future extension unless it is cheap and safe to implement now.

The export format must be proposed after repository discovery.

Preferred direction unless discovery contradicts it:

- local database: Room / SQLite;
- audio files: app-specific storage with explicit export copy;
- export bundle: ZIP archive containing JSON manifest + database snapshot or normalized JSON + audio files.

Do not assume cloud sync.

---

## 6. Required Repository Transition

Create a clean Android v2 repository.

Source repository:

```text
E:\projects\tts-prototype-android
```

Target repository:

```text
E:\projects\tts-prototype-android-v2
```

Target remote:

```text
https://github.com/SindromRadioSpb/tts-prototype-android-v2
```

Required behavior:

- do not damage or delete the existing repository;
- use the existing repository as behavioral and product reference;
- create Android v2 as a new repository;
- initialize Git in the new repo;
- connect the remote;
- commit meaningful milestones;
- push to the remote after validated checkpoints.

Use PowerShell commands only.

---

## 7. Operating Principle

Treat the system as a behavioral contract, not as a codebase.

Sequence work as:

```text
discovery → constraints → strategy → implementation → validation → commit/push
```

Prefer incremental replacement over full rewrites.

Because the target is native Android, some implementation will necessarily be new.
However, the migration must preserve product behavior, user workflows, data semantics, and provider constraints.

Do not optimize for elegance at the cost of correctness.

When rules conflict, prioritize:

1. Product correctness
2. User data safety
3. Android runtime stability
4. Local-first library/export behavior
5. Provider constraint compliance
6. Type safety
7. Code quality
8. Test completeness
9. UI polish

---

# Phase 1 — Behavioral Inventory

Do not skip this phase.

Before designing or implementing Android v2, thoroughly analyze the existing repository:

```text
E:\projects\tts-prototype-android
```

Produce a structured inventory of the current behavior.

Include:

## 1.1 User-facing features

Inventory all visible product surfaces, especially:

- Classic Mode;
- IDE Mode;
- text input;
- table generation;
- Hebrew text columns;
- niqqud behavior;
- transliteration behavior;
- Russian translation behavior;
- TTS playback;
- provider selection;
- library/dashboard behavior;
- saved texts;
- text/audio storage;
- export/import if present;
- table editing;
- mobile-specific UI behavior;
- error/status badges;
- provenance fields;
- settings/configuration;
- API-key flows;
- any current Railway/server/cloud assumptions.

## 1.2 Inputs, outputs, and side effects

For each feature, document:

- user input;
- generated output;
- local side effects;
- network side effects;
- storage side effects;
- provider calls;
- error states;
- fallback behavior.

## 1.3 External integrations

Identify all external integrations:

- Google Translate;
- GCP Translate;
- Gemini;
- Google TTS;
- browser/system TTS;
- Railway;
- Node server routes;
- sidecars;
- local AI services;
- cloud storage;
- browser APIs;
- localStorage / IndexedDB / files;
- any hidden assumptions.

Classify each integration as:

```text
Allowed in Android v2
Not allowed in Android v2
Reference only
Needs decision
```

## 1.4 Data models and implicit contracts

Document:

- table row schema;
- text/library item schema;
- audio metadata schema;
- provider/provenance schema;
- translation result schema;
- TTS result schema;
- settings schema;
- export schema if present;
- implicit assumptions currently encoded in JS/HTML/server routes.

## 1.5 Edge cases

Identify known or likely edge cases:

- Hebrew RTL display;
- niqqud presence/absence;
- transliteration mismatch;
- provider failure;
- network offline;
- quota exhaustion;
- invalid API key;
- long texts;
- partially generated tables;
- edited rows;
- audio generation failure;
- audio file missing;
- app restart during generation;
- export with missing audio;
- Android permissions;
- Android storage restrictions;
- mobile keyboard/layout behavior.

## 1.6 Confidence levels

For each inventory item assign:

```text
High confidence
Medium confidence
Low confidence
```

Do NOT suggest improvements yet.

Focus only on understanding behavior.

---

# Phase 2 — Targeted Clarification

After completing the behavioral inventory, ask questions only about:

- low-confidence behaviors;
- ambiguous logic;
- Android-specific constraints that cannot be inferred;
- API-key handling requirements;
- export format decisions that materially affect implementation;
- user-data safety requirements;
- failure tolerance.

Avoid generic questions.

Assume high-confidence behavior is correct unless contradicted.

If implementation can proceed safely with explicit assumptions, state the assumptions and continue instead of blocking.

---

# Phase 3 — Android Constraints Definition

Before proposing architecture, explicitly define Android v2 constraints.

Include:

## 3.1 Runtime constraints

Define:

- minimum Android version;
- target Android SDK;
- offline behavior;
- online-only provider behavior;
- network timeout strategy;
- app lifecycle behavior;
- background task limitations;
- storage model;
- export permissions;
- audio playback model.

If not specified, propose conservative defaults and mark them as assumptions.

## 3.2 Performance requirements

Define expected behavior for:

- app startup;
- Classic Mode screen load;
- table generation;
- translation latency;
- TTS request latency;
- local database operations;
- export duration for large libraries;
- audio playback startup.

Do not invent unrealistic benchmarks.
Prefer user-visible responsiveness and stability.

## 3.3 Reliability expectations

Define:

- what happens offline;
- what happens on provider failure;
- what happens when API quota is exhausted;
- what happens if audio generation fails;
- what happens if export is interrupted;
- what data must never be lost.

## 3.4 Deployment constraints

Define:

- local Android app build;
- debug/release separation;
- secrets/API-key storage;
- no Railway dependency;
- no cloud data dependency;
- GitHub repository separation;
- future Play Store readiness if applicable.

## 3.5 Team and maintenance constraints

Assume the project must remain maintainable by a small team / solo developer.

Prefer:

- simple architecture;
- explicit modules;
- boring proven Android technologies;
- clear documentation;
- reproducible builds;
- testable provider boundaries.

---

# Phase 4 — Migration Strategy

Design a controlled migration strategy.

A full blind rewrite is not allowed.

Because the target is Android-native, implementation may be new, but behavior must be migrated incrementally.

Use a strangler-fig approach at the product behavior level.

Identify seams:

- Classic Mode UI;
- IDE Mode UI;
- provider layer;
- text processing layer;
- table/result model;
- local library;
- audio storage;
- export system;
- settings/API-key management.

For each seam, define:

- current behavior;
- Android v2 target behavior;
- migration risk;
- test/validation method;
- rollback or fallback approach.

Implementation must prioritize Classic Mode first.

IDE Mode must be migrated only enough to be present as experimental unless shared infrastructure is ready.

---

# Phase 5 — Architecture and Technology Selection

Only after discovery and constraints definition, propose Android v2 architecture.

Preferred baseline unless discovery provides a strong reason otherwise:

```text
Language: Kotlin
UI: Jetpack Compose
Architecture: MVVM or unidirectional state flow
Async: Kotlin Coroutines + Flow
Local DB: Room / SQLite
Networking: Retrofit or Ktor client
Serialization: kotlinx.serialization
DI: Hilt or simple manual DI depending on project size
Audio playback: ExoPlayer / Media3 or Android native audio APIs
Secure API key storage: Android Keystore + encrypted preferences
Export: ZIP bundle with manifest + data + audio files
Testing: JUnit, AndroidX test, Compose UI tests where practical
```

Do not choose trendy technology without justification.

Do not use webview as the default migration strategy unless explicitly justified by risk analysis.

If a WebView bridge is proposed as a temporary compatibility layer, it must be:

- temporary;
- isolated;
- documented;
- not the final architecture for Classic Mode;
- not dependent on Railway or desktop localhost services.

---

# Phase 6 — Provider Architecture

Create a strict provider boundary.

Translation providers must be isolated behind a common interface.

Allowed Android v2 translation providers:

```text
google_translate_free
gcp_translate
gemini_legacy
```

TTS providers must be isolated behind a common interface.

Allowed Android v2 TTS providers:

```text
google_online_tts
system_or_browser_fallback_low_quality
```

Provider requirements:

- typed request/response models;
- explicit provider IDs;
- explicit provenance;
- explicit error categories;
- no hidden fallback;
- no silent provider switching unless documented;
- user-visible provider status;
- retry policy per provider;
- quota/API-key errors surfaced clearly;
- no Railway dependency;
- no localhost sidecar dependency.

Provider errors should be categorized at least as:

```text
NetworkUnavailable
Timeout
Unauthorized
QuotaExceeded
BillingRequired
InvalidApiKey
ProviderUnavailable
InvalidResponse
UnsupportedLanguage
Unknown
```

---

# Phase 7 — Local Library, Audio Storage, and Export

Design the local library as a first-class product feature.

The library must store:

- source text;
- generated rows/table;
- Hebrew original;
- niqqud text if available;
- transliteration;
- Russian translation;
- translation provider provenance;
- TTS provider provenance;
- timestamps;
- edit history or at least edited flags;
- audio metadata;
- local audio file references if audio is stored;
- app/schema version.

The library must be local-first.

Do not require cloud storage.

Do not require Railway.

Export must support audio-aware backup.

Define:

- export manifest schema;
- export file structure;
- handling of missing audio;
- handling of partial/corrupted items;
- schema version;
- future import compatibility;
- privacy implications;
- Android file picker / share sheet behavior.

Preferred export shape unless contradicted:

```text
library-export-YYYYMMDD-HHMM.zip
  manifest.json
  library.json
  audio/
    item_<id>_<voice/provider>.mp3|wav
  metadata/
    app_version.json
    provider_provenance.json
```

If Room database snapshot is used, explain why and how compatibility will be maintained.

---

# Phase 8 — Quality System

Define a quality strategy focused on correctness, not arbitrary coverage percentages.

Required tests:

## 8.1 Behavioral regression tests

Cover migrated behavior from the existing web app:

- Classic Mode text input;
- generation of rows;
- provider selection;
- translation result handling;
- TTS result handling;
- save to library;
- reload from library;
- edit table row;
- export library;
- export with audio;
- export with missing audio;
- offline provider failure behavior.

## 8.2 Contract tests

Provider contract tests:

- Google Translate Free;
- GCP Translate;
- Gemini Legacy;
- Google Online TTS;
- low-quality fallback TTS.

Use fakes/mocks for CI-safe tests.

Do not require real API keys in automated tests.

## 8.3 Integration tests

Critical user paths:

- first app launch;
- configure API key;
- Classic Mode generate table;
- save text to library;
- generate/play audio;
- export library;
- reopen app and verify saved data;
- switch to IDE Mode and back.

## 8.4 Android UI tests

At minimum:

- Classic Mode screen renders on phone-size viewport;
- long text does not break layout;
- RTL Hebrew text displays correctly;
- action buttons remain accessible;
- loading/error states visible;
- export action reachable;
- IDE Mode clearly marked experimental.

Do not chase arbitrary 100% coverage.
Tests must validate behavior.

---

# Phase 9 — Type Safety and Code Standards

Enforce:

- Kotlin strict null-safety;
- explicit data models;
- no untyped maps for core entities unless justified;
- no implicit provider response parsing in UI layer;
- consistent formatting;
- clear package boundaries;
- no business logic inside Composables;
- no API keys hardcoded in source;
- no secrets committed to Git;
- no silent swallowing of provider errors.

When strict typing conflicts with legacy behavior:

1. preserve correct behavior first;
2. apply type safety second;
3. document compromise explicitly;
4. create follow-up task if needed.

---

# Phase 10 — Observability and Safety

Define Android-appropriate observability.

Include:

- structured logs for provider calls without exposing secrets;
- local debug diagnostics screen or exportable logs if useful;
- provider status indicators;
- user-visible error messages;
- crash-safe local persistence;
- export failure reporting;
- migration/version logs for local DB;
- no sensitive API keys in logs.

Production safety must include:

- safe failure modes;
- retry boundaries;
- no infinite loading;
- no data loss on provider failure;
- no library corruption on interrupted export;
- no crash when audio file is missing.

---

# Phase 11 — UI/UX Constraints

The Android UI must be designed as a premium mobile application, not as a raw port of a desktop/web page.

## 11.1 General UI constraints

- no fixed heights that break small screens;
- all long content must scroll safely;
- support Android insets and system bars;
- support dynamic keyboard appearance;
- no controls hidden behind keyboard;
- important actions must remain reachable on phone screens;
- readable typography;
- clear hierarchy;
- consistent spacing;
- explicit loading states;
- explicit empty states;
- explicit error states;
- no cramped web-table layout on narrow screens.

## 11.2 Classic Mode UI constraints

Classic Mode is primary.

It must have:

- clear input area;
- clear generate/action flow;
- visible provider status;
- readable generated result;
- mobile-friendly row/card presentation if table is too dense;
- editing affordances suitable for touch;
- TTS playback controls per item/row where applicable;
- save-to-library action;
- export/library navigation.

If a table layout is retained, it must support:

- horizontal scroll where appropriate;
- sticky/visible key controls;
- readable Hebrew RTL cells;
- touch-safe editing;
- no accidental full-table copy/paste bugs.

A card/list representation may be preferable for Android.

## 11.3 IDE Mode UI constraints

IDE Mode is experimental.

It must:

- be reachable;
- be visually marked as experimental;
- not dominate the app navigation;
- not confuse users who only need Classic Mode;
- not block Classic Mode usability.

## 11.4 Accessibility

Include:

- content descriptions for icon buttons;
- sufficient touch targets;
- readable contrast;
- screen reader-safe labels where practical;
- support for font scaling as much as possible;
- predictable focus behavior.

## 11.5 UI Definition of Done evidence

For each major UI patch provide evidence:

- screenshot or written smoke result for phone portrait;
- screenshot or written smoke result for phone landscape if relevant;
- confirmation that long content scrolls;
- confirmation that keyboard does not block primary input/action;
- confirmation that Hebrew RTL text is readable;
- confirmation that loading/error/empty states are present.

---

# Phase 12 — Security and Secrets

API keys must not be hardcoded.

Implement or plan:

- secure local API-key storage;
- masked API-key display;
- ability to update/delete keys;
- no API keys in logs;
- no API keys in export unless explicitly approved;
- no secrets committed to Git;
- clear error messages for invalid/missing keys.

For the first implementation, if secure storage is delayed, explicitly document it as a blocker or temporary compromise.

---

# Phase 13 — Implementation Roadmap

Produce a step-by-step roadmap before changing code.

The roadmap must include:

- repository bootstrap;
- Android project creation;
- migration of shared behavioral models;
- Classic Mode implementation;
- provider interfaces;
- allowed translation providers;
- allowed TTS providers;
- local library;
- audio storage;
- export bundle;
- IDE Mode experimental shell;
- settings/API-key screens;
- validation/tests;
- documentation;
- commit/push checkpoints.

Use patch-style phases.

Example structure:

```text
PATCH-00 — Repository bootstrap and Android skeleton
PATCH-01 — Behavioral model and local data schema
PATCH-02 — Classic Mode primary UI shell
PATCH-03 — Translation provider interface and allowed providers
PATCH-04 — TTS provider interface and allowed providers
PATCH-05 — Local library persistence
PATCH-06 — Audio storage and playback
PATCH-07 — Export library with audio manifest/bundle
PATCH-08 — IDE Mode experimental shell
PATCH-09 — Settings/API-key management
PATCH-10 — Regression tests and Android UI smoke tests
PATCH-11 — Documentation and release checklist
```

For each patch define:

- goal;
- files/modules;
- risks;
- validation;
- rollback strategy;
- commit message.

---

# Phase 14 — Execution Rules

Do not start implementation before completing:

1. behavioral inventory;
2. Android constraints definition;
3. migration strategy;
4. initial implementation roadmap.

During implementation:

- make small commits;
- keep Git status clean between milestones;
- do not mix unrelated changes;
- do not delete the original repo;
- do not introduce Railway dependency;
- do not introduce unapproved providers;
- do not commit secrets;
- do not silently drop library/export behavior;
- do not let IDE Mode delay Classic Mode.

After each patch:

- run relevant tests;
- perform a smoke check;
- update documentation if behavior changed;
- commit with a meaningful message;
- push only after validation.

---

# Phase 15 — Required Deliverables

Produce the following deliverables.

## 15.1 Discovery report

Create:

```text
docs/DISCOVERY_BEHAVIORAL_INVENTORY.md
```

Include:

- feature inventory;
- provider inventory;
- data model inventory;
- current dependencies;
- Android allowed/not-allowed classification;
- confidence levels;
- risks and unknowns.

## 15.2 Android migration plan

Create:

```text
docs/ANDROID_V2_MIGRATION_PLAN.md
```

Include:

- architecture;
- migration seams;
- patch roadmap;
- provider strategy;
- local library/export strategy;
- testing strategy;
- risk register.

## 15.3 Provider policy

Create:

```text
docs/ANDROID_V2_PROVIDER_POLICY.md
```

Include:

- allowed translation providers;
- disallowed translation providers;
- allowed TTS providers;
- disallowed TTS providers;
- error handling;
- API-key handling;
- provenance rules.

## 15.4 Library/export specification

Create:

```text
docs/ANDROID_V2_LIBRARY_EXPORT_SPEC.md
```

Include:

- local storage model;
- audio storage model;
- export bundle structure;
- manifest schema;
- missing audio handling;
- future import compatibility.

## 15.5 UI/UX specification

Create:

```text
docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md
```

Include:

- Classic Mode mobile layout;
- generated result display;
- editing behavior;
- TTS controls;
- library navigation;
- export entrypoint;
- IDE Mode experimental placement;
- accessibility requirements;
- UI DoD evidence checklist.

## 15.6 Implementation

Create the Android v2 app in:

```text
E:\projects\tts-prototype-android-v2
```

Initialize Git and connect remote:

```text
https://github.com/SindromRadioSpb/tts-prototype-android-v2
```

---

# Phase 16 — PowerShell Command Discipline

All command-line instructions must be PowerShell-compatible.

Do not provide Bash-only commands.

When creating the repo, use PowerShell-safe commands.

Example expected style:

```powershell
cd E:\projects
mkdir tts-prototype-android-v2
cd E:\projects\tts-prototype-android-v2
git init
git remote add origin https://github.com/SindromRadioSpb/tts-prototype-android-v2
git status
```

Do not use placeholders.

Do not assume Unix paths.

---

# Final Rule Hierarchy

When requirements conflict, resolve them in this order:

1. Preserve correct user-facing behavior from the current product where applicable.
2. Protect user data and local library contents.
3. Keep Android v2 independent from Railway and cloud storage.
4. Restrict providers to the approved Android v2 provider set.
5. Prioritize Classic Mode over IDE Mode.
6. Maintain Android app stability.
7. Keep architecture simple and maintainable.
8. Use strict typing and clear module boundaries.
9. Add tests that validate behavior.
10. Improve UI polish after correctness and stability are secured.

Do not guess silently.

Make uncertainty explicit.

If a behavior is unclear but non-blocking, document the assumption and continue.

If a behavior is unclear and could cause data loss, provider misuse, security issues, or major architecture mistakes, stop and ask a targeted question.
