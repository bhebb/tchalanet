## Status: DRAFT

## 1. Lecture du contexte

- [ ] 1.1 Lire `LimitTarget.java` — identifier les sous-types du sealed type (`Tenant`, `Outlet`, `Agent`, `Terminal`, `DrawChannel`)
- [ ] 1.2 Lire `LimitAssignmentPersistenceAdapter.listActiveForTargets()` — vérifier qu'il supporte une liste non-vide
- [ ] 1.3 Si l'implémentation ne supporte pas une liste non-vide : corriger la requête SQL/JPQL en premier

## 2. LimitContext.toTargets()

- [ ] 2.1 Ajouter la méthode `public List<LimitTarget> toTargets()` dans `LimitContext` :
  - Toujours inclure `LimitTarget.Tenant(tenantId)`
  - Si `outletId != null` : inclure `LimitTarget.Outlet(outletId)`
  - Si `agentId != null` : inclure `LimitTarget.Agent(agentId)`
  - Si `terminalId != null` : inclure `LimitTarget.Terminal(terminalId)`
  - Si `drawChannelId != null` : inclure `LimitTarget.DrawChannel(drawChannelId)`
- [ ] 2.2 Tests unitaires `LimitContextTest.toTargets_allIds()` et `LimitContextTest.toTargets_partialIds()`

## 3. LimitPolicyRuntimeService.evaluate()

- [ ] 3.1 Remplacer `Collections.emptyList()` par `ctx.toTargets()` dans `assignments.listActiveForTargets(ctx.toTargets(), Instant.now())`
- [ ] 3.2 Supprimer l'import `java.util.Collections` si devenu inutilisé
- [ ] 3.3 Test unitaire `LimitPolicyRuntimeServiceTest.evaluate_passesTargetsToAssignments()` (mock `LimitAssignmentReaderPort`, vérifie les arguments reçus)

## 4. Vérification LimitAssignmentReaderPort default method

- [ ] 4.1 La default method `listActive(TenantId)` dans `LimitAssignmentReaderPort` appelle aussi `listActiveForTargets(List.of(), ...)` — corriger pour passer `List.of(new LimitTarget.Tenant(tenantId))`

## 5. Test d'intégration

- [ ] 5.1 `LimitPolicyRuntimeServiceIT` : créer des assignments pour des cibles spécifiques, appeler `evaluate()`, vérifier que les limits sont effectivement prises en compte

## 6. Vérification finale

- [ ] 6.1 `./mvnw clean verify -pl tchalanet-server` → build vert + tous tests
- [ ] 6.2 Valider avec les features qui utilisent `LimitPolicyRuntimeService` que l'évaluation ne produit pas de régressions
- [ ] 6.3 Mettre à jour CHANGELOG (`FIX: LimitPolicyRuntimeService.evaluate() now loads assignments for actual context targets`)
