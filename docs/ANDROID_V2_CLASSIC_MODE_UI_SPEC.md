# Android v2 Classic Mode UI/UX Master Spec

Date: 2026-04-25

Classic Mode is the production workflow. It must feel like a native Android study app, not a compressed web table.

The detailed mobile parity contract for the source web Classic Mode and Library v3 v4 screenshots is [Classic Mode Mobile v4 UI Spec](ui/ANDROID_V2_CLASSIC_MODE_MOBILE_V4_UI_SPEC.md). Future Classic/Library UI implementation must follow that document for the `📚 Библиотека` entry point, Library v3 modal/screen, filters, dropdowns, tag chips, saved-text cards, card actions, metadata editor, and JSON credential attachment UI.

## Phone Portrait Layout

Top to bottom:

1. App bar with mode switch and library/settings actions.
2. Hebrew source input card with scrollable multiline text field.
3. Provider controls as compact chips/menus.
4. Primary generation button with progress state.
5. Generated row cards in a vertical list.
6. Save/export actions near the generated result and library context.

## Phone Landscape Behavior

- Source input and generated rows may use two-pane layout when width allows.
- No row card may shrink below readable Hebrew line height.
- Keyboard must not hide the generation button or active editing field.

## Source Input

- Accepts long Hebrew text.
- Uses readable RTL rendering.
- Preserves line breaks.
- Shows validation only for empty input or unsupported size.

## Provider Controls

- Translation provider: `google_translate_free`, `gcp_translate`, `gemini_legacy`.
- TTS provider: `google_online_tts`, `system_or_browser_fallback_low_quality`.
- Blocked providers are not selectable.
- Provider chips show missing key/configuration state before generation.

## Generated Row Card

Each row card displays:

- Hebrew original, right-aligned/RTL.
- Hebrew with niqqud when available; if missing, show `Niqqud unavailable` degraded badge.
- SBL Academic transliteration.
- Russian phonetic transliteration when available.
- Russian translation.
- Provider/degraded state when row was generated.
- TTS controls and stale/missing audio indicator.
- Edit/reset/reorder actions.

M3 implementation status:

- Generated row cards are wired to `ClassicModeViewModel` and the translation provider registry.
- The row card labels the actual translation provider, for example `Translation: google_translate_free`.
- `gcp_translate` and `gemini_legacy` use secure Settings credentials when configured and otherwise show visible missing-configuration errors.
- Niqqud is shown as not generated in M3.
- Row play remains disabled until row/text audio UI wiring; edit controls are implemented in the Library tab.
- Saved summary count is shown from the local Room-backed repository.

## TTS Controls

- Row play button.
- Row regenerate button when provider configured.
- Text-level play/regenerate action.
- Low-quality fallback label for Android platform TTS.
- Missing audio state with retry/regenerate action.

P012 behavior:

- Classic `Speak` invokes the selected TTS provider for source-level Hebrew synthesis.
- `google_online_tts` reads the secure Settings credential and writes the returned MP3 under app cache for smoke validation.
- `system_or_browser_fallback_low_quality` routes through the Android platform TTS adapter.
- The `Speak` button shows `Speaking...` while synthesis is active and reports either the local file name or a mapped provider error.
- The `Speak` result is not yet adopted into `audio_assets`, `row_audio`, or `text_audio`, and it does not start audible playback automatically.

## Editing

- Use bottom sheet or dialog with fields: Hebrew, niqqud, transliteration, Russian transliteration, Russian translation.
- Save applies one row transaction.
- Reset can restore selected fields.
- Editing Hebrew or niqqud marks row audio stale immediately.
- Reorder uses drag handle or explicit move controls with 48dp touch targets.

## Loading, Empty, Error States

- Empty: prompt user to enter Hebrew text.
- Loading: show cancellable progress and selected provider.
- Provider error: show category, provider, and action.
- Database error: keep current visible table and show retry.
- Export partial: show missing audio count and export location/status.

Current M3 behavior:

