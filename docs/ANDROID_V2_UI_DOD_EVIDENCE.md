# Android v2 UI DoD Evidence

Date: 2026-04-25

This file is updated whenever UI changes. M3 has code/test evidence but no emulator screenshot evidence yet.

| Area | Status | Date | Device/emulator | Build variant | Evidence path or written result | Known issues | Follow-up patch |
|------|--------|------|-----------------|---------------|----------------------------------|--------------|-----------------|
| Classic Mode portrait | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | M0 shell compiled only. | Needs visual verification. | M12 |
| Classic Mode landscape | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | M0 shell compiled only. | Needs visual verification. | M12 |
| Long Hebrew text | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | Scroll behavior not manually captured. | Needs long text fixture. | M12 |
| Keyboard behavior | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | IME behavior not manually captured. | Needs device/emulator check. | M12 |
| Generated rows | Code evidence collected; manual visual evidence pending. | 2026-04-25 | Not run | Debug | `ClassicModeViewModelTest` verifies provider-backed generation; `AppRoot` renders generated row cards with provider label. | Needs emulator screenshot/manual portrait check. | M12 |
| Row editing | Code evidence collected; manual visual evidence pending. | 2026-04-25 | Unit tests | Debug | M8 adds Library tab inline row editor, reset, up/down reorder, add row, add after, and delete row; `LibraryViewModelTest` covers edit/reset/reorder/delete/add behavior. | Needs emulator screenshot/manual keyboard and touch-target evidence. | M12/M13 |
| TTS controls | Partial code evidence collected; UI/manual evidence pending. | 2026-04-25 | Not run | Debug | M4 added TTS providers; M5 added audio storage/playback controller tests. Row buttons remain disabled in UI. | Needs device playback and UI wiring. | M7/M12 |
| Library screen | Partial code evidence collected; manual screenshot evidence pending. | 2026-04-25 | Unit tests | Debug | M7 adds Library tab with browse/open/archive/restore/delete and row preview; M8 adds row mutation controls; `LibraryViewModelTest` covers lifecycle and row edit behavior. | Add emulator screenshots/manual evidence. | M12/M13 |
| Export flow | Partial code evidence: repository ZIP export is implemented; SAF/share UI is not implemented. | 2026-04-25 | Unit tests | Debug | `LibraryZipExportRepositoryTest` verifies ZIP structure, manifest, missing-audio manifest, and export history. | Add SAF/share UI evidence when export UI is wired. | M7/M12 |
| Settings credentials | Code evidence collected; manual visual and device Keystore evidence pending. | 2026-04-25 | Unit tests | Debug | M9 adds Settings tab with provider credential status, masked values, save/delete actions, and validation; `ProviderSettingsRepositoryTest` and `SettingsViewModelTest` cover behavior. | Needs emulator screenshot and Android Keystore persistence smoke. | M12/M13 |
| Error states | Partial code evidence collected. | 2026-04-25 | Not run | Debug | Blank input, duplicate save, and provider missing-configuration messages covered by `ClassicModeViewModelTest`. | Database/export/TTS error UI still pending. | M13 |
| IDE Mode experimental screen | Not collected yet: M0 shell only. | 2026-04-25 | Not run | Debug | Needs visible experimental label verification. | Keep separate from Classic. | M10 |

## Related Docs

- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
