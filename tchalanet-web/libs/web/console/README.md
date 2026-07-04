# @tch/web/console

Shared web-console capabilities used by admin and platform surfaces.

This library owns cross-surface console data-access and operational helpers. Reusable visual console components remain in `@tch/ui/console`.

## Label Pipes

Use these pipes when rendering catalog or ticketing codes in admin/platform console templates.
They are standalone Angular pipes exported by `@tch/web/console`.

Import them in the standalone component that owns the template:

```ts
import {
  ConsoleBetLabelPipe,
  ConsoleBetOptionLabelPipe,
  ConsoleBetTypeLabelPipe,
  ConsoleGameLogoTextPipe,
  ConsoleGameNamePipe,
} from '@tch/web/console';

@Component({
  standalone: true,
  imports: [
    ConsoleGameNamePipe,
    ConsoleGameLogoTextPipe,
    ConsoleBetTypeLabelPipe,
    ConsoleBetOptionLabelPipe,
    ConsoleBetLabelPipe,
  ],
})
export class ExampleComponent {}
```

### Game Codes

Use `consoleGameName` for visible game names from catalog game codes.

```html
{{ game.gameCode | consoleGameName: game.displayName }}
```

`consoleGameName` is the console/admin pipe. Prefer it on admin and platform screens because
it accepts an optional backend `displayName` and falls back through the console catalog labels.
`gameLabel` from `@tch/page-model` is the older PageModel/public pipe for i18n keys under
`catalog.game.*`, `catalog.bet_type.*`, and `catalog.option.*`; do not introduce it in new
console screens when `@tch/web/console` is already available.

Examples:

| Input               | Output          |
| ------------------- | --------------- |
| `HT_BOLET`          | `Borlette`      |
| `HT_MARYAJ`         | `Maryaj`        |
| `HT_MARYAJ_GRATUIT` | `Maryaj gratis` |
| `HT_LOTO3`          | `Loto 3`        |
| `HT_LOTO4`          | `Loto 4`        |
| `HT_LOTO5`          | `Loto 5`        |

If `displayName` is provided and is not a technical code, it wins. Unknown codes fall back
to a readable form, for example `HT_SUPER_GAME` becomes `Super Game`.

Use `consoleGameLogoText` for compact initials in logo/avatar placeholders:

```html
{{ game.gameCode | consoleGameLogoText: game.displayName }}
```

Examples: `HT_BOLET -> Bo`, `HT_LOTO3 -> L3`, `HT_MARYAJ_GRATUIT -> MG`.

### Bet Types

Use `consoleBetTypeLabel` when rendering a bet type without a specific option.

```html
{{ row.betType | consoleBetTypeLabel }}
```

Common examples:

| Input            | Output          |
| ---------------- | --------------- |
| `STRAIGHT`       | `Direct`        |
| `BOX`            | `Permuté`       |
| `FRONT_PAIR`     | `Deux premiers` |
| `BACK_PAIR`      | `Deux derniers` |
| `MARRIAGE_2D2D`  | `Maryaj 2x2`    |
| `LOTTO3_3D`      | `Loto 3`        |
| `LOTTO4_PATTERN` | `Loto 4`        |
| `LOTTO5_PATTERN` | `Loto 5`        |

The pipe normalizes case, spaces, dashes, and `*` before lookup. Unknown codes fall back to
a readable technical-code label.

### Bet Options

Use `consoleBetOptionLabel` when the backend returns both a bet type and an option number.
The first argument is the bet type; the second is the option.

```html
{{ row.betType | consoleBetOptionLabel: row.betOption }}
```

Examples:

| Bet type         | Option | Output                |
| ---------------- | ------ | --------------------- |
| `MARRIAGE_2D2D`  | `1`    | `Ordre exact`         |
| `MARRIAGE_2D2D`  | `2`    | `Revers / double`     |
| `LOTTO3_3D`      | `1`    | `Exact`               |
| `LOTTO3_3D`      | `2`    | `Permuté`             |
| `LOTTO4_PATTERN` | `3`    | `Deux premiers`       |
| `LOTTO4_PATTERN` | `4`    | `Deux derniers`       |
| `LOTTO5_PATTERN` | `1`    | `1er + 2e lot`        |
| `LOTTO5_PATTERN` | `2`    | `1er + 3e lot`        |
| `LOTTO5_PATTERN` | `3`    | `Mixte 1er/2e/3e lot` |

If the option is unknown but numeric, the pipe returns `Option N`. If the option is absent,
it returns `null`.

Use `consoleBetLabel` when the UI should prefer the option label but still show the bet type
when no option exists:

```html
{{ row.betType | consoleBetLabel: row.betOption }}
```

This is the safest default for odds/pricing rows where `betOption` may be optional.

### i18n Keys

For labels that are already stable i18n keys, use `tchLabel` from `@tch/page-model` instead
of the console catalog pipes:

```html
{{ row.labelKey | tchLabel }}
```

Examples include PageModel copy, navigation/action labels, and draw channel keys such as
`draw_channel.ca.eve.label`.
