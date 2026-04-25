# Android v2 UI DoD Evidence

Date: 2026-04-25

This file is updated whenever UI changes. M2 has code/test evidence but no emulator screenshot evidence yet.

| Area | Status | Date | Device/emulator | Build variant | Evidence path or written result | Known issues | Follow-up patch |
|------|--------|------|-----------------|---------------|----------------------------------|--------------|-----------------|
| Classic Mode portrait | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | M0 shell compiled only. | Needs visual verification. | M12 |
| Classic Mode landscape | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | M0 shell compiled only. | Needs visual verification. | M12 |
| Long Hebrew text | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | Scroll behavior not manually captured. | Needs long text fixture. | M12 |
| Keyboard behavior | Not collected yet: no UI milestone has been implemented after M0. | 2026-04-25 | Not run | Debug | IME behavior not manually captured. | Needs device/emulator check. | M12 |
| Generated rows | Code evidence collected; manual visual evidence pending. | 2026-04-25 | Not run | Debug | `ClassicModeViewModelTest` verifies fake row generation; `AppRoot` renders generated row cards. | Needs emulator screenshot/manual portrait check. | M12 |
| Row editing | Not collected yet: editing is not implemented. | 2026-04-25 | Not run | Debug | No editing sheet yet. | M8 work required. | M8 |
| TTS controls | Not collected yet: TTS is not implemented. | 2026-04-25 | Not run | Debug | Static controls only if present. | M4/M5 work required. | M5 |
| Library screen | Partial code evidence collected. | 2026-04-25 | Not run | Debug | Classic Mode shows saved summary count from `LibraryRepository`; full library screen is not implemented. | M7 work required. | M7 |
| Export flow | Not collected yet: export UI is not implemented. | 2026-04-25 | Not run | Debug | No SAF/share flow yet. | M6 work required. | M6 |
| Error states | Partial code evidence collected. | 2026-04-25 | Not run | Debug | Blank input and duplicate save messages covered by `ClassicModeViewModelTest`. | Provider/database/export error UI still pending. | M13 |
| IDE Mode experimental screen | Not collected yet: M0 shell only. | 2026-04-25 | Not run | Debug | Needs visible experimental label verification. | Keep separate from Classic. | M10 |

## Related Docs

- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [QA Test Strategy](ANDROID_V2_QA_TEST_STRATEGY.md)
