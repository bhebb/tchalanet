# DOMAIN — Pricing (Catalog)

> **Module**: `catalog.pricing`  
> **Type**: Catalog (reference data)  
> **Status**: STABLE  
> **Layer**: `catalog/`  
> **Audience**: Backend, Product, Ops

---

## 1. Purpose

The **Pricing catalog** defines the **odds, multipliers, and pricing parameters** used to calculate ticket payouts.

It acts as a **reference table** consumed by core domains at runtime.

Pricing data is:

- read-mostly
- tenant-scoped
- stable during a selling window
- mutable only via admin configuration

Seller-terminal-specific odds overrides do **not** live in this catalog. They are owned by
`core.pricing`, which resolves effective sale odds with this precedence:

```text
seller-terminal override -> tenant default pricing catalog -> error
```

---

## 2. Responsibilities

### What Pricing DOES

- Store odds / multipliers per:
  - tenant
  - game
  - bet type
  - bet option
- Provide a **read-only lookup API** for pricing resolution
- Support **admin CRUD** for updating pricing tables
- Allow versioned or flagged pricing configurations (future)

### What Pricing DOES NOT do

- Calculate winnings
- Apply business rules
- Validate tickets
- Perform settlements
- Emit domain events
- Decide seller-terminal override precedence

All monetary logic remains in **core.sales / core.payout**.

---

## 3. Conceptual model

### Pricing dimensions

Pricing entries are uniquely defined by:

- `tenant`
- `gameCode`
- `betType`
- `betOption`

Each entry yields:

- an **odds value** or **multiplier**
- an `active` flag

### Conceptual invariant

> At most one **active pricing row** exists for a given `(tenant, gameCode, betType, betOption)`.

Enforcement is done at:

- database level (unique constraints / soft-delete)
- admin service validation

---

## 4. Read access (catalog role)

Pricing is consumed as **lookup data**.

Typical usage:

- resolve tenant default odds;
- provide fallback odds to `core.pricing`;
- support admin screens for tenant default pricing.

Ticket placement with a seller-terminal context should call
`core.pricing.api.query.ResolveSellerTerminalOddsQuery`, not `PricingCatalog` directly. This
includes promotion-created free game lines. That query returns tenant default odds, optional
seller-terminal odds, effective odds and source.

Read access must be:

- deterministic
- side-effect free
- cacheable

**Interface `PricingCatalog`** :

```java
BigDecimal oddsFor(TenantId tenantId, String gameCode, BetType betType, Short betOption)
  // Retourne les odds pour la combinaison tenant/gameCode/betType/betOption

PricingStatsView stats()
  // total + active count
```

**`PricingView`** (record exposé en admin) :

| Champ | Type | Sens |
|---|---|---|
| `id` | `PricingOddsId` | Identifiant |
| `tenantId` | `TenantId` | Propriétaire |
| `gameCode` | `String` | Code du jeu |
| `betType` | `String` | Nom du BetType |
| `betOption` | `Short?` | Code de l'option (null si non requis) |
| `odds` | `BigDecimal` | Multiplicateur (scale 4) |
| `active` | `boolean` | Entrée active |

**`PricingStatsView`** : `int total` · `int active`

Example usage (conceptual):

```java
pricingCatalog.oddsFor(tenantId, gameCode, betType, betOption);
```

Example effective sale usage:

```java
queryBus.ask(new ResolveSellerTerminalOddsQuery(
    tenantId, sellerTerminalId, gameCode, betType, betOption
));
```

---

## 5. Write access (admin only)

Pricing configuration is updated by:

- platform admins
- tenant admins (depending on deployment rules)

Write operations include:

- create pricing entry
- update odds
- deactivate previous pricing
- soft-delete obsolete rows

Changes are not retroactive by default — already sold tickets retain their pricing context.

`core.sales` snapshots the resolved effective odds in `TicketLine.oddsSnapshot`. Result settlement
and payout calculation read that snapshot and never re-resolve current pricing for historical
tickets.

---

## 6. Multi-tenancy

Pricing is tenant-scoped.

Rules:

- no global pricing rows
- each tenant owns its pricing table
- reads always require `TenantId`

---

## 7. Caching expectations

Pricing lookups are:

- high-frequency
- low-cardinality
- latency-sensitive

Therefore:

- aggressive caching is expected
- eviction occurs only on admin writes

Cache semantics are implementation details (defined in `catalog/pricing/internal/cache`).

---

## 8. Lifecycle & stability

Pricing data:

- changes infrequently
- must be stable during a draw window
- is not event-driven

Any future need for:

- pricing versioning
- historical pricing replay
- time-based pricing

MUST be introduced via:

- explicit schema changes
- an ADR
- likely migration toward core responsibility

---

## 9. Out of scope (explicit)

- Promotions
- Dynamic pricing
- Risk management
- Limits
- Bonus or discount systems

These belong to core or feature modules, not the Pricing catalog.

---

## 10. Summary

| Aspect         | Decision                 |
| -------------- | ------------------------ |
| Type           | Catalog (reference data) |
| Reads          | High-frequency lookup    |
| Writes         | Admin only               |
| Events         | ❌ None                  |
| Business logic | ❌ None                  |
| Tenancy        | Tenant-scoped            |
| Caching        | ✅ Yes                   |
| Paging         | ❌ No                    |
| Seller override precedence | `core.pricing` |

---

## 11. Related documents

- `openspec/context/75-catalog-rules.md`
- `AGENTS.md`
- `ARCHITECTURE.md`
