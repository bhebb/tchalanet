# Change: align-web-architecture-design-docs

## Why

The Web UI foundation now has explicit `ui/components`, `ui/styles`, and `ui/theme` libraries, while
the Web architecture and central design-system documentation still describe the previous target and
an outdated palette. The remaining target libraries must be planned without creating empty Nx
projects.

## What changes

- Align Web architecture documentation with the active UI libraries and the phased target.
- Document the rule that target libraries are created only with a concrete code extraction.
- Align central design-system documentation with the current Web palette, tokens, and typography.
- Record Mobile/POS alignment as a future Mobile-owned change without changing Mobile here.

## Impact

- Documentation changes in `tchalanet-web` and `tchalanet-docs`.
- No runtime code changes and no Mobile project changes.

## Non-goals

- No empty `api`, `config`, `page-model`, or `widgets` library scaffolding.
- No Mobile/POS implementation migration.

