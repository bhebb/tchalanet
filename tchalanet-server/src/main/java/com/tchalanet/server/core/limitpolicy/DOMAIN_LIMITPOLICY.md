# Domaine Limitpolicy

> Ce fichier est un **template** pour documenter le domaine backend.
> Copie/complète les sections ci-dessous (voir `docs/DOMAIN_TEMPLATE.md`).
> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/limits.md`

---

## 1. Rôle du domaine

**Responsabilité principale**

> Valider les tickets et contrôles de vente/payout contre des règles de limites configurables (par période, portée, dimension, bet type), et exposer des décisions de “allow/deny + reason”.
> **Ce que le domaine fait**

- Définit et évalue des limites (stake, count, exposure, payout) selon période (per_ticket, per_draw, per_day).
- Cumule des faits (sold_count, sold_stake_total, potential_payout_exposure, daily totals) par AggregationScope.
- Retourne des décisions structurées pour les handlers de vente/payout.
  **Ce que le domaine ne fait pas**
- Ne calcule pas les gains (payout); ne vend pas des tickets (sales).
- Ne gère pas l’UI; fournit uniquement des décisions.

---

## 2. Modèle métier (agrégats / entités)

### Définitions communes

- Période (Period): `per_ticket` | `per_draw` | `per_day`
- Portée d’agrégation (AggregationScope): `AGENT` | `OUTLET` | `ZONE` | `RANGE` | (optionnel) `TENANT`
- Dimension (Dimension): `line` | `ticket` | `selection` | `total`
- Canonisation `selection_key`:
  - 2D: "00".."99" (2 chars)
  - 3D: "000".."999" (3 chars)
  - Marriage: "12-34" (trié min-max)
  - Lotto4/5 pattern: "<pattern>:<digits>" (ex: "x0123xx:0123")
- Bet Types:
  - Borlette 2D: `MATCH_1_2D`, `MATCH_2_2D`, `MATCH_3_2D`
  - Marriage 2D+2D: `MARRIAGE_2D2D`
  - Lotto: `LOTTO3_3D`, `LOTTO4_PATTERN`, `LOTTO5_PATTERN`

### Entités / agrégats principaux

- `LimitRule` — définition JSON d’une règle (period, scope, dimension, params, applies_to).
- `LimitDecision` — résultat (allow/deny, code, message, meta).

### Invariants métier

- Canonisation de `selection_key` obligatoire avant évaluation.
- Les calculs d’agrégation respectent AggregationScope et Period.
  > Valeur métier clé :
  > Protéger la vente et le payout avec des limites tenant-safe et explicites.

---

## 3. Cas d’utilisation (ports d’entrée)

- `EvaluateTicketLimitsQuery` — évalue un ticket (lines, totals, context) et retourne décisions.
- `EvaluatePayoutLimitsQuery` — évalue un payout (par ligne/ticket/jour).
- `ListActiveLimitRulesQuery` — liste des règles actives pour un tenant.

---

## 4. Ports de sortie (dépendances externes)

- `LimitRuleRepoPort` — lecture des règles.
- `LimitFactsReaderPort` — lecture des agrégats (= sold_count, sold_stake_total, potential_payout_exposure, daily totals) avec clés `(period, scopeKey, betType, selection_key)`.

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapper infra.web.model ↔ application.command/query.model.
- Records immuables pour DTO simples.

---

## 6. Règles métier importantes

- Décision deny quand `fact + delta > max` (ou `< min`) selon la règle.
- Support `applies_to`: filtrage par bet_types et pattern selection.
- Idempotence de la lecture des facts (éviter double-compte dans la transaction en cours).

---

## 7. Intégration avec les autres domaines

Dépend de : sales (ticket context), payout (payout context), draw (drawId), user/outlet (scopeKey).
Utilisé par : sales (avant émission), payout (avant paiement).

---

## 8. Notes techniques

- Multi-tenant; RLS; wrappers ID.
- Les facts sont idéalement lus via projections SQL/JDBC (performants) ou cache L2.
- Décisions agrégées par scope (AGENT/OUTLET/ZONE/RANGE/TENANT) selon config tenant.

---

## 9. Catalogue des limites supportées (résumé)

- MAX_STAKE_PER_SELECTION_PER_TICKET
  - Period: per_ticket; Dimension: selection; Facts: aucun; Params: `{max,currency,applies_to}`.
- MAX_SALES_COUNT_PER_SELECTION_PER_DRAW
  - Period: per_draw; Dimension: selection; Scope: AGENT|OUTLET|ZONE|RANGE; Facts: `sold_count_so_far`.
- MAX_EXPOSURE_PER_SELECTION_PER_DRAW
  - Period: per_draw; Dimension: selection; Scope: AGENT|OUTLET|ZONE|RANGE; Facts: `sold_stake_total_so_far`.
- MAX_TOTAL_STAKE_PER_DRAW
  - Period: per_draw; Dimension: total; Scope: AGENT|OUTLET|ZONE|RANGE; Facts: `sold_total_stake_so_far`.
- MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW
  - Period: per_draw; Dimension: selection; Scope: OUTLET|ZONE|RANGE|TENANT; Facts: `potential_payout_exposure_so_far`.
- MAX_STAKE_PER_LINE
  - Period: per_ticket; Dimension: line; Facts: aucun.
- MIN_STAKE_PER_LINE
  - Period: per_ticket; Dimension: line; Facts: aucun.
- MAX_LINES_PER_TICKET
  - Period: per_ticket; Dimension: ticket; Facts: aucun.
- MAX_STAKE_PER_TICKET
  - Period: per_ticket; Dimension: ticket; Facts: aucun.
- DAILY_STAKE_CAP
  - Period: per_day; Scope: AGENT|OUTLET|ZONE|RANGE; Facts: `daily_stake_total_so_far`.
- MAX_PAYOUT_PER_LINE
  - Period: per_ticket|per_payout_tx; Dimension: line; Facts: `payoutPerLine`.
- MAX_PAYOUT_PER_TICKET
  - Period: per_ticket|per_payout_tx; Dimension: ticket; Facts: `payoutTotal`.
- DAILY_PAYOUT_CAP
  - Period: per_day; Scope: AGENT|OUTLET|ZONE|RANGE; Facts: `daily_payout_total_so_far`.
- MAX_CANCELS_PER_DAY
  - Period: per_day; Scope: AGENT|OUTLET; Facts: `daily_cancel_count_so_far`.

---

## 10. Incohérences / TODO

- Définir la stratégie de cache des facts (L2 Redis + invalidation).
- Spécifier les clés exactes de scope (AGENT:userId, OUTLET:outletId, ZONE:zoneId, RANGE:rangeId, TENANT:tenantId).
- Valider la canonisation `selection_key` pour patterns complexes.
