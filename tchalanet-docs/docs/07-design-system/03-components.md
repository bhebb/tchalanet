# 03 Components

> Status: normative  
> Scope: shared component behavior and visual rules

## Button hierarchy

| Type | Usage |
| --- | --- |
| Primary filled | Main operation: sell, confirm, validate, pay. |
| Secondary filled | Strong secondary action: verify ticket. |
| Tertiary filled | Landing CTA: Demander une démo. |
| Outlined | Secondary/alternative action. |
| Text | Low emphasis action. |
| Destructive | Cancel, void, reject, delete. |

Sur le Web, chaque composant consomme les tokens globaux `--tch-*` et expose ses points
d’adaptation locaux sous forme de variables `--comp-*` avec fallback vers `--tch-*`.

## POS ActionCard

POS uses large action cards instead of normal small buttons.

### Variants

| Variant | Color | Usage |
| --- | --- | --- |
| `primary` | `primary` | Vendre ticket. |
| `secondary` | `secondaryContainer` | Vérifier ticket. |
| `financial` | `secondary` or `primary outlined` | Payer gagnant. |
| `neutral` | `surfaceBright` + outline | Sync available. |
| `warning` | `warningContainer` | Offline, pending sync. |
| `disabled` | disabled surface | Unavailable action. |

### POS action rules

- `VENDRE TICKET` is the only full-width dominant action on POS home.
- Secondary action cards use two-column grid.
- Action cards must have large icons and readable labels.
- Action cards should not use red unless destructive.

## Sync action

Sync always shows both action and state.

Allowed labels:

```text
SYNC. DONNÉES • À JOUR
SYNC. DONNÉES • 3 EN ATTENTE
SYNC. DONNÉES • HORS LIGNE
SYNC. DONNÉES • ERREUR
```

Visual states:

| State | Style |
| --- | --- |
| À jour | neutral or subtle success |
| En attente | warning |
| Hors ligne | warning |
| Erreur | error |

## Cards

| Card type | Background | Border | Shadow |
| --- | --- | --- | --- |
| Stat card | `surfaceBright` | `outline` | `elevation1` |
| Main amount card | `surfaceBright` | `outline` | `elevation1` |
| Last transaction | `surfaceContainerHigh` | optional | `elevation0` |
| Warning metric | `surfaceBright` + tertiary left border | tertiary border | `elevation1` |

## Bottom navigation

Rules:

- Active item may use the secondary/action accent role.
- Inactive items use `onSurfaceVariant`.
- Navigation must not hide primary action.
- POS bottom nav should remain stable across key screens.

## Text fields

Rules:

- Label is required.
- Placeholder is not a label.
- Focus ring must be visible.
- Error text must be explicit.
- POS numeric inputs must use large numeric text.

## Status badges

Use semantic colors:

```text
Online = success
Offline = warning/error depending severity
Session open = success
Pending sync = warning
Blocked/rejected = error
```
