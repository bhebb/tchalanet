# Design

## Boundaries

- PageModel renders content only: rows, columns, widgets, and dynamic widget payloads.
- Public shell renders public header, content slot, footer, and the existing mobile bottom navigation.
- Private shell renders top app bar, navigation drawer, and routed content.
- Runtime payloads are resolved and camelCase. Angular does not interpret storage bindings.

## Library responsibilities

- `libs/ui/styles` is the canonical SCSS primitive surface.
- `libs/ui/theme` owns runtime theme state, presets, token application, light/dark mode, and
  `OverlayContainer` synchronization.
- `libs/ui/components` owns reusable presentational shell components and navigation contracts.
- Existing application shell components may remain as thin composition/adaptation layers.

## Theme move

The existing `libs/shared/theme` library is already the runtime theme implementation. It will be
moved atomically to `libs/ui/theme` as the final implementation step:

- no facade and no parallel theme API;
- move implementation, generated registry, SCSS bridges, tests, and generation tool together;
- update TypeScript aliases, imports, SCSS include paths, scripts, and documentation together;
- validate runtime token application, dark mode, presets, and overlays after the move.

`ui/theme` is the runtime owner, not a component-style bucket. `ui/styles` contains compile-time
primitives only. `ui/components` consumes the resulting `--tch-*` tokens.

## Navigation contract

`ActionItem` is the only new navigation/action contract. Shared helpers distinguish route and URL
destinations and provide display text consistently. Components ignore unsupported destinations
rather than resolving backend bindings.
