# UI Theme Runtime

## Responsibilities

- `ThemeStore` owns the current preset and light/dark/system mode.
- `ThemeDomApplier` applies runtime tokens and synchronizes `:root`, `.tch-theme`, and
  `OverlayContainer`.
- Theme SCSS generates Material presets and derives the global `--tch-*` token set.

## Design Decisions

1. Runtime theme behavior belongs in `ui/theme`, not `ui/styles` or components.
2. Components never hardcode theme colors; they consume `--tch-*` tokens.
3. Every reusable component exposes local `--comp-*` variables with `--tch-*` fallbacks.
4. Header, footer, card, and navigation surfaces are derived once.
5. Angular Material is harmonized through global overrides, not per-component hacks.
6. Dark mode is global through `.tch-theme.dark` and `:root[data-theme='dark']`.
7. Tenant presets may override tokens but cannot alter component logic.
8. `ui/styles` contains compile-time SCSS primitives only.
9. `ui/components` contains reusable token-consuming components.
