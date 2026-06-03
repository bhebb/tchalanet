# PageModel Convention

> Status: DRAFT v0.1  
> Scope: dynamic page payloads, widget renderer, actions

## Rule

PageModel describes page structure. It does not carry i18n bundles, theme definitions, settings payloads, or authorization rules.

Runtime bootstrap loads PageModel alongside settings, i18n, and theme as separate concerns.

## Placement

Target placement:

```text
apps/tch-portal/src/app/core/pagemodel/
apps/tch-portal/src/app/shared/ui/
apps/tch-portal/src/app/features/<surface>/
```

Use `core/pagemodel` for:

- API client;
- renderer orchestration;
- widget registry;
- action dispatch boundaries;
- fallback/error handling.

Use feature folders for page-specific containers and widget implementations when a widget is born from a real feature.

## Payload Rules

PageModel may include:

```text
page key
sections
widgets
layout hints
i18n keys
action descriptors
widget input props
```

PageModel must not include:

```text
translation maps
theme tokens or presets
settings maps
raw permissions as frontend authority
backend domain objects not shaped for display
```

## Text

Use i18n keys:

```text
titleKey
subtitleKey
labelKey
emptyStateKey
```

The renderer resolves keys through the current i18n runtime.

## Theme And Settings

Widgets consume active theme through CSS variables and consume settings through runtime state/helpers.

A PageModel may reference a feature flag key, but it must not include the flag value. The frontend runtime decides whether the flagged part renders.

## Actions

Actions are descriptors. The renderer routes them to known handlers:

```text
link
route
command
externalLink
```

Unknown action types render disabled or are omitted, depending on the widget contract.

## Failure Behavior

If PageModel loading fails:

- public routes show a fallback page or error panel;
- private routes show a recoverable loading/error state;
- the app shell remains usable when possible.

## Anti-Patterns

Do not:

- let arbitrary backend widget types instantiate Angular classes directly;
- put business mutations in generic widget renderers;
- let PageModel decide auth;
- mix PageModel response with bootstrap payloads into one giant API response;
- create a widget library before a real page needs the widget.
