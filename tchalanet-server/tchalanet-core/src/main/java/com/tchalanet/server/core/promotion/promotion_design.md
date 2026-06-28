# DOMAIN_PROMOTION — core.promotion + Sales integration V1

> **Status**: NORMATIVE
> **Scope**: `core.promotion` and the promotion integration inside `core.sales`
> **Audience**: Backend developers and AI coding agents (Claude Code)
> **Supersedes**: `design.md` (the promotion "rule engine" design — see §13)

---

## 0. How to use this document

This is the **source of truth** for promotion in V1: both `core.promotion`
(configuration + evaluation) and the promotion integration inside `core.sales`
(materialisation + snapshot). Read it fully before creating, editing, or
reviewing any file under `core/promotion/` or `core/sales/.../sell/promotion/`.

This document is a **hardening design for an existing domain**, not a greenfield
proposal. The domain already exists; the goal is to close scope, remove
rule-engine residue, and make the money-critical boundary harder to violate.

If something here conflicts with older notes (`design.md` in particular),
**this document wins**. If a requested change contradicts a rule here, do not
implement it silently — flag the conflict and ask.

Repo hierarchy still applies: `ARCHITECTURE.md` > `PLAYBOOK.md` >
`conventions/` > this doc for anything structural. This doc only refines the
promotion concern; it never overrides architecture or conventions.

---

## 1. Purpose

`core.promotion` answers one question and owns one kind of configuration:

```text
Given a sale evaluation context, which commercial effects apply now?
```

It is a `core` domain because a wrong promotion decision directly affects
money: payout base amounts, waived charges, boosted odds. It is NOT a generic
rule engine.

Concrete V1 use cases: "maryaj gratuit" rules, seasonal campaigns (e.g.
Christmas), simple marketing promotions.

Admin UX implication:

- The admin surface is a **Promotions / Campaigns** console.
- "Maryaj gratis" is a preconfigured campaign template and shortcut, not a
  separate promotion subsystem.
- Admins configure typed campaigns and rules; they do not author a generic DSL.
- `HT_MARYAJ_GRATUIT` is displayed as a real catalog game because settlement,
  odds and receipt rendering treat it like one.

---

## 2. V1 scope — what is IN

Three commercial effects, and nothing else:

```text
FREE_GAME_LINE   add a free game line (the maryaj gratuit game)
BOOST_ODDS       override odds on existing lines
WAIVE_CHARGE     cancel / zero a buyer charge (e.g. SMS)
```

A campaign groups rules. A rule carries eligibility + one or more effects.

The V1 admin creation UI should expose exactly these effect families:

- `FREE_GAME_LINE`
- `BOOST_ODDS`
- `WAIVE_CHARGE`

Any other promotion type is a backend/domain change first, not a front-only
dropdown addition.

---

## 3. V1 scope — what is OUT (do NOT build)

Explicitly excluded. Do not add them, do not leave hooks, do not "prepare"
tables for them.

- Generic rule engine / tenant-authored rule DSL
- Per-rule `evaluation_phase` as a stored attribute (§7 — phase is a call
  parameter, not rule data)
- Rule versioning (`promotion_rule_version`, duplicate-on-edit)
- Stacking, exclusivity groups, `BEST_OF_GROUP`, priority arbitration beyond a
  single integer `priority` field
- Promotion simulation runs / admin "what-if" tooling
- `COMMISSION_MODIFIER` effect
- A `FREE_EXTRA_LINES` effect distinct from `FREE_GAME_LINE` — there is ONE
  free-line effect; multiple lines are expressed by `quantity` on it
- Per-rule usage quota (`quota_key` / `max_uses`) — see §8
- Offline promotion evaluation (offline receipts promise no promotions)
- A promotion-side listener that creates the applied snapshot (§10)

If a task asks for any of the above, stop and confirm — it is a scope change.

Existing code that still exposes any excluded concept is treated as migration
debt, not precedent. Do not preserve it for compatibility unless a caller is
already in production and the exception has an ADR.

---

## 4. The three blocks of core.promotion

`core.promotion` has exactly three responsibilities. Keep them separate.

### Block 1 — Campaign + Rules (the aggregate)

- `PromotionCampaign` is the **aggregate root**.
- A `PromotionRule` lives **inside** the campaign aggregate. It is not a root.
- Rules, effects and eligibility are manipulated **through the campaign**.
- Lifecycle is carried by the **campaign**, never by the rule.

