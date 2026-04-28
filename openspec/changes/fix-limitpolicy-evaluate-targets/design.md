## Context

`LimitPolicyRuntimeService.evaluate(LimitContext ctx)` appelle :

```java
var assigns = assignments.listActiveForTargets(Collections.emptyList(), Instant.now()); // placeholder
```

La liste vide signifie que `LimitResolver.resolve()` ne reçoit jamais les assignments spécifiques aux cibles du contexte. Les limites par agent, outlet, terminal ou drawChannel ne sont donc jamais appliquées — seules les limites de niveau tenant (si explicitement récupérées via `listActive`) pourraient l'être, mais elles ne le sont pas non plus.

`LimitContext` contient les identifiants nécessaires : `tenantId`, `agentId`, `terminalId`, `outletId`, `drawChannelId`, `drawId`. Le port `LimitAssignmentReaderPort.listActiveForTargets(List<LimitTarget> targets, Instant now)` attend exactement une liste de `LimitTarget` construits depuis ces identifiants.

La hiérarchie `LimitTarget` est un sealed type (`LimitTarget.Tenant`, `LimitTarget.Outlet`, `LimitTarget.Agent`, `LimitTarget.Terminal`, `LimitTarget.DrawChannel`).

Par ailleurs, `LimitAssignmentReaderPort.listActive(TenantId)` contient aussi un placeholder identique dans sa default implementation — il faudra que l'implémentation concrète soit correcte.

## Goals / Non-Goals

**Goals:**

- `LimitPolicyRuntimeService.evaluate()` construit la liste des `LimitTarget` depuis le `LimitContext` et la passe à `assignments.listActiveForTargets()`
- Les assignments nulls (quand `agentId`, `terminalId`, `outletId`, `drawChannelId` sont null dans le contexte) sont simplement omis de la liste
- L'implémentation concrète de `LimitAssignmentReaderPort` est vérifiée pour correctement gérer une liste de cibles non vide
- Tests unitaires couvrant la construction des cibles et l'appel complet

**Non-Goals:**

- Refonte du moteur d'évaluation (`InProcessLimitEvaluationEngine`)
- Correction du scoring `LimitResolver` (traité dans `fix-limitpolicy-resolver-scoring`)
- Exposition HTTP du résultat d'évaluation (change séparé si nécessaire)

## Decisions

### D1 — Construction des LimitTarget dans le service

Trois options :

1. Construire la liste dans `LimitPolicyRuntimeService.evaluate()` directement
2. Ajouter une méthode helper `LimitContext.toTargets()` dans le record
3. Créer un `LimitTargetExtractor` utilitaire

**Décision** : option 2. `LimitContext.toTargets()` est plus cohérent avec le domaine (le contexte sait de quelles cibles il parle) et simplifie le service. Le service reste lisible :

```java
var assigns = assignments.listActiveForTargets(ctx.toTargets(), Instant.now());
```

### D2 — Gestion des nulls dans toTargets()

Les champs `agentId`, `terminalId`, `outletId`, `drawChannelId` sont optionnels. `toTargets()` retourne uniquement les targets non-null. `tenantId` est toujours inclus (obligatoire).

### D3 — @Autowired(required = false) pour engine

`InProcessLimitEvaluationEngine` et `ExposureFactsReaderPort` sont déjà `@Autowired(required = false)`. Ce pattern est acceptable pour un MVP mais signale que le service n'est pas complet. À laisser tel quel pour ce change.

**Note** : le `@Autowired` champ sur `exposureFacts` et `engine` viole la règle "constructor injection uniquement" du projet. Ce sera corrigé dans un ADR ou change séparé.

## Risks / Trade-offs

- **[Risque] Performance** : `listActiveForTargets` avec une liste de 4-5 cibles peut générer une requête `IN (...)` volumineuse si mal indexée. → Vérifier l'implémentation de `LimitAssignmentPersistenceAdapter` et les indexes sur `limit_assignment`.
- **[Risque] Comportement silencieux actuel** : des features qui dépendent de l'évaluation "toujours permissive" pourraient être affectées une fois les limites réellement appliquées. → Communiquer l'impact avant déploiement ; les tests d'intégration existants révèleront les régressions.
- **[Trade-off] Correction partielle** : le `@Autowired` champ reste pour ce change (non-critique pour la correction du placeholder).

## Migration Plan

1. Lire `LimitTarget` sealed type pour comprendre les sous-types disponibles
2. Ajouter `List<LimitTarget> toTargets()` sur `LimitContext`
3. Modifier `LimitPolicyRuntimeService.evaluate()` pour utiliser `ctx.toTargets()`
4. Vérifier `LimitAssignmentPersistenceAdapter.listActiveForTargets()` — s'assurer qu'il supporte une liste non-vide
5. Tests unitaires
6. `./mvnw clean verify`

## Open Questions

- Q1 : `LimitAssignmentPersistenceAdapter.listActiveForTargets()` supporte-t-il bien une liste non-vide ? (à vérifier avant implémentation)
- Q2 : Des features existantes s'appuient-elles sur l'évaluation "toujours permissive" (ALLOW) ? (impact du changement de comportement)
