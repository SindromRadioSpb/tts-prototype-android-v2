# PROMPT FOR CODEX — Classic Mode Android v2 Mobile UI Specification Based on v4 Screenshots

You are operating as a senior Android UI/UX engineer, Jetpack Compose engineer, and product migration architect.

Your task is to create a detailed implementation-ready UI/UX specification for the Android v2 mobile application based on the current web Classic Mode and Library v3 screenshots.

This task is documentation/specification first.

Do not implement the full UI code in this task unless explicitly required later.

You must update the existing Android v2 documentation control plane so that future UI migration patches can implement the Android interface with strong visual and behavioral parity to the source web product.

---

# 0. Repositories

Source web repository:

```text
E:\projects\tts-prototype-android
```

Target Android v2 repository:

```text
E:\projects\tts-prototype-android-v2
```

Work only in the target repository:

```powershell
cd E:\projects\tts-prototype-android-v2
```

Do not modify the source repository.

Use the source repository only as behavioral and visual reference.

All command-line instructions must be PowerShell-compatible.

---

# 1. Screenshot Source Files

The UI reference screenshots are located in the source repository:

```text
E:\projects\tts-prototype-android\Picture UI\v4\0. Main classic mode.JPG
E:\projects\tts-prototype-android\Picture UI\v4\1. library_v3.PNG
E:\projects\tts-prototype-android\Picture UI\v4\2. library_v3_level.PNG
E:\projects\tts-prototype-android\Picture UI\v4\3. library_v3_tag.PNG
E:\projects\tts-prototype-android\Picture UI\v4\4. library_v3_search.PNG
E:\projects\tts-prototype-android\Picture UI\v4\5. library_v3_sort.PNG
E:\projects\tts-prototype-android\Picture UI\v4\6. library_v3_list_cards.PNG
E:\projects\tts-prototype-android\Picture UI\v4\7. library_v3_action_cards.PNG
E:\projects\tts-prototype-android\Picture UI\v4\8. library_v3_metadata_cards_изменить.PNG
```

Copy these screenshots into the Android v2 documentation UI folder.

Create this target folder if it does not exist:

```text
E:\projects\tts-prototype-android-v2\docs\ui\v4
```

Use PowerShell-safe copying.

Example:

```powershell
cd E:\projects\tts-prototype-android-v2

New-Item -ItemType Directory -Force -Path .\docs\ui\v4 | Out-Null

Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\0. Main classic mode.JPG" ".\docs\ui\v4\0. Main classic mode.JPG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\1. library_v3.PNG" ".\docs\ui\v4\1. library_v3.PNG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\2. library_v3_level.PNG" ".\docs\ui\v4\2. library_v3_level.PNG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\3. library_v3_tag.PNG" ".\docs\ui\v4\3. library_v3_tag.PNG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\4. library_v3_search.PNG" ".\docs\ui\v4\4. library_v3_search.PNG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\5. library_v3_sort.PNG" ".\docs\ui\v4\5. library_v3_sort.PNG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\6. library_v3_list_cards.PNG" ".\docs\ui\v4\6. library_v3_list_cards.PNG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\7. library_v3_action_cards.PNG" ".\docs\ui\v4\7. library_v3_action_cards.PNG" -Force
Copy-Item "E:\projects\tts-prototype-android\Picture UI\v4\8. library_v3_metadata_cards_изменить.PNG" ".\docs\ui\v4\8. library_v3_metadata_cards_изменить.PNG" -Force
```

After copying, verify:

```powershell
Get-ChildItem .\docs\ui\v4
```

---

# 2. Existing Project State

Android v2 already has:

- Kotlin + Jetpack Compose project skeleton.
- Classic Mode shell.
- Experimental IDE Mode shell.
- Room local library storage implemented.
- Classic Mode functional flow shell implemented.
- Translation provider layer implemented.
- TTS/audio provider contracts implemented.
- Documentation control plane already exists.

Current migration status includes:

- M0 Foundation complete.
- M1 Local Room library storage complete.
- M2 Classic Mode functional flow complete.
- M3 Translation providers complete.
- M4 TTS/audio provider contracts complete.
- Next implementation milestone is M5 Audio storage and playback.

This task adds detailed UI/UX specification for Classic Mode mobile Library v3 and related metadata/settings behavior. It must be reflected in the migration roadmap and UI documentation.

---

# 3. Main Objective