### Block 2 — Configuration (admin, cold path)

- Tenant admins configure campaigns and rules.
- Controllers under `internal/infra/web/admin/`, scope `/api/v1/admin/...`.
- Goes through `CommandBus` / `QueryBus` (mandatory in a `core` module).
- Cold path: no performance constraint, runs outside any sale.

### Block 3 — Resolution (sale, hot path, PURE READ)

- `EvaluatePromotionQuery`: `PromotionEvaluationContext -> PromotionDecision`.
- Called by `core.sales` via `QueryBus`.
- **Pure read. It NEVER persists anything.** See §9.
- Evaluation logic lives in a pure domain service (`PromotionRuleEvaluator`),
  no Spring, no I/O.
- Decision persistence/cache, if present, is an implementation detail of the
  read side only. It must not become the historical applied snapshot.

---

## 5. Ownership boundary — Promotion vs Sales

This boundary is non-negotiable and is the rule most likely to be violated.

```text
core.promotion  OWNS campaign / rule / effect configuration and PURE
                evaluation. It produces a PromotionDecision and nothing else.

core.sales      OWNS sale materialisation:
                  - ticket
                  - ticket lines
                  - money breakdown
                  - applied promotion snapshot

settlement / payout  RE-READ Sales snapshots only. They never re-evaluate
                     promotions.
```

Hard rules:

- Promotion never creates ticket artifacts (no ticket, no ticket line, no
  money breakdown, no charge).
- Promotion never writes during `EvaluatePromotionQuery`.
- The `applied_promotion_snapshot` table belongs to `core.sales`.
- `core.promotion` may expose the decision model and evaluation query through
  its public API. It must not expose snapshot commands, snapshot ports, or
  snapshot persistence as part of the promotion domain.
- Sales **owns the container**; the **content** of the snapshot is the
  `PromotionDecision` produced by Promotion, stored faithfully and frozen.
  Sales does not re-interpret or recompute the decision — it materialises it
  and freezes it.
- Settlement reading "snapshots only" is correct **because** the snapshot
  embeds the original `PromotionDecision` verbatim.

---

## 6. Persistence — typed tables, no config JSON

Configuration data is **typed and relational**. JSON is allowed in **exactly
one place**: the immutable applied snapshot (owned by `core.sales`, §11).

### promotion_campaign

```text
promotion_campaign
  id, tenant_id
  code            unique per (tenant_id, code)
  name
  status          PromotionCampaignStatus enum (@Enumerated STRING)
  priority        int
  starts_at       Instant      -- the campaign validity window
  ends_at         Instant
  audit columns, version
```

The validity window lives on the **campaign**. Rules do NOT carry their own
`starts_at` / `ends_at`; they inherit the campaign window.

No `config_version` rule-versioning column. If a cache-invalidation hash is
genuinely needed for the resolution cache, name it `config_hash` and document
its runtime use; otherwise it does not exist.

### promotion_rule

```text
promotion_rule
  id, tenant_id
  campaign_id
  rule_key          unique per (tenant_id, campaign_id, rule_key)
  priority          int
  -- scalar eligibility (no eligibility_json):
  min_paid_total    Money,     nullable
  before_local_time LocalTime, nullable
  audit columns, version
```

- NO `eligibility_json`, NO `effects_json`.
- NO `status` on the rule — lifecycle is the campaign's (§4, Block 1).
- NO `evaluation_phase` (§7).
- NO `quota_key` / `max_uses` (§8).

### promotion_rule_effect

```text
promotion_rule_effect
  id, rule_id
  effect_type         FREE_GAME_LINE | BOOST_ODDS | WAIVE_CHARGE
  game_code           nullable  -- FREE_GAME_LINE
  payout_base_amount  nullable  -- FREE_GAME_LINE
  quantity            nullable  -- FREE_GAME_LINE (number of free lines)
  odds_override       nullable  -- BOOST_ODDS only
  charge_type         nullable  -- WAIVE_CHARGE
```

One rule -> N effects. Columns are nullable per effect type; the valid
combination per `effect_type` is enforced by a domain validator at effect
construction (including amount scale — a malformed amount is rejected here,
not discovered during a sale).

### promotion_rule_eligibility_line

```text
promotion_rule_eligibility_line
  id, rule_id
  game_code
  min_count           int   -- "paidLineCount by gameCode >= N"
```