- Empty Hebrew input shows a validation message and does not generate rows.
- Generate button shows `Generating...` while translation provider work is running.
- Save button is disabled until rows exist.
- Speak, row play, edit, and export actions are visibly disabled because their milestones are not implemented.
- Duplicate save returns a visible local-library conflict message and does not overwrite silently.
- Provider missing configuration and mapped provider errors are shown without silent fallback.

M7 behavior:

- Current implementation uses a separate Library tab for browse/open/archive/restore/delete and row preview.
- Target v4 parity requires `📚 Библиотека` to open Library v3 from Classic Mode as a modal/screen with the source filter/card workflow.
- Future UI work must move the production Library v3 workflow into Classic-owned navigation instead of treating it as an IDE-like separate mode.

M8 behavior:

- Library row cards now expose inline editing instead of a compressed web table.
- The row editor includes Hebrew original, Hebrew with niqqud, SBL transliteration, Russian phonetic transliteration, and Russian translation.
- Save applies a single row mutation and keeps the selected text visible.
- Reset restores fields that repository metadata marks as edited.
- Reorder uses explicit `Up` and `Down` buttons with disabled edge states; drag-and-drop remains deferred.
- `Add row`, `Add after`, and `Delete row` are available in the Library tab.
- Row cards show `Edited` and `Added` badges from `EditMeta`.
- Manual portrait/landscape and keyboard evidence is still pending in [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md).

M9 behavior:

- Settings is a separate top-level tab.
- Provider credentials show configured/missing status and masked values only.
- Credential input is single-line and rejects raw service account JSON/private-key material.
- Classic provider selection uses configured credentials for `gcp_translate`, `gemini_legacy`, and `google_online_tts`; missing credentials still surface without silent fallback.

P013 v4 UI spec decision:

- The target Settings UX for provider credentials is JSON file attachment and validation through Android Storage Access Framework, not manual raw text entry.
- The current single-line key UI is an interim implementation and must be replaced or demoted when the JSON credential flow is implemented.

P014 behavior:

- Classic Mode now owns the visible `📚 Библиотека` entry point and opens Library v3 as a modal/screen instead of using a separate production Library tab.
- The Classic main screen follows the v4 mobile hierarchy with quota/status strip, source input, provider settings, generated row cards, save action, and source-level `🔊 Озвучить`.
- Library v3 renders the v4 filter stack, tag chips, saved text cards, stacked card actions, delete confirmation, and text-level metadata editor.
- Saved text `Открыть` and `Продолжить` load rows back into Classic Mode and update `last_opened_at`.
- Text-level metadata edits persist `TITLE*`, `LEVEL`, `TAGS`, `SOURCE`, and `TEMA` into Room schema version 2.
- Settings shows target JSON credential controls as disabled future actions while the interim single-line smoke path remains available.
- Manual screenshot, landscape, IME, accessibility, and narrow-phone action evidence remain pending in [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md).
- The previous M8 row-level edit/reset/reorder repository and ViewModel behavior remains tested, but the final UI access path must be reconciled with the v4 Classic-owned Library flow before release hardening.

## Accessibility and Insets

- Touch targets at least 48dp.
- Buttons and icon controls have content descriptions.
- Text contrast meets Android accessibility expectations.
- UI handles status/navigation bars and IME insets.
- Long content scrolls without overlapping controls.

## UI DoD Evidence Checklist

- [ ] Portrait screenshot or written result recorded.
- [ ] Landscape screenshot or written result recorded.
- [ ] Long Hebrew text verified.
- [ ] Keyboard behavior verified.
- [x] Library v3 modal/filter/card/metadata implementation has code-level evidence in P014.
- [x] Row edit workflow has unit-level evidence through `LibraryViewModelTest`.
- [ ] Row edit workflow has manual device/emulator evidence.
- [ ] TTS stale/missing states verified.
- [ ] Error states verified.
- [ ] IDE Mode remains visibly experimental.

Evidence is tracked in [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md).

## Related Docs

- [Premium Product Target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md)
- [Classic Mode Mobile v4 UI Spec](ui/ANDROID_V2_CLASSIC_MODE_MOBILE_V4_UI_SPEC.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
