# Android v2 Local Library Implementation Plan

Date: 2026-04-25

This is the actionable plan and implementation status for M1: Local Room library storage.

## Implementation Status

Implemented:

- `RoomLibraryRepository` with the repository surface defined below.
- Room entity/DAO/database layer for local library and audio metadata.
- Duplicate text conflict through `text_key`.
- Transactional save/update/edit/reset/reorder/delete/add/archive/opened flows.
- Update flow preserves existing row IDs and row audio links for rows kept by order.
- Loaded `LibraryText.rows` include default row audio asset keys when linked.
- Default row audio stale marking when Hebrew or niqqud fields change.
- Robolectric Room tests for M1 behavior.

Still pending outside M1:

- SAF/share export UI wiring.
- Audio file cleanup.
- Compose screenshot/manual device evidence.

M2 update:

- Classic Mode can now save fake-generated rows through the `LibraryRepository` port into the Room-backed runtime repository.
- The ViewModel uses the repository summary flow to show saved text count in Classic Mode.
- Full open/archive/restore/delete lifecycle UI is implemented at repository/ViewModel/UI level in M7; screenshot/manual evidence is still pending.
- Row edit/reorder/reset/delete/add workflow is implemented at ViewModel/UI level in M8; screenshot/manual evidence is still pending.

## Repository Interfaces

Target repository surface:

- `observeTexts(includeArchived: Boolean): Flow<List<LibraryTextSummary>>`
- `getText(textId: String): LibraryTextWithRows`
- `saveGeneratedText(input: SaveGeneratedTextRequest): SaveTextResult`
- `updateGeneratedText(textId: String, input: SaveGeneratedTextRequest): SaveTextResult`
- `patchRow(textId: String, rowId: String, fields: EditableRowFields): LibraryRow`
- `resetRowFields(textId: String, rowId: String, fields: Set<RowField>): LibraryRow`
- `reorderRows(textId: String, orderedRowIds: List<String>): List<LibraryRow>`
- `deleteRow(textId: String, rowId: String): Unit`
- `deleteText(textId: String): Unit`
- `addRow(textId: String, afterRowId: String?, fields: EditableRowFields): LibraryRow`
- `archiveText(textId: String, archived: Boolean): Unit`
- `markOpened(textId: String, openedAt: Instant): Unit`

Current code exposes a small `LibraryRepository` port for M2 ViewModel wiring:

- `observeTexts(includeArchived: Boolean)`
- `saveGeneratedText(input: SaveGeneratedTextRequest)`

`RoomLibraryRepository` implements this port and also keeps the broader M1 repository methods.

## DAO Responsibilities

- DAOs expose table-level reads/writes only.
- Repositories own transactions and mapping to domain models.
- DAOs do not compute duplicate policy, stale audio, reset semantics, or provider provenance rules.

## Transaction Rules

- One write transaction per logical user action.
- No provider network call inside a DB transaction.
- No file copy inside a DB transaction.
- If a transaction fails, UI receives a database error category and the previous state remains visible.

## Save and Update Flow

1. Normalize tags to a deduped list.
2. Compute `text_key` from source text and relevant generation settings.
3. Insert `library_texts`; on duplicate return conflict result.
4. Insert `library_rows` with stable IDs and sequential `order_index`.
5. Store provider provenance in `table_model_meta_json`.
6. Return saved text with rows.

Update uses the same transaction but preserves row IDs where existing rows can be matched by order/hash and marks removed rows deleted.

## Edit, Reset, Delete, Add, Reorder

- Editable fields: `hebrew_plain`, `hebrew_niqqud`, `translit`, `translit_ru`, `russian`.
- Edit stores original field value in `edit_meta_json` before first change.
- Reset restores selected fields from `edit_meta_json.original`.
- Delete removes one row and compacts `order_index`.
- Add inserts after selected row or at end and compacts order.
- Reorder validates that the submitted row ID set exactly matches current non-deleted rows.

## UI State Implications

- Library writes expose `Saving`, `Saved`, `Conflict`, and `Failed` states.
- Row mutations keep the current table visible while operation is in progress.
- Conflict UI must offer update existing or cancel; it must not silently overwrite.
- Stale audio indicator appears immediately after Hebrew/niqqud edit.

M7 Library tab behavior:

- active summaries are shown by default;
- archived summaries are included only when the user enables the archived filter;
- opening a text marks `last_opened_at` and displays row previews;
- archive keeps the selected text open so the user can restore it immediately;
- delete clears the selected text and removes it from summaries through Room cascade behavior.

M8 row lifecycle behavior:

- editing opens an inline row draft form for Hebrew original, niqqud, SBL transliteration, Russian phonetic transliteration, and Russian translation;
- saving an edit sends only changed fields to `patchRow`;
- reset restores repository-tracked edited fields without requiring the user to manually select each field in this first UI pass;
- explicit `Up` and `Down` controls reorder rows through exact row ID validation;
- `Delete row` removes the row and compacts order;
- `Add row` appends at the end, while `Add after` inserts after a specific row;
- new rows require at least one non-blank field.

## Testing Plan

- Save/load round trip with Hebrew, niqqud, transliteration, Russian, tags.
- Duplicate text returns conflict.
- Update preserves stable row IDs where expected.
- Update preserves existing row audio links for preserved row IDs.
- Load includes default row audio link for UI consumers.
- Edit records original values and marks stale audio.
- Reset restores selected fields only.
- Reorder rejects missing/extra row IDs.
- Delete compacts order and cascades row audio links.
- Archive hides text by default but keeps data.
- App restart safety: close/reopen DB and read saved text.

Implemented test file:

- `app/src/test/java/com/sindromradiospb/ttsprototypev2/data/repository/RoomLibraryRepositoryTest.kt`
- `app/src/test/java/com/sindromradiospb/ttsprototypev2/feature/library/LibraryViewModelTest.kt`

## Failure Behavior

- DB locked or transaction failure: no partial row set is visible.
- Corrupt JSON metadata: repository returns structured error and does not crash UI.
- Missing audio file referenced by DB: row remains readable and audio is marked missing.

## Concurrency Assumptions

- Single process app.
- Repository serializes write operations with Room transactions.
- Multiple read flows may observe changes.
- Parallel provider calls may complete out of order, but only ViewModel/repository action decides what is saved.

## Related Docs

- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