Only the multi-line eligibility condition needs a table. Scalar conditions
stay as columns on `promotion_rule`.

### Worked example

> "If the buyer spends at least 1000 HTG AND has at least 3 paid lines of
> game X, then add a free maryaj gratuit line."

```text
promotion_rule
  min_paid_total = 1000 HTG
promotion_rule_eligibility_line
  (game_code = X, min_count = 3)
promotion_rule_effect
  (effect_type = FREE_GAME_LINE, game_code = HT_MARYAJ_GRATUIT,
   quantity = 1, payout_base_amount = ...)
campaign window: starts_at / ends_at on promotion_campaign
```

All persistence follows `conventions/persistence.md`: `tenant_id` on
tenant-scoped rows, RLS, standard audit columns, typed IDs outside
persistence, Envers where audited.

---

## 7. Evaluation phase is a call parameter, not rule data

`PromotionEvaluationContext.phase()` tells the evaluator **why** it is called:

```text
SALE_PREVIEW       evaluate the CURRENT sale cart so the cashier can show the
                   buyer what they will get if they confirm. Decision is
                   computed and RETURNED, never persisted. This is a preview
                   of the real cart in progress — NOT an admin simulation of
                   hypothetical carts.
SALE_CONFIRMATION  evaluate the cart being confirmed. Decision is computed and
                   RETURNED to core.sales, which freezes it (§11).
```

The **same rules** are evaluated in both phases. The phase never changes which
rules apply and never causes Promotion to write.

Therefore:

- `promotion_rule` MUST NOT have an `evaluation_phase` column.
- The read port exposes `findActiveRules()`, NOT `findActiveRulesForPhase()`.
- `PromotionEvaluationPhase` has exactly `SALE_PREVIEW` and
  `SALE_CONFIRMATION`. Any extra value (`COMMISSION`, settlement, payout,
  simulation, etc.) re-opens the rule-engine design and is out of V1.

---

## 8. No per-rule usage quota in V1

`quota_key` / `max_uses` are removed from V1. A usage quota implies a mutable
usage counter incremented per sale, in the hot path, under concurrency — a
full mechanism, not two columns. It is not scoped for V1.

If usage limiting is needed later it is a dedicated design (counter table,
transactional increment, concurrency guard), or it belongs to
`core.limitpolicy`. Until then, these columns do not exist.

The same applies to usage ports/services. A `PromotionQuotaPort`, quota
repository, or quota check inside `PromotionRuleEvaluator` is out of V1 even if
it does not yet have a database table.

---

## 9. Resolution handler — reference behaviour

`EvaluatePromotionQueryHandler` is a **pure read**:

- Loads active rules via `findActiveRules()` (no phase filter, §7).
- Delegates per-rule evaluation to the pure `PromotionRuleEvaluator`.
- Builds a `PromotionDecision` (status `APPLIED` / `NOT_ELIGIBLE`).
- **Returns** the decision. It does NOT persist it — not on
  `SALE_CONFIRMATION`, not on any phase.
- Never mutates rules or campaigns.

If a `PromotionDecision` must be persisted, that is a separate command,
triggered by `core.sales` as part of the sale transaction (§11) — never by the
query handler. A query that writes violates CQRS (`ARCHITECTURE.md`).

Timestamps are `Instant`. Calendar conditions (`before_local_time`, campaign
window) use the tenant timezone. Never the JVM or client timezone.

---

## 10. Aggregate invariants — PromotionCampaign

Enforced by the campaign aggregate. Not in controllers, not in services.

### Lifecycle

```text
DRAFT -> ACTIVE -> PAUSED -> ACTIVE -> INACTIVE -> ARCHIVED
```

- `PromotionCampaignTransition` gives the *target* status; the aggregate
  validates the *source* status. Illegal transitions throw a domain exception.
- The legal transition matrix lives in the aggregate. (Open item: confirm
  exactly which sources are legal for `ARCHIVE` and whether `INACTIVE` may
  return to `ACTIVE`.)

### Content is editable only in DRAFT

- `addRule`, `updateRule`, `deleteRule` are legal **only when the campaign is
  `DRAFT`** (`requireDraft()` in the aggregate).
- Once a campaign leaves `DRAFT`, its rules, eligibility and effects are
  **immutable**. `PAUSED` does NOT re-open editing.
- To change a non-draft campaign: create a new one.

