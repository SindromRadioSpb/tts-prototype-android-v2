# Android v2 Classic Mode Mobile v4 UI Spec

Date: 2026-04-25

This document is the implementation-ready UI contract for migrating the current web Classic Mode and Library v3 mobile experience into native Android v2. It is based on the v4 screenshot set copied into `docs/ui/v4`.

The goal is strong visual and behavioral parity with the source web product while adapting the layout to native Android, Jetpack Compose, touch targets, IME behavior, and scoped storage. Android must not reproduce source screenshot clipping where card action buttons overflow off-screen.

## Screenshot Inventory

| ID | Source file | Copied doc path | Screen / state | Purpose |
|----|-------------|-----------------|----------------|---------|
| UI-V4-00 | `E:\projects\tts-prototype-android\Picture UI\v4\0. Main classic mode.JPG` | [docs/ui/v4/0. Main classic mode.JPG](v4/0.%20Main%20classic%20mode.JPG) | Classic Mode mobile main screen | Defines Classic entry hierarchy, Library entry point, status cards, source input, settings sections, result/table area. |
| UI-V4-01 | `E:\projects\tts-prototype-android\Picture UI\v4\1. library_v3.PNG` | [docs/ui/v4/1. library_v3.PNG](v4/1.%20library_v3.PNG) | Library v3 default modal/screen | Defines header, export/import/refresh/close actions, filters, save button, tag chips, reset entry. |
| UI-V4-02 | `E:\projects\tts-prototype-android\Picture UI\v4\2. library_v3_level.PNG` | [docs/ui/v4/2. library_v3_level.PNG](v4/2.%20library_v3_level.PNG) | Level selector open | Defines selected checkmark, dark overlay menu, scrollable level options. |
| UI-V4-03 | `E:\projects\tts-prototype-android\Picture UI\v4\3. library_v3_tag.PNG` | [docs/ui/v4/3. library_v3_tag.PNG](v4/3.%20library_v3_tag.PNG) | Tags mode selector open | Defines `ALL` / `ANY` dropdown behavior. |
| UI-V4-04 | `E:\projects\tts-prototype-android\Picture UI\v4\4. library_v3_search.PNG` | [docs/ui/v4/4. library_v3_search.PNG](v4/4.%20library_v3_search.PNG) | Search scope selector open | Defines text/notes/rows scope menu and large multiline item labels. |
| UI-V4-05 | `E:\projects\tts-prototype-android\Picture UI\v4\5. library_v3_sort.PNG` | [docs/ui/v4/5. library_v3_sort.PNG](v4/5.%20library_v3_sort.PNG) | Sort selector open | Defines sort options and selected checkmark. |
| UI-V4-06 | `E:\projects\tts-prototype-android\Picture UI\v4\6. library_v3_list_cards.PNG` | [docs/ui/v4/6. library_v3_list_cards.PNG](v4/6.%20library_v3_list_cards.PNG) | Library list cards | Defines loaded count, saved text card content, tag chips, source URL, copy action, and visible clipping risk. |
| UI-V4-07 | `E:\projects\tts-prototype-android\Picture UI\v4\7. library_v3_action_cards.PNG` | [docs/ui/v4/7. library_v3_action_cards.PNG](v4/7.%20library_v3_action_cards.PNG) | Card action buttons | Defines action order and colors; also documents source web overflow that Android must fix. |
| UI-V4-08 | `E:\projects\tts-prototype-android\Picture UI\v4\8. library_v3_metadata_cards_изменить.PNG` | [docs/ui/v4/8. library_v3_metadata_cards_изменить.PNG](v4/8.%20library_v3_metadata_cards_%D0%B8%D0%B7%D0%BC%D0%B5%D0%BD%D0%B8%D1%82%D1%8C.PNG) | Metadata edit dialog | Defines text-level metadata fields, labels, actions, helper text, and keyboard-sensitive layout. |
| UI-V4-09 | `E:\projects\tts-prototype-android-v2\docs\ui\v4\9. ТАБЛИЦА ОТОБРАЖЕНИЕ И СЦЕНАРИИ.PNG` | [docs/ui/v4/9. ТАБЛИЦА ОТОБРАЖЕНИЕ И СЦЕНАРИИ.PNG](v4/9.%20%D0%A2%D0%90%D0%91%D0%9B%D0%98%D0%A6%D0%90%20%D0%9E%D0%A2%D0%9E%D0%91%D0%A0%D0%90%D0%96%D0%95%D0%9D%D0%98%D0%95%20%D0%98%20%D0%A1%D0%A6%D0%95%D0%9D%D0%90%D0%A0%D0%98%D0%98.PNG) | Classic table display and scenarios | Defines the source table block, visible column controls, row action column, selected row highlight, and post-row playback scenario. |
| UI-V4-10 | `E:\projects\tts-prototype-android-v2\docs\ui\v4\Результат_андроид.png` | [docs/ui/v4/Результат_андроид.png](v4/%D0%A0%D0%B5%D0%B7%D1%83%D0%BB%D1%8C%D1%82%D0%B0%D1%82_%D0%B0%D0%BD%D0%B4%D1%80%D0%BE%D0%B8%D0%B4.png) | Android pre-P018 result gap | Documents the previous generated-result card layout that P018 replaces with the source-style table. |

