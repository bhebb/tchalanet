# Design — admin-limits-active-control-v1

## Context Packs

- `openspec/context/10-non-negotiables.md`
- `openspec/context/20-backend-rules.md`
- `openspec/context/30-frontend-rules.md`

Near-code references:

- `tchalanet-server/docs/`
- `tchalanet-server/openspec/context/10-non-negotiables.md`
- `tchalanet-web/docs/conventions/feature-playbook.md`
- `tchalanet-web/docs/conventions/error.md`
- `tchalanet-web/AGENTS.md`

## UX Model

The central Limit page is not a technical rule catalog. It is an operations view:

1. add a common limit quickly;
2. see active limits;
3. pause/remove a limit safely;
4. jump to the owner page when deeper configuration is needed.

### Page structure

```text
Limit lavant
Siveye reg ki pwoteje lavant, nimewo, tiraj ak machin yo.

[Bloke nimewo] [Plafon sou nimewo] [Limit tikè] [Limit machann]

Limit aktif

Blokaj nimewo
  No 12, 45       Texas - 10:00      Jodi a      Aktif      Modifye | Dezaktive | Efase

Plafon sou nimewo
  No 12 <= 500 G  Tout santral       Pèmanan     Aktif      Modifye | Dezaktive | Efase

Limit tikè
  Max 200 liy     Tout santral       Pèmanan     Aktif      Modifye | Dezaktive | Efase

Limit machann
  Max 50 tikè     POS-005            Jodi a      Aktif      Modifye | Dezaktive | Efase
```

Mobile layout uses stacked cards grouped by business type. Desktop can use the same grouping with denser row layout. No horizontal scrolling is required for core actions.

## Business Grouping

The UI groups rules by intent. The backend may still expose rule keys; the frontend maps them to business groups using a shared model.

| Business group | Rule keys |
|---|---|
| `NUMBER_BLOCK` | `BLOCK_SELECTION_PER_DRAW` |
| `NUMBER_CAP` | `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW`, `MAX_SALES_COUNT_PER_SELECTION_PER_DRAW` |
| `TICKET_LIMIT` | `MAX_LINES_PER_TICKET`, `MAX_STAKE_PER_LINE`, `MAX_STAKE_PER_TICKET`, `MAX_STAKE_PER_SELECTION_PER_TICKET`, `MAX_STAKE_PER_BET_TYPE_PER_TICKET`, `MAX_SALES_COUNT_PER_TICKET` |
| `SELLER_LIMIT` | `MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW`, `MAX_STAKE_PER_AGENT_PER_DRAW` |
| `ADVANCED` | `BLOCK_BET_TYPE` and any supported rule not classified above |

`MAX_SALES_COUNT_PER_TICKET` is ambiguous with `MAX_LINES_PER_TICKET`. The implementation should not emphasize it in the default quick actions until the product meaning is confirmed.

## Active Limit Read Model

Extend `TenantAdminPoliciesOverviewView` with:

```java
List<ActiveLimit> activeLimits
```

Suggested fields:

```java
record ActiveLimit(
    UUID assignmentId,
    RuleKey ruleKey,
    String group,
    String targetType,
    UUID targetId,
    String targetLabel,
    String targetCode,
    boolean enabled,
    BreachOutcome onBreach,
    JsonNode params,
    Instant startsAt,
    Instant endsAt,
    List<String> actions) {}
```

The exact Java field names can follow existing backend naming conventions, but the contract must give the web enough information to render a row without additional label lookups.

### Label resolution

Target labels are business labels:

- `TENANT` → tenant/global label such as `Tout santral`;
- `DRAW_CHANNEL` → draw channel display label from the draw channel owner;
- `SELLER_TERMINAL` → terminal code/name from seller terminal owner;
- `AGENT` → seller/agent display label if supported, otherwise stable code fallback.

The feature/BFF layer must aggregate through stable APIs/query buses, not repositories or direct SQL from another module.

## Quick Actions

### Block number

Default behavior:

- scope: draw channel;
- duration: today;
- channel picker: active/open sellable channels first;
- number input: prominent and first;
- save creates or updates `BLOCK_SELECTION_PER_DRAW`.

When opened from draw or draw channel context, the channel is locked and clearly shown.

### Number stake cap

Default behavior:

- scope: draw channel;
- duration: today unless the admin changes it;
- number input + amount input;
- creates or updates `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW`.

### Ticket limit

Starts from the common ticket rules:

- max lines per ticket;
- max stake per line;
- max stake per ticket.

Tenant/global is the default target. More specific scopes should be available only if the UI has enough context to make them understandable.

### Seller limit

Starts from terminal/seller context if available. From the central page, the admin selects a seller terminal.

## Actions

Every active limit row/card supports only actions that are meaningful for that rule and target:

- **Modifye** opens the relevant dialog with existing params.
- **Dezaktive** keeps the assignment but sets `enabled = false`.
- **Efase** deletes the assignment after confirmation.

The list refreshes after each mutation. Errors use the existing web error conventions and standard toast/dialog patterns.

## Contextual Effective Limits

Contextual detail pages show a read-only effective summary before the full rule editor.

Split the display into two concepts:

- **Règles statiques**: configured active assignments, such as blocked numbers and stake caps.
- **Expositions chaudes**: runtime exposure alerts derived from sales already recorded on the draw.

For draw detail:

```text
Règles statiques
  Nimewo bloke : 12, 45         Bloke · Soti nan kanal tiraj
  Plafon sou nimewo : 500 HTG   Bloke · Soti nan santral

Expositions chaudes
  12                             84% · 420 HTG
  45                             71% · 355 HTG
```

For draw channel detail, use the same compact summary for limits assigned directly to that channel plus inherited tenant limits.

The goal is diagnostics, not full administration. If a seller says a sale is blocked, the admin can open the draw and see the static rules plus the hot exposures that could explain it.

The full configuration block remains below the summary for editing. The central Limit page remains the only place that shows all assignments across tenant, channels, and seller terminals.

## Empty State

If no active limit exists, show:

- one short explanation that no special restriction is active;
- primary action `Bloke nimewo`;
- secondary action `Ajoute limit`.

Do not show a technical empty table.

## Web Architecture

- API calls remain in feature data-access services.
- The overview page exposes unwrapped business data to the component.
- Use Angular signals/resource patterns already present in the admin portal.
- User-visible text must use i18n keys in `ht`, `fr`, and `en`.
- Component layout follows existing admin console patterns and remains mobile-first.

## Backend Architecture

- Query handlers are read-only.
- Feature aggregation uses stable contracts.
- No business logic in controllers.
- No direct SQL or repository access from `features/*` into another module.
- Strongly typed IDs remain at module boundaries outside persistence.
