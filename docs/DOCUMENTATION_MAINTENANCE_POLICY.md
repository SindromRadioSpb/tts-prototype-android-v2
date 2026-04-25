# Documentation Maintenance Policy

Date: 2026-04-25

This policy is binding for future Codex, Claude, and human implementation work.

## Required Rule

Any future implementation agent must update docs whenever it changes:

- behavior;
- architecture;
- provider policy;
- data model;
- schema;
- export format;
- UI flow;
- error behavior;
- security or secrets behavior;
- test strategy;
- release process;
- milestone status.

## No-Manual-Reminder Rule

The developer must not need to ask:

```text
Update the documentation
```

Documentation update is part of the task. If docs are stale, the implementation is incomplete.

## Patch DoD Rule

Every future patch must answer:

```text
Which docs did this patch affect?
Were they updated?
If not, why not?
```

## Commit Rule

Documentation updates should be committed together with the behavior change or in a directly adjacent docs commit.

## Status Rule

After each patch, update when relevant:

- [Patch Register](ANDROID_V2_PATCH_REGISTER.md)
- [Migration Plan](ANDROID_V2_MIGRATION_PLAN.md)
- [Risk and Gap Register](ANDROID_V2_RISK_AND_GAP_REGISTER.md)

## ADR Rule

If a meaningful architecture decision is made, add or update an ADR under [ADR Index](adr/README.md).

## UI Rule

If UI changes, update:

- [Classic Mode UI Spec](ANDROID_V2_CLASSIC_MODE_UI_SPEC.md)
- [UI DoD Evidence](ANDROID_V2_UI_DOD_EVIDENCE.md)

## Data Rule

If schema, data, or export behavior changes, update:

- [Data Model](ANDROID_V2_DATA_MODEL.md)
- [Export Spec](ANDROID_V2_LIBRARY_EXPORT_SPEC.md)

## Provider Rule

If provider behavior changes, update:

- [Provider Policy](ANDROID_V2_PROVIDER_POLICY.md)
- [Provider Implementation Plan](ANDROID_V2_PROVIDER_IMPLEMENTATION_PLAN.md)
- [Error Handling](ANDROID_V2_ERROR_HANDLING.md)

## Final Instruction

Future agents must treat this maintenance policy as binding.

## Related Docs

- [README](README.md)
- [Patch Register](ANDROID_V2_PATCH_REGISTER.md)
- [ADR-0006](adr/0006-documentation-as-living-control-plane.md)
