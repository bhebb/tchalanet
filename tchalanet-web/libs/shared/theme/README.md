# Shared Theme

Runtime theme services, generated preset registry, SCSS token bridges, and the theme generation tool.

Key folders:

- `src/runtime` - Angular services, theme types, token mapping, and the switcher component.
- `src/registry` - generated `THEME_PRESETS` registry.
- `src/scss` - Material 3 preset catalog and runtime CSS variable bridges.
- `tools` - registry generator used by `pnpm theme:generate`.
