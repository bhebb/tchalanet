# DOMAIN_PROMOTION — Tchalanet

Ce paquet contient la proposition de domaine `core.promotion` et les OpenSpec pour développer un moteur de règles commerciales évolutif.

Objectif : supporter des règles comme :

- Maryaj gratuit selon seuil d'achat ;
- multiplicateur spécial de payout, ex. le 25 décembre avant midi le premier lot paie 60x au lieu de 50x ;
- multiplicateurs variables par tenant ;
- rabais, bonus fixe, commission boost, règles tenant/outlet/draw/time-window ;
- ouverture future vers un moteur de règles sans coupler `sales`, `settlement`, `payout` au moteur choisi.

## Contenu

```text
docs/
  DOMAIN_PROMOTION.md
  PROMOTION_RULE_ENGINE_DECISIONS.md
  PROMOTION_TESTING_STRATEGY.md
  PROMOTION_ADMIN_UI_NOTES.md

openspec/changes/add-domain-promotion/
  proposal.md
  tasks.md
  design.md
  specs/domain-promotion/spec.md
  specs/sales-integration/spec.md
  specs/settlement-payout-integration/spec.md
  specs/admin-api/spec.md
  specs/offline-sync-integration/spec.md

sql/
  Vxxx__domain_promotion.sql

examples/java/
  core/promotion/api/*
  core/promotion/internal/*
  core/sales-integration/*
  core/settlement-integration/*
```

## Décision principale

`core.promotion` expose un contrat stable :

```text
PromotionEvaluationContext -> PromotionDecision
```

L'implémentation V1 est simple, typée et codée en Java. Plus tard, on pourra brancher un moteur DMN/Drools/JSON rules derrière une interface interne `PromotionRuleEngine`, sans modifier `core.sales`, `core.limitpolicy`, `core.settlement` ou `core.payout`.

## Décision confirmée : multiplicateurs variables par tenant

Les multiplicateurs normaux ou promotionnels sont tenant-scoped. Une promotion de type `PAYOUT_MULTIPLIER_OVERRIDE` ou `PAYOUT_MULTIPLIER_BOOST` stocke le multiplicateur appliqué dans `effect_json`, puis le snapshot métier est copié sur la ligne/ticket lors de la vente.

Le settlement ne relit pas la configuration active pour les tickets déjà vendus. Il utilise la snapshot canonique stockée au moment de la vente.

## Non-objectifs V1

- Pas de moteur Drools/DMN en V1.
- Pas d'éditeur visuel de règles en V1.
- Pas de promotions offline avancées en V1.
- Pas de DSL libre non validé.