## Navigation Flow

```text
Classic Mode main screen
  -> tap "📚 Библиотека"
  -> Library v3 screen/modal
  -> filter/search/sort/list saved texts
  -> card actions:
       Open
       Continue
       Edit metadata
       Archive
       Delete
  -> tap "Изменить"
  -> Metadata edit screen/dialog
  -> Save/Cancel/Close
```

Classic Mode remains the primary workflow. Library v3 belongs to Classic Mode and must not be owned by IDE Mode. IDE Mode may reuse shared models later, but it must not control Library v3 navigation, filtering, metadata editing, export, or import.

## Classic Mode Main Screen Entry Point

Reference: [UI-V4-00](v4/0.%20Main%20classic%20mode.JPG).

Classic Mode must include a visible `📚 Библиотека` entry point near the top of the production workflow. It must be reachable without scrolling through advanced settings. It opens Library v3 as a modal sheet, full-screen dialog, or route that clearly returns to Classic Mode through `Закрыть`.

Rules:

- `📚 Библиотека` must remain available before generation, after table generation, after save, and after provider errors.
- The Library entry must not navigate to IDE Mode.
- The Library entry should sit beside the IDE mode entry only as a mode-level control; Library remains Classic-owned.
- If Classic Mode has stale result badges, source update warnings, or disabled table actions, the Library entry remains enabled.

## Classic Table Display And Scenarios

Reference: [UI-V4-09](v4/9.%20%D0%A2%D0%90%D0%91%D0%9B%D0%98%D0%A6%D0%90%20%D0%9E%D0%A2%D0%9E%D0%91%D0%A0%D0%90%D0%96%D0%95%D0%9D%D0%98%D0%95%20%D0%98%20%D0%A1%D0%A6%D0%95%D0%9D%D0%90%D0%A0%D0%98%D0%98.PNG).

Android v2 must render generated Classic rows as a table, not as unrelated vertical cards. The table block above the rows is titled:

```text
🧩 Таблица: отображение и сценарии
```

Required controls:

- `Колонки` / `Скрыть колонки` toggles only the column selector area.
- The prototype preset buttons `Полная`, `Иврит+рус`, `Фонетика`, `Только иврит` are intentionally not part of Android v2; they are represented by individual column checkboxes.
- Column checkboxes are shown in this order: `Действие`, `Иврит`, `Огласовки`, `Транслит`, `Перевод`.
- At least one column must remain visible.
- `▶▶ Построчное воспроизведение` starts auto-next playback from the selected/highlighted row; if no row is selected, it starts from the first row.
- While auto-next is active the start button becomes a stop action.
- The old `Плейлист (Auto-next)` checkbox is not required in Android v2; auto-next is implied by the start button label and behavior.

The generated table columns are:

```text
▶✎ | Иврит | Огласовки | Транслит | Перевод
```

Behavior:

- The `▶✎` action column contains row number, audio cache status, row TTS play action, and row notes action.
- Tapping a row selects/highlights it and makes it the auto-next start point.
- `▶` plays cached row audio if present; otherwise it synthesizes with the selected TTS provider. For saved rows, generated audio is adopted into app-owned storage and linked as row cache.
- `✎` opens the row notes editor. Notes belong to the saved row and must persist in Room.
- Column visibility immediately affects the table.
- Column widths are adjustable from the column headers. On phones this may be implemented as horizontal drag handles plus horizontal scrolling.
- Long Hebrew/Russian text must stay readable through table scrolling or ellipsis; it must not push action buttons off-screen.

Implementation status:

- P018 implements `ClassicTableColumn`, `ClassicTableDisplayState`, `Колонки` / `Скрыть колонки`, checkboxes, selected row, adjustable widths, and auto-next state.
- P018 replaces generated row cards in `Результат` with the table layout required by UI-V4-09.
- Manual screenshot evidence comparing UI-V4-09 with the Android screen remains required before release hardening.

## Library v3 Screen / Modal Layout

Reference: [UI-V4-01](v4/1.%20library_v3.PNG).

Library v3 must visually behave as a modal/sheet/screen with:

- rounded white container;
- dimmed or clearly separated background when opened as a modal;
- top header area;
- title on the left: `Библиотека (v3)`;
- action buttons on the right/top area in this order:
  - `Экспорт Библиотеки`;
  - `Импорт Библиотеки`;
  - `Обновить`;
  - `Закрыть`.

Native Android adaptation:

- On compact phones, action buttons may stack vertically as in the screenshot.
- The header must never clip horizontally or vertically.
- Header buttons must be at least 48dp high.
- Button text must not truncate unless the screen is too narrow even after wrapping; prefer stacking before truncation.
- Content below the header must scroll independently when needed.
- Modal width should be constrained with 16dp side gutters on phones and a max width on tablets.

Visual language:

- background: white or very light gray;
- buttons: light gray with subtle border;
- corners: 16-28dp for modal/header controls, matching the source rounded feel;
- typography: large, bold Russian labels for header actions;
- divider or spacing between header/action area and filter/list area.

## Header Actions

### `Экспорт Библиотеки`

- Starts Library export flow.
- Android v2 target is ZIP export with `manifest.json`, `library/library.json`, audio files, and `metadata/missing_audio.json`.
- Must use Android Storage Access Framework or share sheet when UI is implemented.
- Must show success/failure state.
- Must warn when export is partial because audio is missing.

### `Импорт Библиотеки`

- Opens import flow.
- If import is not implemented, show disabled or explicit `coming later` state.
- Must not silently fail.
- Future import order: Android ZIP first, old web JSON compatibility later.

### `Обновить`

- Refreshes library list from the local Room repository.
- Preserves current filters where possible.
- Shows loading/error state if refresh fails.

### `Закрыть`

- Closes Library v3 and returns to Classic Mode.
- Unsaved filter changes do not require confirmation.
- Metadata editor owns its own unsaved-change confirmation/cancel behavior.

## Search And Filter Controls

Reference: [UI-V4-01](v4/1.%20library_v3.PNG).

The filter area must include controls in this exact order:

1. Search field with placeholder `Поиск (PRO): название / тема / ссылка / уровень / теги (#tag)`.
2. Level selector default `Все уровни`.
3. Tags mode selector default `Теги: ALL`.
4. Search scope selector default `Поиск: Тексты`.
5. Sort selector default `Сорт: Последние открытые`.
6. Primary green action `Сохранить текущую таблицу`.
7. Tag chips cloud.
8. Reset button `Сбросить`.

Native Android adaptation:

- Use a vertical single-column layout on phones.
- Each selector must look like a large rounded field with a chevron.
- Focus state must be visible with a blue/green outline.
- Text field and selectors must be touch-safe and at least 48dp high.
- The long search placeholder may scroll horizontally inside the field or reduce font size within a minimum readable size; it must not break the container.

