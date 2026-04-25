# Android v2 Classic Mode UI/UX Master Spec

Date: 2026-04-25

Classic Mode is the production workflow. It must feel like a native Android study app, not a compressed web table.

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
- `gcp_translate` and `gemini_legacy` currently show visible missing-configuration errors until M9 secure settings.
- Niqqud is shown as not generated in M3.
- Play/edit buttons remain disabled until M4/M5 and M8.
- Saved summary count is shown from the local Room-backed repository.

## TTS Controls

- Row play button.
- Row regenerate button when provider configured.
- Text-level play/regenerate action.
- Low-quality fallback label for Android platform TTS.
- Missing audio state with retry/regenerate action.

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

- A separate Library tab is available for browse/open/archive/restore/delete and row preview.
- Classic Mode still shows the saved summary count, but detailed lifecycle actions live in the Library tab.

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
- [ ] Row edit sheet verified.
- [ ] TTS stale/missing states verified.
- [ ] Error states verified.
- [ ] IDE Mode remains visibly experimental.

Evidence is tracked in [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md).

## Related Docs

- [Premium Product Target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)
- [Audio Architecture](ANDROID_V2_AUDIO_ARCHITECTURE.md)
