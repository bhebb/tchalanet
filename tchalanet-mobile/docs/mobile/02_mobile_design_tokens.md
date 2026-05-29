# 02 Mobile Design Tokens

> Status: normative  
> Scope: colors, typography, spacing, radii, borders, and shadows

Do not define many one-off colors. Define roles, then use the roles everywhere.

## Colors

| Role | Token | Value | Usage |
| --- | --- | --- | --- |
| Primary | `primary` | `#3525CD` | Main action: sell, verify, confirm. |
| Primary | `primaryContainer` | `#4F46E5` | Strong primary surfaces and pressed states. |
| Background | `background` | `#F7F9FB` | Screen background. |
| Surface | `surface` | `#FFFFFF` | Cards, fields, panels. |
| Surface | `surfaceMuted` | `#ECEEF0` | Muted field backgrounds and low emphasis areas. |
| Outline | `outline` | `#C7C4D8` | Light borders. |
| Success | `success` | `#006C49` | Session open, sale accepted, operation OK. |
| Success | `successContainer` | `#6CF8BB` | Positive badges and soft success backgrounds. |
| Warning | `warning` | `#B26A00` | Limit near, offline, confirmation required. |
| Error | `error` | `#BA1A1A` | Rejected, blocked, destructive action. |
| Text | `text` | `#191C1E` | Primary text. |
| Text | `textMuted` | `#464555` | Secondary text. |

Color rules:

- Red is reserved for dangerous actions and blocking errors.
- Green is reserved for success and OK status.
- Primary purple is reserved for main actions.
- Screens must use theme tokens, not hardcoded values.

## Typography

Inter is the default POS typeface.

| Style | Size | Weight | Usage |
| --- | --- | --- | --- |
| `screenTitle` | 22-24 px | semi-bold | Bonjour Marie, Vente acceptee. |
| `sectionLabel` | 10-12 px | medium | Uppercase labels: TIRAGE EN COURS. |
| `body` | 15-16 px | regular | Normal text. |
| `bodyMuted` | 13-14 px | regular | Secondary text. |
| `numeric` | 18-22 px | medium/bold | Amounts, ticket numbers, totals. |
| `heroCode` | 48-64 px | bold | Client code: 40CP-JBMR. |

Rule: amounts, codes, and numbers must be more readable than their labels.

## Spacing

Use a 4 px grid.

| Token | Value | Usage |
| --- | --- | --- |
| `space4` | 4 | Micro. |
| `space8` | 8 | Small. |
| `space12` | 12 | Compact POS gap. |
| `space16` | 16 | Standard screen margin. |
| `space24` | 24 | Section separation. |
| `space32` | 32 | Large separation. |

POS dimensions:

- Screen margin: 12 or 16 px.
- Standard gap: 12 px.
- Header height: 48-56 px.
- Primary button height: 56-72 px.
- Cart line height: 56-64 px.
- Bottom action bar height: 72-96 px.

Touch rules:

- Any touch action is at least 44 px high.
- Primary POS action is ideally 56 px or taller.

## Radius, Border, Shadow

| Token | Value |
| --- | --- |
| `radiusSmall` | 4 |
| `radiusMedium` | 8 |
| `radiusLarge` | 12 |

Rules:

- Border: 1 px outline or outline variant.
- Shadows are very light and reserved for important surfaces.
- No large shadows.
- No default nested cards.
- Fields use a border or a clear muted background.
- Primary buttons are filled.
- Secondary buttons are outline or surface.