## Dropdown Menu Behavior

Dropdowns may be implemented as `DropdownMenu`, modal selector sheet, or custom popup. The chosen component must preserve the screenshot interaction: current field stays in place, a selectable overlay appears, selected item has a checkmark, and the rest of Library v3 remains visually behind the selector.

### Level Selector

Reference: [UI-V4-02](v4/2.%20library_v3_level.PNG).

Options:

```text
Все уровни
alef
alef+
bet
bet+
gimel
gimel+
dalet
dalet+
he
he+
vav
```

Behavior:

- Selected item shows a checkmark.
- Menu is scrollable.
- Menu has max height and must not cover the entire screen.
- It overlays above current state and must not push layout down.
- Text is large and readable.
- A scrollbar or scroll affordance is visible when options exceed menu height.

### Tags Mode Selector

Reference: [UI-V4-03](v4/3.%20library_v3_tag.PNG).

Options:

```text
Теги: ALL
Теги: ANY
```

Behavior:

- `ALL`: text must match all selected tags.
- `ANY`: text may match any selected tag.
- Selected item shows a checkmark.
- Menu is compact and overlays the filter area without layout shift.

### Search Scope Selector

Reference: [UI-V4-04](v4/4.%20library_v3_search.PNG).

Options:

```text
Поиск: Тексты
Поиск: Тексты + заметки + строки
Поиск: Только строки
Поиск: Только заметки
```

Behavior:

- Selected item shows a checkmark.
- Scope controls where search text is applied.
- Notes search is pending until Room has notes metadata.
- Pending scopes must be disabled with an explicit unavailable state or fully supported by repository query.
- UI must not silently ignore selected scope.

### Sort Selector

Reference: [UI-V4-05](v4/5.%20library_v3_sort.PNG).

Options:

```text
Сорт: Последние открытые
Сорт: Последние изменённые
Сорт: Название A→Я
Сорт: Уровень A→Я
Сорт: Тема A→Я
```

Behavior:

- Selected item shows a checkmark.
- Sorting is deterministic.
- Sorting should be implemented in repository/query layer when possible.
- Selected sort is preserved across refresh.
- Future filter preference persistence must be documented when implemented.

## Tag Chips Cloud

References: [UI-V4-01](v4/1.%20library_v3.PNG), [UI-V4-06](v4/6.%20library_v3_list_cards.PNG).

Example chips:

```text
#hitlist.mako 75
#song 8
#fix4 2
#smoke 2
#verb 2
#verbs 2
#ВАЖНО 1
#ульпан 1
#FaceBook 1
#grammar 1
#materials 1
#ulpan 1
```

Behavior:

- Chips are tappable.
- Tapping applies/removes selected tag filter.
- Count shows matching text count for that tag.
- Chips wrap to multiple lines and never overflow horizontally.
- Selected chip state is visually distinct.
- If no tags exist, show an empty state such as `Теги пока отсутствуют`.

Visual rules:

- pill-shaped chip;
- blue tag text;
- gray count;
- light border/background;
- compact spacing between chips.

## Save Current Table

Button: `Сохранить текущую таблицу`.

Behavior:

- Saves current Classic Mode generated table into local Room library.
- If no generated table exists, show disabled state or explicit message.
- On success, refresh Library v3 list and show confirmation.
- Duplicate behavior follows [Local Library Plan](../ANDROID_V2_LOCAL_LIBRARY_PLAN.md).
- Button is green and visually primary.

## Reset Filters

Button: `Сбросить`.

Behavior:

- Resets search text, level, tag mode, selected tags, search scope, and sort to defaults.
- Must not delete library items.
- Must not clear current generated table.
- Refreshes visible library list after reset.

## Library List / Card Layout

Reference: [UI-V4-06](v4/6.%20library_v3_list_cards.PNG).

The list must show loaded count and timestamp:

```text
Загружено: 200 · 25.04.2026, 17:07
```

Each card must include:

