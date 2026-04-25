# ADR-0003: Provider Allowlist for Android v2

Date: 2026-04-25
Status: Accepted

## Context

The source app includes server, sidecar, local, and experimental providers. Android v2 must not depend on Railway, localhost services, Python sidecars, Web/WASM runtimes, or desktop flows.

## Decision

Runtime providers are allowlisted only:

- `google_translate_free`
- `gcp_translate`
- `gemini_legacy`
- `google_online_tts`
- `system_or_browser_fallback_low_quality`

## Consequences

- Blocked providers may be documented as history but not wired into runtime.
- Adding a provider requires policy, code allowlist, fake implementation, contract tests, UI labels, and docs.
- Silent fallback for quota/billing/invalid-key is forbidden.

## Alternatives considered

- Port all web providers: rejected because it would import sidecar and server assumptions.
- Open plugin model now: rejected until core Classic workflow is stable.

## Follow-up

Keep `AndroidV2ProviderPolicy` and provider docs synchronized.
