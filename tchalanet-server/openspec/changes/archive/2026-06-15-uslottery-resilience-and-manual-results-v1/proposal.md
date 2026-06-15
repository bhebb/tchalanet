# Proposal — US Lottery Providers Resilience & Manual Results V1

## Status

Done — 2026-06-15

## Why

Ajouter plusieurs providers US Lottery sans fragiliser le pipeline de résultats.

Le pipeline actuel (`close → fetch → apply → settle`) est fragile : une exception dans un provider ou un mapper stoppe le scheduler tick entier. De plus, il n'existe pas de moyen de couper rapidement un slot ou un jeu externe cassé sans redéploiement.

Cette V1 renforce la résilience et ajoute des contrôles opérationnels sans changer le pipeline officiel.

## What

### 1. Résilience adapter

`UsLotteryExternalResultsFetchAdapter` retourne un bundle vide (sans throw) pour tout échec : provider inconnu, client non enregistré, provider désactivé, gameCodes vides, exception client, exception mapper.

### 2. Filtrage actif des jeux

Le fetch handler lit les game codes actifs depuis `result_slot.source_cfg` avant d'appeler le port. Un slot sans game codes actifs est sauté silencieusement.

`source_cfg.pick3.active=false` exclut le game code Pick 3 de la query. `active` absent = `true` (rétrocompatible).

### 3. Kill switches ops

Trois kill switches opérationnels, tous via endpoints platform/ops sécurisés et audités :

- `result_slot.active=false` — coupe le fetch global d'un slot.
- `source_cfg.pickN.active=false` — coupe un jeu externe dans un slot.
- `draw_channel.active=false` — coupe l'exposition commerciale tenant d'un channel.

Pas de modification de `draw_channel_game` dans cette V1.

### 4. Expansion providers

Ajout des providers NJ, PA, IL, MI, OH, CA, MO au minimum via enum + YAML config.

Si un client + mapper complet peut être construit (transport fiable) : implémenté.
Si pas faisable (HTML fragile, endpoint non documenté) : provider activé dans l'enum et YAML, résultats gérés manuellement via la proposition (Slice 5).

Premier candidat plein : PA (RSS). Autres candidats : MO, CA, NJ.

### 5. Résultat manuel avec permission et trust routing

Un tenant admin peut avoir la permission `DRAW_RESULT_MANUAL_PROPOSE` pour proposer un résultat.

Le résultat s'écrit directement dans `draw_result` via `RecordManualDrawResultCommand` existant.

Le routing est piloté par `trust_policy` du slot (`source_cfg`) :
- `AUTO_CONFIRM_HIGH_CONFIDENCE` → `status=CONFIRMED` direct.
- `REQUIRE_PLATFORM_REVIEW` → `status=PROVISIONAL`, un platform admin confirme.

Conflit : si `status=CONFIRMED` existe déjà et `force=false` → bloqué. Premier écriture gagne.

## Non-goals

- Pas de table `draw_result_proposal`.
- Pas de moteur générique de scraping.
- Pas de modification de `draw_channel_game`.
- Pas de settlement direct depuis `core.uslottery`.
- Pas de correction après settlement dans cette V1.
- Pas de migration Flyway créée par l'agent — le DB est géré par l'utilisateur.

## Principles

1. `result_slot` reste le référentiel global des slots externes.
2. `draw_channel` reste l'exposition commerciale tenant.
3. `source_cfg` dans `result_slot` porte le mapping provider, les flags par jeu, et la trust policy.
4. Un provider qui échoue ne bloque pas les autres providers.
5. Seul `core.drawresult` produit le résultat officiel utilisé par apply / settle.
6. `RecordManualDrawResultCommand` est le seul chemin d'écriture manuelle.