- numeric position, for example `1.`;
- title with mixed Hebrew/Russian/Latin text;
- topic badge, for example `lyrics`;
- source/link icon;
- progress line: `Прогресс: строка № —`;
- source label: `Источник:`;
- source URL as clickable/copyable text;
- copy button/icon for source;
- dates:
  - `Последнее открытие: ...`;
  - `Создан: ...`;
- tag chips.

Native Android adaptation:

- Use a Compose `Card`.
- Preserve visual hierarchy and large readable text.
- Hebrew in title must render correctly.
- Long URLs must not break card layout; use ellipsis plus copy action.
- Cards must not be clipped.
- Card content must not slide under action buttons.

## Library Card Action Buttons

Reference: [UI-V4-07](v4/7.%20library_v3_action_cards.PNG).

Each card must provide actions in this order:

```text
Открыть
Продолжить
Изменить
В архив
Удалить
```

Visual meaning:

- `Открыть`: blue primary action.
- `Продолжить`: green action.
- `Изменить`: gray neutral action.
- `В архив`: gray neutral action.
- `Удалить`: destructive red/pink action.

Behavior:

- `Открыть`: opens saved text/table in Classic Mode, updates last opened timestamp, and does not regenerate automatically unless future behavior explicitly requires it.
- `Продолжить`: opens saved text and continues from saved progress/current row if progress metadata exists. If progress metadata is absent, either behaves exactly like `Открыть` with a visible note or shows `progress not available`.
- `Изменить`: opens text-level metadata editor and must not modify row content.
- `В архив`: marks text archived without deleting data.
- `Удалить`: destructive, requires confirmation, and must not delete unrelated shared audio assets.

Android layout rule:

The source web screenshot shows actions on the right and clipped off-screen. Android must preserve action set, order, and colors, but must not reproduce clipping.

Allowed layouts:

- Wide phone / landscape: content left, vertical action rail right.
- Narrow phone: stacked full-width buttons below content.
- Narrow phone alternative: trailing overflow action opens an action sheet with the same order and colors.

Acceptance criterion: all actions are reachable without horizontal scrolling and no action is clipped off-screen.

## Metadata Editing Screen / Dialog

Reference: [UI-V4-08](v4/8.%20library_v3_metadata_cards_%D0%B8%D0%B7%D0%BC%D0%B5%D0%BD%D0%B8%D1%82%D1%8C.PNG).

When user taps `Изменить`, open a text-level metadata editor.

Required title: `Метаданные текста`.

Required top action: `Закрыть`.

Fields in exact order:

1. `TITLE*`
   - required;
   - example `Position 1. כולם גנבים - אושר כהן`.
2. `LEVEL`
   - dropdown/select;
   - empty example `—`.
3. `TAGS`
   - comma-separated tags;
   - example `hitlist.mako`.
4. `SOURCE`
   - URL/source text;
   - example `https://www.youtube.com/watch?v=...`.
5. `TEMA`
   - topic/theme;
   - example `lyrics`.

Bottom actions:

```text
Cancel
Save
```

Helper text:

```text
Tags вводите через запятую. Пустое поле = очистка значения.
```

Behavior:

- `Save` validates required title.
- `Cancel` discards unsaved changes.
- `Закрыть` behaves like cancel if no changes exist; if changes exist, ask confirmation or discard only through documented behavior.
- Save updates local Room text metadata and refreshes the visible card.
- Metadata editing must not regenerate rows.
- Metadata editing must not modify row text.
- Keyboard must not hide Save/Cancel.
- Dialog/screen must scroll when keyboard is visible.

Native Android adaptation:

- On phones, prefer full-screen dialog or modal bottom sheet when vertical space is limited.
- Keep large rounded fields and premium visual style.
- Preserve field labels and order exactly.
- Mixed Hebrew/Latin text must render correctly.

## Credential Attachment And Validation UI

The Android v2 target UX is provider-specific:

- `gcp_translate` and `google_online_tts`: JSON service-account attachment and validation through Android file picker / Storage Access Framework.
- `gemini_legacy`: pasted single-line Gemini API key as the primary UX, because the source prototype and real Gemini key shape are plain `AIza...` keys.

