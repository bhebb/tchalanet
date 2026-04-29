## Why

`LimitResolver.score()` assigne des scores de priorité aux cibles de limite (Tenant=10, Outlet=50, Agent=60, Terminal=70, DrawChannel=40). La sémantique du scoring n'est pas documentée et l'ordre actuel soulève une ambiguïté : un Terminal (dispositif physique, score 70) prime sur un Agent (personne, score 60) et sur un DrawChannel (canal, score 40). Est-ce intentionnel ? Cette incertitude a été identifiée lors de l'audit du 2026-04-27 (sévérité MOYENNE).

## What Changes

- Documentation explicite de la sémantique du scoring dans `LimitResolver` (commentaires + tests).
- Si le scoring actuel est incorrect : ajustement des valeurs avec justification.
- Ajout de tests unitaires couvrant les cas de préséance entre cibles.
- Si la sémantique est validée "telle quelle" : annotation `// INTENTIONAL` dans le code + entrée dans le CHANGELOG.

## Capabilities

### New Capabilities

<!-- aucune nouvelle capability -->

### Modified Capabilities

<!-- le comportement de résolution peut changer si le scoring est corrigé -->

## Impact

- `core.limitpolicy/domain/resolver/LimitResolver` — méthode `score()`.
- Tests : `LimitResolverTest` (couverture des cas de préséance).
- **Breaking si le scoring change** : les tenants ayant des assignments à la fois sur Agent et Terminal pourraient voir des limites différentes appliquées.
