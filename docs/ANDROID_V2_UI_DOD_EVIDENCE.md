# Android v2 UI DoD Evidence

Date: 2026-04-25

This file is updated whenever UI changes. Current evidence is code/test evidence only; emulator screenshot and device smoke evidence are still pending.

| Area | Status | Date | Device/emulator | Build variant | Evidence path or written result | Known issues | Follow-up patch |
|------|--------|------|-----------------|---------------|----------------------------------|--------------|-----------------|
| Classic Mode portrait | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | M0 shell compiled only. | Needs visual verification. | M12 |
| Classic Mode landscape | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | M0 shell compiled only. | Needs visual verification. | M12 |
| Long Hebrew text | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | Scroll behavior not manually captured. | Needs long text fixture. | M12 |
| Keyboard behavior | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | IME behavior not manually captured. | Needs device/emulator check. | M12 |
| Generated rows | Code evidence collected; manual visual evidence pending. | 2026-04-25 | Not run | Debug | `ClassicModeViewModelTest` verifies provider-backed generation; `AppRoot` renders generated row cards with provider label. | Needs emulator screenshot/manual portrait check. | M12 |
| Row editing | Code evidence collected; manual visual evidence pending. | 2026-04-25 | Unit tests | Debug | M8 adds Library tab inline row editor, reset, up/down reorder, add row, add after, and delete row; `LibraryViewModelTest` covers edit/reset/reorder/delete/add behavior. | Needs emulator screenshot/manual keyboard and touch-target evidence. | M12/M13 |
| TTS controls | Partial code evidence collected; UI/manual evidence pending. | 2026-04-25 | Unit tests | Debug | M4 added TTS providers; M5 added audio storage/playback controller tests; P012 wires Classic source-level `Speak` to selected TTS provider and verifies success/missing-configuration through `ClassicModeViewModelTest`. Row playback/adoption remains unwired. | Needs device playback, real-key network smoke, and row/text audio UI wiring. | M12/M13 |
| Library screen | Partial code evidence collected; manual screenshot evidence pending. | 2026-04-25 | Unit tests | Debug | M7 adds Library tab with browse/open/archive/restore/delete and row preview; M8 adds row mutation controls; `LibraryViewModelTest` covers lifecycle and row edit behavior. | Add emulator screenshots/manual evidence. | M12/M13 |
| Library v3 filters | Not collected yet: v4 parity spec only. | 2026-04-25 | Not run | Debug | P013 requires search field, level selector, tags mode, search scope, sort, save current table, tag chips, and reset in source order. | Implement and capture portrait screenshot. | Next UI parity patch |
| Library v3 dropdowns | Not collected yet: v4 parity spec only. | 2026-04-25 | Not run | Debug | P013 defines level, tags mode, search scope, and sort dropdown behavior based on copied v4 screenshots. | Need expanded-state screenshots and small-screen overflow check. | Next UI parity patch |
| Library v3 list cards | Not collected yet: v4 parity spec only. | 2026-04-25 | Not run | Debug | P013 defines saved text cards with title, topic badge, source URL/copy, progress, dates, and tag chips. | Need card content screenshot with long Hebrew/Russian/URL values. | Next UI parity patch |
| Library v3 action cards | Not collected yet: v4 parity spec only. | 2026-04-25 | Not run | Debug | P013 requires `Открыть`, `Продолжить`, `Изменить`, `В архив`, `Удалить` without source web clipping. | Need narrow-phone evidence that all actions are reachable. | Next UI parity patch |
| Library v3 metadata editor | Not collected yet: v4 parity spec only. | 2026-04-25 | Not run | Debug | P013 defines `Метаданные текста` editor fields and Save/Cancel/Close behavior. | Need keyboard-open screenshot and validation evidence. | Next UI parity patch |
| Export flow | Partial code evidence: repository ZIP export is implemented; SAF/share UI is not implemented. | 2026-04-25 | Unit tests | Debug | `LibraryZipExportRepositoryTest` verifies ZIP structure, manifest, missing-audio manifest, and export history. | Add SAF/share UI evidence when export UI is wired. | M7/M12 |
| Settings credentials | Code evidence collected for interim UI; target JSON UI evidence pending. | 2026-04-25 | Unit tests | Debug | M9 adds Settings tab with provider credential status, masked values, save/delete actions, and validation; P012 keyed adapters read stored credentials; P013 requires JSON attach/validate/delete UX through SAF. | Needs emulator screenshot, Android Keystore persistence smoke, real restricted-key endpoint smoke, and JSON attachment evidence. | Next settings UI patch |
| Error states | Partial code evidence collected. | 2026-04-25 | Unit tests | Debug | Blank input, duplicate save, provider missing-configuration, and source-level TTS missing-configuration messages are covered by `ClassicModeViewModelTest`. | Database/export/manual network failure UI still pending. | M13 |
| IDE Mode experimental screen | Not collected yet: M0 shell only. | 2026-04-25 | Not run | Debug | Needs visible experimental label verification. | Keep separate from Classic. | M10 |

## Related Docs

- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
