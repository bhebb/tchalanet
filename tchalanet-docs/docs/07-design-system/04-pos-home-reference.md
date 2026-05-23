# 04 POS Home Reference — POS_HOME_REFERENCE_V1

> Status: normative  
> Scope: POS terminal / seller home screen

This document captures the validated POS home direction.

## Layout order

```text
1. Terminal header
2. Primary sale action
3. Secondary operational actions
4. Sync status action
5. Daily sales total
6. Key metrics
7. Last transaction
8. Bottom navigation
```

## Reference screen behavior

### Header

Must show:

```text
Tchalanet
Terminal #402
Online/offline status
profile/avatar or operator affordance
menu
```

Preferred labels:

```text
Terminal #402
ONLINE
```

Avoid vague labels:

```text
ID #402
```

### Primary action

```text
VENDRE TICKET
```

Rules:

- full width;
- largest button/action card;
- blue primary;
- large ticket icon;
- uppercase allowed;
- must be immediately visible without scrolling.

### Secondary actions

```text
VÉRIFIER TICKET
PAYER GAGNANT
```

Rules:

- two-column grid;
- secondary/purple acceptable;
- must be smaller than `VENDRE TICKET`;
- `PAYER GAGNANT` is a sensitive financial operation and may later get a financial variant.

### Sync action

Example:

```text
SYNC. DONNÉES • À JOUR
```

Rules:

- sync always displays action + state;
- state must be visible without opening another screen;
- pending/offline states use warning.

### Daily sales total

Example:

```text
VENTES AUJOURD'HUI
42,850 HTG
```

Rules:

- use large numeric typography;
- amount should be readable in one glance;
- if amount is too long, split currency to second line.

Responsive behavior:

```text
42,850 HTG         preferred
1,245,850 HTG     reduce font size if needed
1,245,850         fallback
HTG
```

### Metrics

Examples:

```text
TICKETS
128

GAINS À PAYER
12,400 HTG
```

Rules:

- money values must show currency;
- `Gains à payer` may use orange because it is financial attention;
- do not use orange as decoration.

### Last transaction

Example:

```text
DERNIÈRE TRANSACTION
Borlette 3-Chif
12:44 • Ref: 8849-01
500 HTG
```

Rules:

- compact but readable;
- amount on right;
- no heavy shadow;
- use surface separation.

## POS Home design rules

- POS home is action-first.
- No marketing content.
- No long scroll.
- Main action appears above metrics.
- Sync state is part of the operational home.
- Money and transaction values are highly readable.
- Borders + soft surfaces are preferred over heavy shadows.
