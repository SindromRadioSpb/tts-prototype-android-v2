# Android v2 Architecture

Date: 2026-04-25

## Architecture Decision

Android v2 uses a single Android app module for now. The app uses Kotlin, Jetpack Compose, MVVM/unidirectional state, Coroutines and Flow, future Room/SQLite persistence, future Retrofit or Ktor networking, kotlinx.serialization, Android Keystore-backed encrypted preferences, Media3 or Android audio APIs, and JUnit/AndroidX tests.

Manual dependency construction is the default until the app has enough object graph complexity to justify Hilt. Hilt must not be introduced only to satisfy convention.

## Package Structure

Target package layout:

| Package | Responsibility |
|---------|----------------|
| `ui` | Compose screens, navigation, UI state rendering only. |
| `ui.theme` | Theme, typography, color, spacing tokens. |
| `feature.classic` | Classic Mode ViewModels, UI state, user actions. |
| `feature.library` | Library screens and saved text lifecycle UI. |
| `feature.settings` | Provider settings, masked key status, diagnostics. |
| `feature.ide` | Experimental IDE Mode shell and future IDE-specific state. |
| `core.model` | Stable domain models shared by UI/data/provider/export layers. |
| `core.provider` | Provider interfaces, allowlist guard, fake provider contracts. |
| `core.audio` | Audio asset keys, playback coordination, stale-state rules. |
| `core.export` | Export/import manifest models and ZIP writer interfaces. |
| `data.db` | Room database, entities, DAOs, migrations. |
| `data.repository` | Transactional library repositories and mapping between Room/domain. |
| `data.settings` | Encrypted settings/secrets adapters. |
| `data.network` | HTTP clients for allowed providers only. |

Existing M0 code already has `core.model`, `core.provider`, `core.export`, `ui`, and `ui.theme`.

## Dependency Direction

Allowed direction:

`UI -> ViewModel/state -> domain contracts -> repositories/providers/audio/export -> platform/network/db adapters`

Forbidden direction:

- Room entities must not depend on Compose.
- Provider adapters must not update UI directly.
- UI must not call Retrofit/Ktor or Room DAOs directly.
- Export code must not read API keys.
- IDE Mode must not own shared Classic state.

## Layer Rules

| Layer | Business logic allowed | Business logic forbidden |
|-------|------------------------|--------------------------|
| Compose UI | Input event forwarding, visual formatting, accessibility text. | Provider decisions, database writes, export assembly. |
| ViewModel/state | Orchestrating user actions, loading/error state, cancellation. | Raw SQL, credential storage details, HTTP response parsing. |
| Domain | Provider IDs, row semantics, edit/stale rules, export contract. | Android context, Room annotations, Compose types. |
| Repository | Transactions, duplicate handling, row order persistence, mapping. | Provider network calls inside DB transactions. |
| Provider layer | Request/response mapping, timeout/retry, provenance, error category. | Silent fallback for quota/billing/invalid-key. |
| Audio layer | Asset keys, playback state, stale/missing indicators. | Translation decisions, credential storage. |
| Settings/secrets | Key save/update/delete, masked status. | Exporting secrets, logging full key values. |

## Local-First Data Flow

1. User enters Hebrew in Classic Mode.
2. ViewModel validates non-empty input and selected allowlisted providers.
3. Translation provider returns rows plus provenance.
4. ViewModel renders generated rows before saving.
5. Save action writes `library_texts` and `library_rows` in one Room transaction.
6. TTS generation writes audio file first, then records `audio_assets` and row/text link in one transaction.
7. Export reads a snapshot from Room, copies available audio files, and writes missing audio metadata for absent files.

## Error Handling Flow

Provider and repository failures map to [Error Handling](ANDROID_V2_ERROR_HANDLING.md). Every user-visible failure has:

- category;
- source layer;
- retry action when safe;
- visible message;
- provenance or affected entity ID when available.

Quota, billing, invalid-key, and unauthorized errors must stop provider fallback and ask the user to fix configuration.

## Extensibility Rules

- New providers require policy update, code allowlist update, fake implementation, contract tests, UI label, error mapping, and docs.
- New Room tables require data model update, migration tests, export compatibility review, and risk register update.
- New UI modes must not bypass shared domain contracts.

## Related Docs

- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
- [Documentation Maintenance Policy](DOCUMENTATION_MAINTENANCE_POLICY.md)