Required flow:

```text
Settings
  -> Provider credentials
  -> Select provider
  -> Attach JSON file or paste Gemini API key
  -> Validate
  -> Store securely
  -> Show masked status
```

Affected providers:

- `gcp_translate`: service-account JSON;
- `google_online_tts`: service-account JSON;
- `gemini_legacy`: single-line API key. The provider-specific JSON wrapper remains accepted only for backward compatibility.

Backward-compatible Gemini wrapper:

```json
{
  "provider": "gemini_legacy",
  "api_key": "..."
}
```

Validation:

- parse JSON for service-account providers and parse a single-line key for Gemini;
- validate required fields for the selected provider;
- reject wrong provider type;
- reject malformed JSON;
- reject empty credential file;
- reject blank/multiline Gemini API keys;
- show masked credential summary;
- optionally perform provider health check when network is available;
- do not log secrets;
- do not export secrets;
- do not store raw file path as source of truth.

Storage:

- Android Keystore-backed encrypted storage for extracted secret fields;
- encrypted internal app storage only if the full JSON document must be retained;
- no credentials in Room export;
- no credentials in logs;
- no credentials in Git.

Settings UI must present:

- provider name;
- current credential status: `not configured`, `attached but not validated`, `valid`, `invalid`, `expired/revoked if known`;
- `Прикрепить JSON` for service-account providers;
- `Gemini API Key` and `Сохранить API Key` for `gemini_legacy`;
- `Проверить`;
- `Удалить ключ`.

## Compose Implementation Guidance

Recommended components:

- `Scaffold`;
- `ModalBottomSheet` or full-screen route for Library v3;
- `LazyColumn` for scrollable content;
- `Card` for saved text cards;
- `OutlinedTextField` or custom rounded text field;
- `DropdownMenu` or modal selector sheet;
- `FilterChip` or custom chip;
- `AlertDialog` or custom dialog for delete confirmation;
- full-screen dialog or bottom sheet for metadata edit;
- `rememberSaveable` for transient UI state;
- ViewModel-owned state for filters, list data, selected card, and metadata draft;
- repository-backed state for saved library items.

Do not put business logic directly in Composables. Composables render state and emit events. Filtering, sorting, metadata validation, archive/delete, import/export, and credential validation belong in ViewModel/repository/provider layers.

## Proposed UI State Models

```kotlin
enum class LibraryTagsMode {
    ALL,
    ANY,
}

enum class LibrarySearchScope {
    TEXTS,
    TEXTS_NOTES_ROWS,
    ROWS_ONLY,
    NOTES_ONLY,
}

enum class LibrarySortMode {
    LAST_OPENED,
    LAST_MODIFIED,
    TITLE_ASC,
    LEVEL_ASC,
    THEME_ASC,
}

data class LibraryFilterState(
    val query: String,
    val level: String?,
    val selectedTags: List<String>,
    val tagsMode: LibraryTagsMode,
    val searchScope: LibrarySearchScope,
    val sortMode: LibrarySortMode,
)
```

Conceptual card state:

```kotlin
data class LibraryTextCardUiModel(
    val id: String,
    val position: Int,
    val title: String,
    val level: String?,
    val topic: String?,
    val sourceUrl: String?,
    val currentRowNumber: Int?,
    val createdAtLabel: String,
    val lastOpenedAtLabel: String?,
    val tags: List<String>,
    val isArchived: Boolean,
)
```

Conceptual metadata edit state:

```kotlin
data class MetadataEditUiState(
    val textId: String,
    val title: String,
    val level: String?,
    val tagsCsv: String,
    val source: String,
    val tema: String,
    val hasUnsavedChanges: Boolean,
    val validationMessage: String?,
)
```

Conceptual credential file state:

```kotlin
data class CredentialFileUiState(
    val providerId: String,
    val status: CredentialStatus,
    val attachedFileName: String?,
    val maskedSummary: String?,
    val lastValidatedAt: String?,
    val validationMessage: String?,
)
```

