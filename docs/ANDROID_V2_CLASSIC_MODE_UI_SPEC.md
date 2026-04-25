# Android v2 Classic Mode UI Specification

Date: 2026-04-25

## Navigation

- Bottom/top mode switch with `Classic` first and `IDE` second.
- Classic Mode is the default first launch destination.
- IDE Mode is reachable but labeled `Experimental / under development`.

## Classic Mode Layout

- Scrollable single-column phone layout.
- Primary source text field supports multiline Hebrew input and RTL display.
- Provider controls are visible before generation:
  - translation provider
  - TTS provider
  - provider status/key state
- Primary actions:
  - `Generate table`
  - `Speak`
  - `Save`
  - `Export ZIP`

## Generated Result Display

- Use row cards/list by default on phones.
- Preserve table semantics through fields:
  - Hebrew original
  - Hebrew niqqud
  - SBL transliteration
  - Russian phonetic transliteration when present
  - Russian translation
- Wide screens may add table mode later, but narrow screens must not depend on cramped web-table layout.

## Editing Behavior

- Row edit opens a touch-safe sheet/dialog.
- Editable fields: `he_plain`, `he_niqqud`, `translit`, `translit_ru`, `ru`.
- First edit stores original values in edit metadata for reset.
- Editing Hebrew/niqqud marks row audio stale.

## TTS Controls

- Full text playback near source/result.
- Per-row playback on each row card.
- Show audio state: missing, generating, ready, stale, failed.
- System fallback must be labeled low quality.

## Library Navigation

- Save creates a local Room/SQLite library item.
- Saved text reloads must preserve stable text/row IDs, rows, edits, audio metadata and provider provenance.
- Library list supports search/filter later; first shell must keep the entry point visible.

## Export Entry Point

- Export is a first-class action from Classic and Library.
- Export ZIP must remain reachable after a text is saved.
- Show export success/failure and missing-audio warnings.

## IDE Mode Placement

- IDE Mode is a secondary tab/screen.
- It must not dominate Classic Mode.
- It must show experimental status clearly.
- It reuses shared typed models and provider boundaries.

## Accessibility

- Touch targets should be at least 48dp.
- Icon-only controls need content descriptions when icons are introduced.
- Long content scrolls.
- Hebrew text uses RTL alignment.
- Loading, empty and error states are visible.
- Dynamic font scaling must not hide primary actions.

## UI DoD Evidence Checklist

- [ ] Phone portrait: Classic screen renders, text input visible, actions reachable.
- [ ] Phone landscape: content scrolls and actions remain reachable.
- [ ] Long Hebrew text: no clipped fields, rows remain readable.
- [ ] Keyboard: source input and generate action remain accessible.
- [ ] Loading/empty/error states visible.
- [ ] IDE Mode marked experimental.