Create a detailed technical specification and UI/UX implementation spec for the Android v2 Classic Mode mobile interface, based on the v4 screenshots.

The specification must allow future Codex implementation patches to reproduce the same product experience as the current web Classic Mode and Library v3, while adapting it correctly to native Android and Jetpack Compose.

The goal is visual/behavioral parity, not a blind pixel clone that preserves web layout bugs.

The Android version must preserve:

- interaction model;
- hierarchy;
- labels;
- control order;
- filtering behavior;
- card behavior;
- metadata editing behavior;
- import/export/library flow;
- premium mobile feel.

At the same time, Android must fix mobile-specific overflow/clipping problems where the web layout is visibly constrained.

---

# 4. Required New Documentation File

Create:

```text
docs/ui/ANDROID_V2_CLASSIC_MODE_MOBILE_V4_UI_SPEC.md
```

This must be the main detailed UI specification based on the screenshots.

The document must include:

1. Screenshot inventory.
2. Navigation flow.
3. Classic Mode main screen entry point.
4. Library v3 modal/screen layout.
5. Library header/action panel.
6. Search and filter controls.
7. Dropdown menu behavior.
8. Tag chips behavior.
9. Save current table action.
10. Reset behavior.
11. Library list/card layout.
12. Card action buttons.
13. Metadata editing screen/dialog.
14. JSON credential file attachment/validation UI requirement.
15. Compose implementation guidance.
16. Accessibility requirements.
17. Android insets/keyboard behavior.
18. Visual tokens and spacing.
19. Acceptance criteria.
20. UI DoD evidence checklist.
21. Documentation maintenance rules.

---

# 5. Required Updates To Existing Docs

Update these existing docs:

```text
docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md
docs/ANDROID_V2_MIGRATION_PLAN.md
docs/ANDROID_V2_PATCH_REGISTER.md
docs/ANDROID_V2_REQUIREMENTS_TRACEABILITY.md
docs/ANDROID_V2_UI_DOD_EVIDENCE.md
docs/ANDROID_V2_SETTINGS_AND_SECRETS.md
docs/ANDROID_V2_RISK_AND_GAP_REGISTER.md
docs/README.md
```

If any of these files do not exist, create them or add a clearly marked section in the closest existing equivalent document.

The new UI spec must be cross-linked from `docs/README.md`.

The migration plan must mention that Classic Mode mobile UI parity based on v4 screenshots is now a tracked UI implementation requirement.

The patch register must include this documentation patch.

The risk/gap register must include risks around:

- pixel parity vs native Android adaptation;
- dropdown menu behavior on small screens;
- card actions clipping/overflow;
- JSON credential file handling;
- metadata dialog keyboard behavior;
- Hebrew RTL rendering;
- stale documentation risk.

---

# 6. Required UI Specification Content

## 6.1 Screenshot inventory

In `docs/ui/ANDROID_V2_CLASSIC_MODE_MOBILE_V4_UI_SPEC.md`, create a table:

| ID | Source file | Copied doc path | Screen / state | Purpose |
|---|---|---|---|---|

Include all screenshots:

```text
0. Main classic mode.JPG
1. library_v3.PNG
2. library_v3_level.PNG
3. library_v3_tag.PNG
4. library_v3_search.PNG
5. library_v3_sort.PNG
6. library_v3_list_cards.PNG
7. library_v3_action_cards.PNG
8. library_v3_metadata_cards_изменить.PNG
```

Each copied doc path must point to:

```text
docs/ui/v4/<same filename>
```

---

## 6.2 Navigation flow

Document the required navigation flow:

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

Classic Mode remains the primary workflow.

Library v3 is part of Classic Mode, not IDE Mode.

IDE Mode must not own this library workflow.

---

## 6.3 Classic Mode main screen entry point

Based on:

```text
docs/ui/v4/0. Main classic mode.JPG
```

Specify:

- Classic Mode must have a visible `📚 Библиотека` entry point.
- The Library entry point must be reachable without scrolling through unrelated advanced settings.
- It should open the Library v3 screen/modal.
- It must not navigate to IDE Mode.
- The Library button must remain available after table generation and after save.

If the current Compose shell lacks this entry point, future implementation must add it.

---

## 6.4 Library v3 screen/modal layout

Based on:

```text
docs/ui/v4/1. library_v3.PNG
```

Specify that Library v3 must visually behave as a modal/sheet/screen with:

- rounded white container;
- dimmed or separated background when opened as modal;
- top header area;
- title on the left: `Библиотека (v3)`;
- action buttons on the right/top area:
  - `Экспорт Библиотеки`
  - `Импорт Библиотеки`
  - `Обновить`
  - `Закрыть`

Native Android adaptation rule:

- On compact phones, buttons may be stacked vertically as in the screenshot.
- The header must not clip.
- Buttons must be at least 48dp high.
- Text must not be truncated unless absolutely unavoidable.
- Content below header must scroll independently if needed.

The exact visual language should preserve:

- white/very light background;
- light gray buttons;
- rounded corners;
- large bold text;
- clear separation between header/action area and filter/list area.

---

## 6.5 Header action buttons

Document behavior:

### `Экспорт Библиотеки`

- Starts Library export flow.
- Android v2 target: ZIP export with manifest + library JSON + audio files/missing audio report.
- Must use Android Storage Access Framework or share sheet when implemented.
- Must show success/failure.
- Must warn if audio is missing.

### `Импорт Библиотеки`

- Opens import flow.
- If import is not implemented yet, show visible disabled or “coming later” state.
- Must not silently fail.
- Future import must support Android ZIP first, old web JSON compatibility later.

### `Обновить`

- Refreshes library list from local Room repository.
- Must preserve current filters if possible.
- Must show loading/error state if refresh fails.

### `Закрыть`

- Closes Library v3 and returns to Classic Mode.
- If unsaved filter changes exist, no confirmation is required.
- If metadata editing is open with unsaved changes, that dialog owns its own confirmation/cancel behavior.

---

## 6.6 Search and filter controls

Based on:

```text
docs/ui/v4/1. library_v3.PNG
```

The filter area must include controls in this order:

1. Search field:
   - placeholder:
     ```text
     Поиск (PRO): название / тема / ссылка / уровень / теги (#tag)
     ```
2. Level selector:
   - default:
     ```text
     Все уровни
     ```
3. Tags mode selector:
   - default:
     ```text
     Теги: ALL
     ```
4. Search scope selector:
   - default:
     ```text
     Поиск: Тексты
     ```
5. Sort selector:
   - default:
     ```text
     Сорт: Последние открытые
     ```
6. Primary green action:
   ```text
   Сохранить текущую таблицу
   ```
7. Tag chips cloud.
8. Reset button:
   ```text
   Сбросить
   ```

Native Android adaptation:

- Use a vertical single-column layout on phones.
- Each selector must look like a large rounded field with chevron.
- Focus state should be visible.
- Text field and selectors must be touch-safe.
- Long placeholder text must not break the layout; it may scale, scroll horizontally inside the field, or be shortened only if documented.

---

## 6.7 Level dropdown behavior

Based on:

```text
docs/ui/v4/2. library_v3_level.PNG
```

The level selector must show options such as:

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
- Menu has a max height so it does not cover the whole screen.
- It overlays above the current screen state.
- It must not push the entire layout down.
- On small screens, use a Compose `DropdownMenu`, modal bottom sheet, or custom popup, but preserve the interaction experience.

Visual parity:

- dark translucent menu style is acceptable if using native popup style;
- large readable text;
- enough vertical spacing;
- visible scrollbar or scroll affordance when content exceeds menu height.

---

## 6.8 Tags mode dropdown behavior

Based on:

```text
docs/ui/v4/3. library_v3_tag.PNG
```

Options:

```text
Теги: ALL
Теги: ANY
```

Behavior:

- `ALL`: item must match all selected tags.
- `ANY`: item may match any selected tag.
- Selected item shows checkmark.
- Menu is compact.
- It overlays the filter area without breaking layout.

---

## 6.9 Search scope dropdown behavior

Based on:

```text
docs/ui/v4/4. library_v3_search.PNG
```

Options:

```text
Поиск: Тексты
Поиск: Тексты + заметки + строки
Поиск: Только строки
Поиск: Только заметки
```

Behavior:

- Selected item shows checkmark.
- Scope controls where search text is applied.
- If current Room schema does not yet support notes, document this as pending.
- Disabled/pending options must be visibly unavailable or supported by repository query later.
- Do not silently ignore selected search scope.

---

## 6.10 Sort dropdown behavior

Based on:

```text
docs/ui/v4/5. library_v3_sort.PNG
```

Options:

