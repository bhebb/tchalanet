## Status: DRAFT

## 1. Validation de la decision produit

- [ ] 1.1 Confirmer avec les parties prenantes que la hiérarchie `Tenant < DrawChannel < Outlet < Agent < Terminal` est intentionnelle pour le contexte POS
- [ ] 1.2 Si contestée : créer un ADR dans `tchalanet-docs/docs/03-adr/` avant toute modification du scoring

## 2. Documentation dans le code

- [ ] 2.1 Ajouter un commentaire Javadoc sur `LimitResolver.score()` explicitant la hiérarchie :
  ```java
  /**
   * Specificity hierarchy (higher = wins over lower):
   * Tenant(10) < DrawChannel(40) < Outlet(50) < Agent(60) < Terminal(70)
   *
   * A Terminal is the most specific context (exact POS device).
   * A cible that doesn't match the current context returns -1 (excluded).
   */
  ```
- [ ] 2.2 Marquer l'implémentation comme `// INTENTIONAL` sur chaque ligne de score pour signaler que les valeurs ne sont pas arbitraires

## 3. Tests unitaires de préséance

- [ ] 3.1 Créer `LimitResolverTest` (ou compléter s'il existe) :
  - `score_terminal_beats_agent()` — Terminal (70) > Agent (60)
  - `score_agent_beats_outlet()` — Agent (60) > Outlet (50)
  - `score_outlet_beats_drawchannel()` — Outlet (50) > DrawChannel (40)
  - `score_drawchannel_beats_tenant()` — DrawChannel (40) > Tenant (10)
  - `score_nonMatching_target_returns_minus1()` — cible hors contexte → -1
- [ ] 3.2 Créer `LimitResolverPickBestTest` :
  - `pickBest_terminalWinsOverAgent_sameRuleKey()`
  - `pickBest_tenantFallback_whenNoSpecificAssignment()`
  - `pickBest_ignores_inactiveAssignments()`

## 4. Vérification finale

- [ ] 4.1 `./mvnw clean verify -pl tchalanet-server -Dtest="LimitResolverTest,LimitResolverPickBestTest"` → tous verts
- [ ] 4.2 `./mvnw clean verify -pl tchalanet-server` → build complet vert
- [ ] 4.3 Mettre à jour CHANGELOG (`DOCS/TEST: LimitResolver.score() semantique documentee et couverte par tests`)