Rationale: an already-evaluated sale must have been evaluated against a rule
set that did not change underneath it. This keeps the applied snapshot
explainable.

### Rule completeness

- `addRule` creates a **complete, valid** rule: metadata + eligibility +
  at least one effect, in one transaction.
- `createCampaign` creates the campaign and its initial complete rules in the
  same transaction; a campaign creation request with zero rules is rejected.
- A rule with zero eligibility conditions is invalid and is rejected.
- A rule with zero effects is invalid and is rejected.
- A rule is never persisted half-built.
- Separate replace endpoints for eligibility/effects, if exposed for draft
  editing, are legal only while the campaign is `DRAFT` and must reject empty
  replacement lists so they never leave a rule incomplete.

### Delete

- `deleteRule` is legal only in `DRAFT`; a physical delete is safe there
  because no sale has produced a snapshot. There is no rule delete once the
  campaign has left `DRAFT`.

---

## 11. Sales integration — materialisation

Promotion integration inside `core.sales` lives under
`core/sales/internal/application/service/sell/promotion/`.

### Components

```text
SalePromotionEffectApplier   orchestrates applying a PromotionDecision
PromotionTicketLineFactory   builds free TicketLine(s)  -> FREE_GAME_LINE
PromotionOddsBoostApplier    overrides odds on lines    -> BOOST_ODDS
PromotionChargeApplier       waives a charge            -> WAIVE_CHARGE
PromotionSelectionResolver   resolves the selection of a free line
```

### Rules

- These components **materialise** a decision; they never evaluate. The
  `PromotionDecision` is an input, already produced by `core.promotion`.
- `SalePromotionEffectApplier` only acts on `status == APPLIED`.
- The `switch` over `effect.type()` has no silent `default`. An unknown effect
  type means Promotion produced something out of scope — a bug, not a warning.
  The default branch **throws** a domain exception (fail-fast); the sale does
  not proceed in a degraded state.

### Maryaj gratuit is a real game

- `HT_MARYAJ_GRATUIT` is a full game in `catalog.game`, with its own
  `game_code`. Its odds are configured by the tenant in `pricing_odds`, like
  any other game.
- A `FREE_GAME_LINE` line is an ordinary `TicketLine` with `stake = 0`. It
  flows through the normal pipeline (pricing, settlement, payout, snapshot) —
  no parallel path for free lines.

### Single source of truth for odds

- The odds of a game — maryaj gratuit included — live ONLY in `pricing_odds`.
  `PromotionTicketLineFactory` reads odds from `PricingCatalog`.
- The `FREE_GAME_LINE` effect carries `game_code`, `quantity`,
  `payout_base_amount` — **never odds**. This avoids storing the odds twice.
- `BOOST_ODDS` is the only effect that carries an odds value
  (`odds_override`), because its purpose is precisely to substitute the odds.
  That substitution is intentional and is frozen into the snapshot.
- Sales code must not special-case `FREE_EXTRA_LINES`. Multiple free lines are
  represented by `FREE_GAME_LINE.quantity`.

### Free-line selection

- `PromotionSelectionResolver` uses the customer's choice when provided
  (`source = CUSTOMER_SELECTED`).
- When no choice is provided, the system assigns a default selection
  (`source = PROMOTION`). This default is an intentional business decision and
  must be documented as such at the call site — it is not a placeholder.

---

## 12. Sales integration — the applied snapshot and transaction rules

This section overrides any earlier note that said `AfterCommit`.

### The snapshot is persisted IN the sale transaction

- `core.sales` persists `applied_promotion_snapshot` in the **same
  transaction** as the ticket, ticket lines and money breakdown — inside the
  `@TchTx` of `SellTicketHandler`.
- It is NOT created in `AfterCommit`, and NOT created by a separate command
  reacting to an event.

Rationale: the ticket and its promotion snapshot must commit or fail together.
A snapshot created after commit, in its own transaction, can fail and leave a
sold ticket with applied promotion effects but no historical record — exactly
what the snapshot exists to prevent.

### AfterCommit is for events only

- `AfterCommit` publishes side-effects (e.g. `TicketPlacedEvent`). It never
  creates the critical snapshot.
- The snapshot is part of the sale's truth, not a side-effect of it.

### No promotion-side snapshot listener

- There is **no listener inside `core.promotion`** reacting to a sales event
  to build the snapshot. Any draft `TicketPlacedPromotionSnapshotListener`
  must be removed. The snapshot is created by `core.sales` synchronously,
  inside the sale flow.