```text
Сорт: Последние открытые
Сорт: Последние изменённые
Сорт: Название A→Я
Сорт: Уровень A→Я
Сорт: Тема A→Я
```

Behavior:

- Selected item shows checkmark.
- Sorting must be deterministic.
- Sorting must be implemented in repository/query layer where possible.
- UI must preserve selected sort option across refresh.
- Future persistence of filter preferences should be documented if implemented.

---

## 6.11 Tag chips cloud

Based on:

```text
docs/ui/v4/1. library_v3.PNG
docs/ui/v4/6. library_v3_list_cards.PNG
```

Tag chips examples:

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
- Tapping a chip applies it as selected tag filter.
- Count shows number of matching texts for that tag.
- Chips wrap to multiple lines.
- Chips must not overflow horizontally.
- Selected chip state must be visually distinct.
- If no tags exist, show an empty state.

Visual rules:

- pill-shaped chip;
- blue tag text;
- gray count;
- light border/background;
- compact spacing.

---

## 6.12 Save current table

Button:

```text
Сохранить текущую таблицу
```

Behavior:

- Saves current Classic Mode generated table into local Room library.
- If no table exists, show disabled state or explicit message.
- If saving succeeds, update library list and show confirmation.
- If duplicate text exists, use defined duplicate behavior from local library plan.
- Button is green and visually primary.

---

## 6.13 Reset filters

Button:

```text
Сбросить
```

Behavior:

- Resets search text, level, tag mode, selected tags, search scope and sort to defaults.
- Must not delete library items.
- Must not clear current generated table.
- Must refresh visible library list after reset.

---

## 6.14 Library list/cards layout

Based on:

```text
docs/ui/v4/6. library_v3_list_cards.PNG
```

The list must show:

- loaded count and timestamp:
  ```text
  Загружено: 200 · 25.04.2026, 17:07
  ```
- one card per saved text.

Card content must include:

- numeric position:
  ```text
  1.
  ```
- title with mixed Hebrew/Russian/Latin text;
- topic badge, for example:
  ```text
  lyrics
  ```
- source/link icon;
- progress line:
  ```text
  Прогресс: строка № —
  ```
- source label:
  ```text
  Источник:
  ```
- source URL as clickable/copyable text;
- copy button/icon for source;
- dates:
  ```text
  Последнее открытие: ...
  Создан: ...
  ```
- tag chips.

Native Android adaptation:

- Use a Compose Card.
- Preserve visual hierarchy.
- Hebrew text must render correctly inside title.
- URL must not break the card layout.
- Long URLs may be ellipsized with copy action.
- Cards must not be clipped.
- Card content must not slide under action buttons.

---

## 6.15 Library card action buttons

Based on:

```text
docs/ui/v4/7. library_v3_action_cards.PNG
```

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

### `Открыть`

- Opens saved text/table in Classic Mode.
- Updates last opened timestamp.
- Does not regenerate automatically unless required by existing behavior.

### `Продолжить`

- Opens saved text and continues from saved progress/current row if progress metadata exists.
- If progress metadata is absent, behavior must be explicit:
  - either same as `Открыть`;
  - or show “progress not available”.
- Do not silently do nothing.

### `Изменить`

- Opens metadata editing screen/dialog.
- Must not modify row content directly.
- Only edits text-level metadata.

### `В архив`

- Marks text archived.
- Archived items may be hidden by default if future filter supports it.
- Must not delete data.

### `Удалить`

- Destructive.
- Requires confirmation.
- Deletes or soft-deletes according to local library policy.
- Must not accidentally remove unrelated audio if shared by another text/audio asset.

Native Android layout rule:

The web screenshot shows action buttons at the right side and partially clipped on narrow display. Android must preserve the action set and visual order, but must not reproduce clipping.

Allowed Android layouts:

1. Wide phone / landscape:
   - content left, vertical action rail right.
2. Narrow phone:
   - actions inside card as stacked full-width buttons below content;
   - or action overflow button that opens an action sheet with the same order/colors.

Acceptance criterion:

- All actions must be reachable without horizontal scrolling.
- No card action may be clipped off-screen.

---

## 6.16 Metadata editing screen/dialog

Based on:

```text
docs/ui/v4/8. library_v3_metadata_cards_изменить.PNG
```

When user taps `Изменить`, open a metadata editor.

Required title:

```text
Метаданные текста
```

Required top action:

```text
Закрыть
```

Required fields:

