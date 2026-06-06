# UI Styles

Shared SCSS primitives only: breakpoints, functions, mixins, typography, overlay helpers, and
global Material overrides.

This library never selects or mutates the runtime theme. Components consume global `--tch-*`
tokens and expose local `--comp-*` variables.

Import the primitives with `@use 'index'` after adding `libs/ui/styles/src/lib` to the consuming
application's SCSS include paths.