## Accessibility Requirements

- All touch targets are at least 48dp.
- Icon-only controls need content descriptions.
- Dropdowns announce selected value and expanded/collapsed state.
- Tag chips expose selected/unselected state.
- Destructive `Удалить` requires confirmation and announces destructive action.
- Hebrew text is selectable/readable and not mirrored incorrectly.
- Focus order follows visual order: header actions, filters, save, chips, reset, list cards.
- Color cannot be the only signal for destructive/primary action; label text remains visible.

## Android Insets And Keyboard Behavior

- Library v3 respects status/navigation bars and IME insets.
- Metadata editor must keep `Cancel` and `Save` reachable when keyboard is open.
- Long list content scrolls independently from modal header where practical.
- Dropdowns must fit within visible bounds above IME.
- Do not use horizontal scrolling to reveal critical card actions.

## Visual Tokens And Spacing

- Screen gutter: 16dp compact, 24dp large phones/tablets.
- Modal/card radius: 20-28dp for containers, 12-18dp for fields/chips/buttons.
- Field height: minimum 56dp.
- Header button height: minimum 56dp, never below 48dp.
- Primary green: close to source `#2ecc71` family.
- Primary blue: close to source action blue `#3498db` family.
- Destructive red/pink: soft red background with dark red text.
- Neutral buttons: light gray background, subtle border.
- Typography: bold labels for headers/actions, regular body text for metadata and URLs.

## Acceptance Criteria

- P014 implementation status:
  - Classic Mode main screen now exposes `📚 Библиотека` from the primary workflow and keeps Settings/IDE separate.
  - Library v3 is implemented as a Classic-owned modal/screen with header actions, filters, dropdowns, tag chips, list cards, stacked actions, delete confirmation, and metadata editor.
  - The metadata editor persists `TITLE*`, `LEVEL`, `TAGS`, `SOURCE`, and `TEMA` through Room schema version 2.
  - `Открыть` and `Продолжить` load saved rows back into Classic Mode and mark the text opened.
  - Settings enables target JSON credential actions while keeping interim single-line key input for legacy smoke tests.
  - Manual screenshots, keyboard evidence, actual SAF JSON attachment, export/import header flows, source clipboard copy, and Compose UI tests remain required follow-up evidence/work.

- Screenshots are copied into `docs/ui/v4`.
- This spec references all copied screenshots.
- [Classic Mode UI Spec](../ANDROID_V2_CLASSIC_MODE_UI_SPEC.md) links here.
- [Migration Plan](../ANDROID_V2_MIGRATION_PLAN.md) tracks Library v3 v4 screenshot parity in M7/M8/M12.
- [Patch Register](../ANDROID_V2_PATCH_REGISTER.md) records this documentation patch.
- [Settings and Secrets](../ANDROID_V2_SETTINGS_AND_SECRETS.md) states the target credential UX is service-account JSON attachment for GCP/Google TTS and API-key paste for Gemini.
- [Requirements Traceability](../ANDROID_V2_REQUIREMENTS_TRACEABILITY.md) tracks Library v3 UI parity and provider-specific credential flow.
- [UI DoD Evidence](../ANDROID_V2_UI_DOD_EVIDENCE.md) has evidence slots for Library v3 filters, dropdowns, list cards, action cards, and metadata editor.
- [Risk and Gap Register](../ANDROID_V2_RISK_AND_GAP_REGISTER.md) tracks UI parity and credential JSON risks.
- No source repository files are modified.
- No secrets are added.

P017 implementation status:

- `Настройки озвучки` is split out as its own native card with source language, TTS provider, voice, speech rate, pitch, and a provider-key shortcut.
- `Настройки перевода и таблицы` is split out as its own native card with translation provider, transliteration profile, Hebrew table font, and a provider-key shortcut.
- `Результат` now exposes `Обновить` once rows exist; it opens the metadata editor and then saves/updates the Library text card.
- Row cards preserve the source prototype `Действие` column semantics without web clipping: cache marker, row play, and row note action are reachable on narrow phones.
- Row `▶` uses real selected TTS providers. Saved rows adopt generated audio into `audio_assets`/`row_audio`; unsaved rows can play temporary provider output.
- Row notes persist in Room `sentence_notes`, matching the prototype's one-note-per-`(text_id, sentence_id)` rule. The Android note editor improves the web dialog with row context, keyboard-safe scrolling, Markdown helper buttons, and explicit save/delete behavior.
- Remaining evidence gaps: manual screenshot comparison against `0. Main classic mode.JPG`, real-key TTS playback smoke, and Android ZIP export/import expansion for `sentence_notes`.

P019 implementation status:

- Classic source, voice settings, translation/table settings, and result blocks are collapsible with `Скрыть` / `Показать`, preserving the prototype's lower-noise mobile behavior.
- Google Translate Free uses the source-prototype `gtx` path with `sl=iw`, browser User-Agent, and newline batch translation before individual fallback.
- Gemini legacy uses pasted API keys and a strict JSON table prompt for segmentation, niqqud, SBL transliteration, and Russian translation.
- Row TTS cache is reused only when the stored row audio key matches the current provider/voice/language/rate/pitch/text profile.
- The final visible table column can be resized, and missing imported `translit_ru` can be displayed through a Russian phonetic fallback derived from `he_niqqud`.

P020 implementation status:

- Changing the translation provider after table generation now rebuilds the table from the current source text and clears saved-card identity.
- Gemini legacy uses pasted API keys with strict JSON table generation through `gemini-2.0-flash`.
- System fallback TTS hides unsupported voice/rate/pitch controls and reports missing Android `TextToSpeech` engine/language data explicitly.
- SBL Academic and Russian phonetic fallback transliteration are covered by source-prototype fixtures.
- Non-action table cells are selectable by touch or mouse for copy workflows.

## UI DoD Evidence Checklist

Future implementation patches must collect evidence for:

- Classic Mode `📚 Библиотека` entry visible without advanced-settings scrolling.
- Library v3 header with all four actions.
- Search field and four selectors in required order.
- Level dropdown.
- Tags mode dropdown.
- Search scope dropdown.
- Sort dropdown.
- Tag chips wrapping and selected state.
- Save current table action.
- Reset filters action.
- Library list card content.
- Card action buttons without clipping on narrow phone.
- Metadata editor with keyboard open.
- JSON credential attach/validate/delete flow.
- Hebrew RTL title/content rendering.

## Documentation Maintenance Rules

Any future patch changing Library v3 layout, filters, dropdowns, chips, cards, card actions, metadata editor, credential JSON UI, Classic navigation, export/import UI, accessibility, keyboard, or insets must update:

- this file;
- [Classic Mode UI Spec](../ANDROID_V2_CLASSIC_MODE_UI_SPEC.md);
- [UI DoD Evidence](../ANDROID_V2_UI_DOD_EVIDENCE.md);
- [Migration Plan](../ANDROID_V2_MIGRATION_PLAN.md);
- [Patch Register](../ANDROID_V2_PATCH_REGISTER.md);
- [Requirements Traceability](../ANDROID_V2_REQUIREMENTS_TRACEABILITY.md);
- [Risk and Gap Register](../ANDROID_V2_RISK_AND_GAP_REGISTER.md).

Settings/secrets UI changes must also update:

- [Settings and Secrets](../ANDROID_V2_SETTINGS_AND_SECRETS.md);
- [Provider Policy](../ANDROID_V2_PROVIDER_POLICY.md);
- [Provider Implementation Plan](../ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md);
- [Error Handling](../ANDROID_V2_ERROR_HANDLING.md).

Do not wait for a manual documentation request. UI behavior changes with stale docs are incomplete.

## Related Docs

- [Classic Mode UI Spec](../ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [Migration Plan](../ANDROID_V2_MIGRATION_PLAN.md)
- [Settings and Secrets](../ANDROID_V2_SETTINGS_AND_SECRETS.md)
- [UI DoD Evidence](../ANDROID_V2_UI_DOD_EVIDENCE.md)
