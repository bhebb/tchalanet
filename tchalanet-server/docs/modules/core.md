# Module core — Clean Architecture / Hexagonal / CQRS

## Rôle

`core` contient les domaines métier critiques de Tchalanet.

## Domaines actifs

| Domaine | Rôle | Doc focal |
|---|---|---|
| `core.sales` | Cycle de vie ticket, settlement, override, promotion offline | `src/main/java/.../core/sales/DOMAIN_SALES.md` |
| `core.pricing` | Overrides odds seller-terminal + résolution odds effectifs | `src/main/java/.../core/pricing/DOMAIN_PRICING.md` |
| `core.analytics` | Projections KPI, dashboards et rapports dérivés | `src/main/java/.../core/analytics/DOMAIN_ANALYTICS.md` |
| `core.draw` | Cycle de vie draw (SCHEDULED → OPEN → CLOSED → RESULTED → SETTLED) | — |
| `core.drawresult` | Ingestion résultats externes (providers normalisés) | — |
| `core.payout` | Exécution payout après ticket gagnant | — |
| `core.ledger` | Écritures comptables après events sales/payout | — |
| `core.limitpolicy` | Limites par scope + per-tenant offline policy override | — |
| `core.session` | POS operational context (terminal/outlet/session) trusted | — |
| `core.selection` | Catalogue de sélections (numbers, patterns) | — |
| `core.offlinesync` | Ventes offline POS + grants Ed25519 + promotion vers sales | `src/main/java/.../core/offlinesync/DOMAIN_OFFLINESYNC.md` |
| `core.uslottery` | Provider HTTP clients US (NY/FL/GA/TX) | — |
| `core.autonomy` | Approval workflows (sale, payout) | — |

## Pattern

```text
core/<domain>/api/
  command/
  query/
  event/
  model/

core/<domain>/internal/
  domain/
  application/
    command/handler/
    query/handler/
    port/out/
    service/
  infra/
    persistence/
    web/
    event/
    batch/
    scheduler/
    cache/
    config/
```

## API publique Java

`api` expose :

```text
commands
queries
public integration/application events
read models
result models
```

`api` n’expose jamais :

```text
aggregates internes
JPA entities
repositories
handlers
ports out
controllers
cache adapters
```

## Règle CQRS

Writes : CommandBus.  
Reads : QueryBus.  
Events : after-commit.
