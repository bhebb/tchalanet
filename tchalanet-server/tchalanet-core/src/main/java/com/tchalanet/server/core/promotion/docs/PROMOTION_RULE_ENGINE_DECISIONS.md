# Décisions — Promotion Rule Engine

## ADR-001 — Contrat stable et moteur remplaçable

Décision : `core.promotion` expose `PromotionEvaluationContext -> PromotionDecision`.

Raison : `sales`, `settlement`, `payout` et `limitpolicy` ne doivent jamais dépendre de Drools, DMN, SpEL, MVEL ou d'un format JSON interne.

Conséquence : tout futur moteur doit adapter ses résultats vers les effets canoniques Tchalanet.

## ADR-002 — V1 simple Java, pas moteur externe

Décision : V1 utilise `SimplePromotionRuleEngine`.

Raison : le domaine doit d'abord stabiliser les concepts métier : conditions, effets, snapshots, montants, stacking, offline.

Non-objectif : éditeur visuel de règles, DSL libre, Drools/DMN.

## ADR-003 — Snapshots métier, pas snapshots techniques

Décision : les tickets/lignes stockent une snapshot canonique d'effet promotionnel, pas l'expression source de la règle.

Bon :

```json
{
  "effectType": "PAYOUT_MULTIPLIER_OVERRIDE",
  "appliedMultiplier": 60
}
```

Mauvais :

```json
{
  "droolsRule": "when sale.date == christmas then ..."
}
```

## ADR-004 — Online-only par défaut

Décision : les promotions sont online-only par défaut.

Raison : elles peuvent être abusées offline, surtout si elles dépendent de l'heure, d'un seuil ou d'une règle expirée.

V2 : snapshots signés de règles offline.

## ADR-005 — Multiplicateurs variables par tenant

Décision : les multiplicateurs promotionnels sont configurés par tenant via `promotion_rule` et snapshotés à la vente.

Raison : deux tenants peuvent vendre le même jeu avec des campagnes commerciales ou multiplicateurs différents.

Conséquence : le settlement ne relit pas la configuration courante pour les tickets déjà vendus. Il lit `ticket_line_applied_rule.effect_snapshot_json`.

## ADR-006 — Stacking explicite

Décision : chaque règle porte `stackable`, `priority` et `exclusive_group`.

Raison : éviter les conflits silencieux entre deux boosts de payout ou deux promotions incompatibles.

Règles V1 :

```text
- deux overrides de multiplicateur sur la même ligne/rang ne se cumulent pas ;
- la plus haute priorité gagne ;
- priorité égale + conflit = erreur de configuration ;
- free line peut coexister avec payout boost sauf exclusive_group identique.
```
