# Android v2 IDE Mode Experimental Strategy

Date: 2026-04-25

## Why IDE Mode Exists

IDE Mode preserves room for advanced study workflows from the web prototype: structured workspaces, deeper editing, diagnostics, and future authoring tools. It is not the production workflow for the first premium milestone.

## Included Now

- Separate mode entry.
- Experimental labeling.
- Shared domain models only.
- No independent provider policy.
- No Classic Mode data ownership bypass.

## Deferred

- Web prototype parity.
- Complex panel layout.
- Browser APIs.
- WebView bridge.
- Desktop-only sidecars.
- Provider experimentation outside allowlist.

## Stability Rules

- IDE Mode must not block M1-M9.
- IDE Mode must not mutate library data through private paths.
- IDE Mode must not introduce dependencies that Classic Mode does not need.
- IDE Mode must use shared repositories and provider contracts when it becomes functional.

## UI Labeling

The UI must display an experimental label and must not present IDE Mode as the recommended path for normal study generation.

## Future Roadmap

- M10: define minimal experimental workspace shell.
- After M14: evaluate whether IDE workflows belong in the premium product or remain developer/advanced tools.

## Test Expectations

- Navigation to IDE Mode does not reset Classic state.
- IDE Mode label is visible.
- IDE Mode cannot select blocked providers.

## Related Docs

- [Premium Product Target](ANDROID_V2_PREMIUM_PRODUCT_TARGET.md)
- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [ADR-0004](adr/0004-classic-mode-primary-ide-mode-experimental.md)