1. `TITLE*`
   - required;
   - example:
     ```text
     Position 1. כולם גנבים - אושר כהן
     ```

2. `LEVEL`
   - dropdown/select;
   - example empty:
     ```text
     —
     ```

3. `TAGS`
   - text input;
   - comma-separated tags;
   - example:
     ```text
     hitlist.mako
     ```

4. `SOURCE`
   - URL/source text field;
   - example:
     ```text
     https://www.youtube.com/watch?v=...
     ```

5. `TEMA`
   - topic/theme;
   - example:
     ```text
     lyrics
     ```

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
- `Закрыть` behaves like cancel if no changes; if changes exist, ask confirmation or discard according to documented behavior.
- Saving updates local Room text metadata.
- Saving must refresh the card in Library list.
- Metadata editing must not regenerate rows.
- Metadata editing must not modify row text.
- Keyboard must not hide Save/Cancel buttons.
- Dialog/screen must scroll when keyboard is visible.

Native Android adaptation:

- On phones, prefer a full-screen dialog or modal bottom sheet if vertical space is limited.
- Keep large rounded fields and premium visual style.
- Preserve field labels and order exactly.
- Mixed Hebrew/Latin text must render correctly.

---

# 7. JSON Credential File Attachment And Validation UI

Important new requirement.

As in the current web version, provider credentials/keys must be attached and validated as JSON files, not typed manually into text fields.

The Android v2 settings UI must be updated accordingly.

This affects:

```text
docs/ANDROID_V2_SETTINGS_AND_SECRETS.md
docs/ANDROID_V2_PROVIDER_POLICY.md
docs/ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md
docs/ANDROID_V2_ERROR_HANDLING.md
docs/ANDROID_V2_REQUIREMENTS_TRACEABILITY.md
docs/ANDROID_V2_RISK_AND_GAP_REGISTER.md
```

Document this requirement clearly:

## 7.1 Credential input model

Provider credentials must be supplied through Android file picker / Storage Access Framework.

User flow:

```text
Settings
  -> Provider credentials
  -> Select provider
  -> Attach JSON file
  -> Validate
  -> Store securely
  -> Show masked status
```

Manual raw text API-key input is not the target UX.

## 7.2 Providers affected

At minimum document file-based JSON credential flow for:

```text
gcp_translate
google_online_tts
gemini_legacy
```

If Gemini legacy technically requires a simple API key, Android v2 must still use a JSON credential file wrapper unless a later ADR changes the decision.

Example Gemini JSON shape may be documented as a project-specific credential wrapper:

```json
{
  "provider": "gemini_legacy",
  "api_key": "..."
}
```

Do not hardcode this as final if current source repo proves a different schema. Mark as proposed if not yet implemented.

## 7.3 Validation behavior

When user attaches JSON:

- parse JSON;
- validate required fields for selected provider;
- reject wrong provider type;
- reject malformed JSON;
- reject empty credential file;
- show masked credential summary;
- optionally perform provider health check if network is available;
- do not log secrets;
- do not export secrets;
- do not store raw file path as the credential source of truth.

## 7.4 Storage behavior

Credential storage must use:

- Android Keystore-backed encrypted storage;
- or encrypted internal app storage if a full JSON document must be stored;
- no credentials in Room export;
- no credentials in logs;
- no credentials in Git.

## 7.5 UI requirement

Settings UI must not present large raw text boxes for API keys as the primary input.

It must present:

- provider name;
- current credential status:
  - not configured;
  - attached but not validated;
  - valid;
  - invalid;
  - expired/revoked if known;
- button:
  ```text
  Прикрепить JSON
  ```
- button:
  ```text
  Проверить
  ```
- button:
  ```text
  Удалить ключ
  ```

---

# 8. Compose Implementation Guidance

In the new UI spec, include concrete guidance for future implementation.

Recommended components:

- `Scaffold`
- `ModalBottomSheet` or full-screen route for Library v3
- `LazyColumn` for scrollable content
- `Card` for saved text cards
- `OutlinedTextField` or custom rounded text field
- `DropdownMenu` or modal selector sheet
- `FilterChip` / custom chip
- `AlertDialog` or custom dialog for delete confirmation
- full-screen dialog or bottom sheet for metadata edit
- `rememberSaveable` for UI state where appropriate
- ViewModel-owned state for filters and list data
- Repository-backed state for saved library items

State must be explicit:

```text
LibraryUiState
LibraryFilterState
LibrarySortMode
LibrarySearchScope
LibraryTagsMode
LibraryTextCardUiModel
MetadataEditUiState
CredentialFileUiState
```

Do not put business logic directly in Composables.

---

# 9. UI State Models To Document

The spec must define proposed state models, even if code is not implemented in this patch.

At minimum:

```kotlin
enum class LibraryTagsMode {
    ALL,
    ANY
}

enum class LibrarySearchScope {
    TEXTS,
    TEXTS_NOTES_ROWS,
    ROWS_ONLY,
    NOTES_ONLY
}

enum class LibrarySortMode {
    LAST_OPENED,
    LAST_MODIFIED,
    TITLE_ASC,
    LEVEL_ASC,
    THEME_ASC
}

data class LibraryFilterState(
    val query: String,
    val level: String?,
    val selectedTags: List<String>,
    val tagsMode: LibraryTagsMode,
    val searchScope: LibrarySearchScope,
    val sortMode: LibrarySortMode
)
```

Also define card and metadata edit state conceptually.

---

# 10. Acceptance Criteria

Add clear acceptance criteria.

Minimum:

- Screenshots are copied into `docs/ui/v4`.
- New UI spec exists and references all copied screenshots.
- `docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md` links to the new detailed v4 UI spec.
- `docs/ANDROID_V2_MIGRATION_PLAN.md` tracks Classic Mode Library v3 mobile UI parity as part of M7/M8/M12.
- `docs/ANDROID_V2_PATCH_REGISTER.md` includes this documentation patch.
- `docs/ANDROID_V2_SETTINGS_AND_SECRETS.md` states that keys are attached/validated as JSON files, not typed manually.
- `docs/ANDROID_V2_REQUIREMENTS_TRACEABILITY.md` includes requirements for Library v3 UI parity and JSON credential file flow.
- `docs/ANDROID_V2_UI_DOD_EVIDENCE.md` adds evidence slots for Library v3 filters, dropdowns, list cards, action cards, and metadata editor.
- `docs/ANDROID_V2_RISK_AND_GAP_REGISTER.md` tracks new UI and credential risks.
- No source code behavior changes are made unless explicitly justified.
- No secrets are added.
- Source repository is not modified.

---

# 11. Required Checks

Run:

```powershell
cd E:\projects\tts-prototype-android-v2
git status
git diff --check
```

If only docs/images are changed, Gradle build is not required.

If any Kotlin/Gradle/source file is changed, run:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

---

# 12. Commit And Push

Commit changes with a meaningful message.

Suggested commit:

```text
docs(ui): specify classic mode mobile library v4 experience
```

Push to:

```text
origin/main
```

---

# 13. Final Report Required

At the end, report:

1. Copied screenshots.
2. Created docs.
3. Updated docs.
4. New tracked requirements.
5. New risks/gaps.
6. Checks run.
7. Commit hash.
8. Push status.
9. Next recommended implementation milestone.

---

# 14. Documentation Maintenance Rule

This is mandatory.

After this task, future Codex implementation patches must keep this UI documentation current without waiting for manual developer requests.

If a future patch changes:

- Library v3 layout;
- filters;
- dropdowns;
- tag chips;
- card layout;
- card actions;
- metadata editor;
- JSON credential UI;
- Classic Mode navigation;
- export/import UI;
- accessibility behavior;
- Android keyboard/insets behavior;

then it must update:

```text
docs/ui/ANDROID_V2_CLASSIC_MODE_MOBILE_V4_UI_SPEC.md
docs/ANDROID_V2_CLASSIC_MODE_UI_SPEC.md
docs/ANDROID_V2_UI_DOD_EVIDENCE.md
docs/ANDROID_V2_MIGRATION_PLAN.md
docs/ANDROID_V2_PATCH_REGISTER.md
docs/ANDROID_V2_REQUIREMENTS_TRACEABILITY.md
docs/ANDROID_V2_RISK_AND_GAP_REGISTER.md
```

If settings/secrets UI changes, it must also update:

```text
docs/ANDROID_V2_SETTINGS_AND_SECRETS.md
docs/ANDROID_V2_PROVIDER_POLICY.md
docs/ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md
docs/ANDROID_V2_ERROR_HANDLING.md
```

The developer must not need to ask separately:

```text
Update the documentation
```

Documentation update is part of every relevant implementation patch.

A patch that changes UI behavior but leaves the UI docs stale is incomplete.