### Snapshot content

- The snapshot stores the `PromotionDecision` faithfully and frozen (this is
  the one place JSON is acceptable — immutable historical truth).
- Settlement and payout re-read this snapshot and never re-evaluate rules.

---

## 13. Hardening plan for the existing domain

This is the implementation sequence for bringing the current code back inside
the V1 boundary. Keep the steps separate enough that each PR can be reviewed
against one ownership rule.

### Step 1 — Freeze the public contract

- Keep `EvaluatePromotionQuery`, `PromotionEvaluationContext`,
  `PromotionDecision`, `PromotionEffect`, and the campaign/rule admin models
  needed by V1.
- Remove public API types that only support excluded V1 concepts:
  `PromotionStackingPolicy`, `PromotionRuleStatus`, quota fields, simulation
  request/response types, and snapshot commands/views owned by promotion.
- Keep `PromotionEvaluationPhase` limited to `SALE_PREVIEW` and
  `SALE_CONFIRMATION`.

### Step 2 — Narrow persistence

- Merge promotion schema into the pre-go-live core migration, following
  `docs/conventions/persistence.md`; do not add another promotion migration
  without explicit approval.
- Remove `promotion_rule.evaluation_phase`, `promotion_rule.status`,
  `quota_key`, `max_uses`, config JSON columns, and rule-versioning tables.
- Move `applied_promotion_snapshot` ownership to `core.sales` persistence.
  The JSON decision payload is allowed there because it is immutable history.

### Step 3 — Make evaluation pure

- `EvaluatePromotionQueryHandler` loads active campaign rules and returns a
  `PromotionDecision`; it does not create a snapshot and does not mutate quota,
  rule state, usage counters, or campaign state.
- `PromotionRuleEvaluator` handles only typed V1 eligibility and the three V1
  effects. Comments or placeholders for future evaluator families should be
  removed.
- Cache invalidation, if kept, is tied to campaign/rule configuration changes,
  not to sale confirmation.

### Step 4 — Move materialisation to sales

- `core.sales` evaluates by calling promotion, then materialises the returned
  decision into ticket lines, charges, odds snapshots, money totals, and the
  applied snapshot in the sale transaction.
- Remove `TicketPlacedPromotionSnapshotListener` and
  `TicketApprovedPromotionSnapshotListener` from `core.promotion`.
- Pending-approval flows must still freeze the applied snapshot in the sales
  transaction that turns the ticket into sold/approved state; not after commit.

### Step 5 — Close old rule-engine surfaces

- Delete the promotion simulation admin controller and mapper.
- Delete or rewrite sales references to `FREE_EXTRA_LINES`.
- Ensure switch statements fail fast for unknown effect types.
- Add focused tests around: no write in evaluation, DRAFT-only rule edits,
  invalid lifecycle transitions, free-line quantity, odds boost, charge waiver,
  and snapshot creation inside the sales transaction.

---

## 14. Relationship to design.md

`design.md` described a full promotion **rule engine**: six templates, six
evaluation phases, stacking, exclusivity groups, rule versioning, simulation.

**V1 does not build that.** `design.md` is kept only for historical context
and for the parts that remain valid regardless of scope:

- Rule (live config) vs applied snapshot (immutable history) separation.
- Settlement/payout read snapshots, never live rules.
- `Instant` storage + tenant timezone for calendar logic.

Everything else in `design.md` is OUT for V1 (§3). When `design.md` and this
document disagree, this document wins.

---

## 15. Quick checklist for a promotion PR

core.promotion:

- [ ] No `*_json` column on configuration tables.
- [ ] No `evaluation_phase` on `promotion_rule`; read port is `findActiveRules()`.
- [ ] No `status` on `promotion_rule`; lifecycle is on the campaign.
- [ ] No `quota_key` / `max_uses`; no `config_version` rule-versioning column.
- [ ] No `PromotionStackingPolicy`, `PromotionRuleStatus`,
  `PromotionQuotaPort`, simulation admin endpoint, or promotion-owned snapshot
  command/view remains in the V1 surface.
- [ ] `EvaluatePromotionQuery` persists nothing, on any phase.
- [ ] Rule edits (`add`/`update`/`delete`) rejected unless campaign is `DRAFT`.
- [ ] Campaign creation persists campaign + initial complete rules atomically.
- [ ] `addRule` produces a complete rule with >= 1 eligibility condition and
  >= 1 effect.
- [ ] Eligibility/effects replacement endpoints reject empty lists.
- [ ] Campaign validity window on `promotion_campaign`, not on the rule.
- [ ] `PromotionEvaluationPhase` only contains `SALE_PREVIEW` and
  `SALE_CONFIRMATION`.
- [ ] Only `FREE_GAME_LINE` / `BOOST_ODDS` / `WAIVE_CHARGE` exist; no
  `FREE_EXTRA_LINES`.

core.sales integration:

- [ ] `applied_promotion_snapshot` persisted in the sale transaction, not
  `AfterCommit`.
- [ ] `AfterCommit` used only to publish events.
- [ ] No listener in `core.promotion` building the snapshot.
- [ ] Sales materialises the decision; it never evaluates.
- [ ] `SalePromotionEffectApplier` default branch throws, never warns.
- [ ] Odds read from `pricing_odds`; `FREE_GAME_LINE` effect carries no odds.
- [ ] Snapshot embeds the `PromotionDecision` verbatim; settlement re-reads it.

---

## 16. Sélection auto-générée & template tenant — ✅ IMPLÉMENTÉ (2026-06-10, slices 3-4)

> Source de vérité : `tchalanet-server/openspec/changes/maryaj-gratis-auto-selection-v1/`
> (proposal + design + tasks). Mettre à jour le statut quand les slices livrent.

### Extension de l'effet (FREE_GAME_LINE)

Champs ajoutés sur `promotion_rule_effect` (migration V222) :

```text
choice_mode                       NONE | CUSTOMER_SELECTS | SELLER_SELECTS | AUTO_GENERATE
generation_strategy               RANDOM | LOW_EXPOSURE_RANDOM
regenerable_before_confirm        boolean (défaut false)
max_regenerations_before_confirm  int (défaut 3)
```

Décision d'implémentation : le `selectionMode` de la spec est porté par le
`PromotionChoiceMode` existant (désormais persisté en `choice_mode`) —
`AUTO_GENERATE` = sélection auto-générée ; pas de colonne doublon.
L'enum stratégie vit dans `core.selection.api.model.SelectionGenerationStrategy`
(module neutre) pour éviter un cycle promotion <-> sales.

Règles :

- `AUTO_GENERATED` exige une stratégie supportée.
- `LOW_EXPOSURE_RANDOM` est **refusé partout en V1** (validation effet,
  activation campagne, génération, régénération) — l'enum n'est qu'une
  réservation.
- Promotion **ne génère jamais** les numéros. Elle décide l'effet ;
  `core.sales.SelectionGenerationService` génère et matérialise
  (voir `core/sales/DOMAIN_SALES.md` §11).
- Ce mécanisme complète `PromotionSelectionResolver` (§11) : avec
  `selection_mode = AUTO_GENERATED`, la sélection vient du générateur côté
  sales (`selectionSource = AUTO_GENERATED`), pas du client ni d'un défaut.

### Template par défaut tenant

Implémenté en code versionné (`MaryajGratisDefaultTemplate`) + commande
`InstantiateDefaultMaryajGratisCommand` (idempotente par code), endpoint
`POST /admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate`.

```text
DEFAULT_MARYAJ_GRATIS (seed versionné en code, pas de campagne globale runtime)
  effect = FREE_GAME_LINE, gameCode = HT_MARYAJ_GRATUIT
  quantity = paramètre tenant admin, défaut 1
  payoutBaseAmount = paramètre tenant admin, défaut 50 HTG
  choiceMode = paramètre tenant admin, défaut AUTO_GENERATE
  generationStrategy = RANDOM si AUTO_GENERATE
  regenerableBeforeConfirm = paramètre tenant admin, défaut true, max 3
  éligibilité : minPaidTotal > 0 + au moins 1 ligne payante
```

- Instanciation : template plateforme -> campagne tenant ACTIVE via commande
  admin interne. Hook onboarding = follow-up ; **jamais de backfill
  automatique silencieux** des tenants existants (tâche ops avec dry-run).
- En V0, le shortcut admin tenant crée la campagne avec ces paramètres. Si une
  campagne `DEFAULT_MARYAJ_GRATIS` existe déjà, l'endpoint reste idempotent et
  retourne l'existant ; les changements de règle passent par une nouvelle
  campagne/règle, pas par mutation silencieuse d'une campagne active.
