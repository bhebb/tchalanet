# Refactor — Draw bounded context

## Contexte général

Nous avons un bounded context `draw` dans un backend Spring Boot / Java, avec une architecture hexagonale.

L'objectif est de structurer proprement le BC `draw` sans changer la logique métier, en alignant les classes existantes sur :

- Commands & CommandHandlers
- Queries & QueryHandlers
- Use cases (`application.command.handler` & `application.query.handler`)
- Ports OUT
- Batchs
- Web controllers

> Règles d'or :
>
> - `@UseCase` doit être utilisé à la place de `@Service` sur les use cases.
> - `@TchTx` doit être utilisé à la place de `@Transactional` sur les méthodes/classes transactionnelles.

---

## 1. Ports IN – Command side

**Package** : `com.tchalanet.server.draw.application.ports.in`

Créer / organiser les interfaces suivantes (ports IN). Ces interfaces ne contiennent que des signatures de méthodes. Les implémentations seront dans `application.command.handler` avec `@UseCase`.

### 1.1 Cycle de vie & génération

```java
public interface CloseDueDrawsCommandHandler {
    void handle(); // multi-tenant à l’intérieur
}
```

À créer aussi (interfaces, même si les impls viendront plus tard) :

- `GenerateDrawsForDateRangeCommandHandler`
- `CancelDrawCommandHandler`
- `ReopenDrawForSalesCommandHandler`
- `OpenDueDrawsCommandHandler`

### 1.2 Résultats

```java
public interface FetchAndApplyExternalResultCommandHandler {
    void handle(UUID drawId);
}
```

Et les interfaces :

- `RecordManualDrawResultCommandHandler`
- `OverrideDrawResultCommandHandler`
- `InvalidateDrawResultCommandHandler` (optionnel pour V2)

### 1.3 Settlement

```java
public interface SettleDrawCommandHandler {
    void handle(UUID drawId);
}
```

Et en option :

- `RetrySettleDrawCommandHandler`
- `ResetSettlementForDrawCommandHandler`

### 1.4 Cache / maintenance

- `RefreshInternalDrawCacheCommandHandler`
- `RefreshPublicDrawsCacheCommandHandler`
- `RebuildDrawReadModelsCommandHandler` (optionnel)

> À FAIRE : Adapter les classes existantes pour qu’elles implémentent ces interfaces (dans `application.command.handler`).
> Ne pas changer la logique métier interne, seulement l’architecture et les noms.

---

## 2. Commands – modèles

**Package** : `com.tchalanet.server.draw.application.command.model`

Créer des `record`s pour les commands (DTO d’entrée des use cases) si ce n’est pas déjà fait.

Exemples :

- `CloseDueDrawsCommand` (facultatif si `handle()` sans param)
- `GenerateDrawsForDateRangeCommand`
- `CancelDrawCommand`
- `ReopenDrawForSalesCommand`
- `OpenDueDrawsCommand`
- `FetchAndApplyExternalResultCommand` (record avec `drawId` et éventuels paramètres de provider)
- `RecordManualDrawResultCommand`
- `OverrideDrawResultCommand`
- `InvalidateDrawResultCommand`
- `SettleDrawCommand`
- `RetrySettleDrawCommand`
- `ResetSettlementForDrawCommand`
- `RefreshInternalDrawCacheCommand`
- `RefreshPublicDrawsCacheCommand`
- `RebuildDrawReadModelsCommand`

**IMPORTANT** : Ne pas inventer de nouveaux champs dans ces records si ce n’est pas nécessaire. Mapper proprement ce qui existe déjà vers ces modèles.

### Mapping & DTO conversions

Pour les conversions entre les DTO HTTP (`infra.web.model`) et les Commands/Queries, utiliser MapStruct (préféré) :

- Créer des interfaces `Mapper` dans `draw.infra.web.mapper`.
- Configurer MapStruct avec `componentModel = "spring"` pour injection.
- Tester les mappers (unit tests) pour garantir les conversions.

Exemple :

```java
@Mapper(componentModel = "spring")
public interface DrawWebMapper {
  CreateDrawCommand toCreateDrawCommand(CreateDrawRequest request);
  DrawSummaryResponse toDrawSummaryResponse(Draw draw);
}
```

---

## 3. Use cases Command – impl

**Package** : `com.tchalanet.server.draw.application.command.handler`

Règles :

- Annoter les classes avec `@UseCase`.
- Annoter la classe ou la méthode principale avec `@TchTx` si c’est transactionnel.
- Implémenter les interfaces définies dans `port.in`.

Exemples de refactor :

### CloseDueDrawsCommandHandlerImpl

```java
@UseCase
public class CloseDueDrawsCommandHandlerImpl implements CloseDueDrawsCommandHandler {

    @TchTx
    @Override
    public void handle() {
        // logique métier existante
    }
}
```

### ApplyDrawResultService → FetchAndApplyExternalResultUseCase

Renommer `ApplyDrawResultService` en `FetchAndApplyExternalResultUseCase` et implémenter `FetchAndApplyExternalResultCommandHandler` :

```java
@UseCase
public class FetchAndApplyExternalResultUseCase implements FetchAndApplyExternalResultCommandHandler {

    @TchTx
    @Override
    public void handle(UUID drawId) {
        // logique actuelle d'ApplyDrawResultService
    }
}
```

### Autres :

- `SettleDrawsUseCaseImpl` → implémenter `SettleDrawCommandHandler`, annoter `@UseCase` + `@TchTx`.
- `RefreshDrawCacheService` → renommer en `RefreshInternalDrawCacheUseCase`, implémenter `RefreshInternalDrawCacheCommandHandler`, annoter `@UseCase` et `@TchTx` si nécessaire.
- `RefreshPublicDrawsCacheUseCaseImpl` → implémenter `RefreshPublicDrawsCacheCommandHandler`, annoter `@UseCase`.

> Règle pour Copilot : Ne modifie pas le corps des méthodes (logique métier). Ajoute juste `@UseCase`, `@TchTx` et `implements <HandlerInterface>`.

---

## 4. Port IN – Query side

**Package** : `com.tchalanet.server.draw.application.port.in`

Créer les interfaces de query handlers (mêmes règles que pour les commands) :

- `GetNextDrawQueryHandler`
- `GetNextDrawsQueryHandler`
- `ListDrawsQueryHandler`
- `ListTodayDrawsQueryHandler`
- `ListLast7DaysDrawsQueryHandler`
- `ListTodayResultsQueryHandler`
- `ListLast7DaysResultsQueryHandler`
- `ListLastDaysResultsQueryHandler`
- `GetPublicHomePageDrawsQueryHandler`
- `GetPublicDrawsSummaryQueryHandler`
- `GetPublicResultsSummaryQueryHandler`
- `GetDrawDetailsQueryHandler`
- `GetDrawResultQueryHandler`
- `GetDrawSettlementStatusQueryHandler`
- `ListUnsettledResultedDrawsQueryHandler`

Chaque interface expose des méthodes prenant une _Query_ (record) du package `application.query.model` et retournant le résultat approprié (DTO ou modèle).

---

## 5. Queries – modèles

**Package** : `com.tchalanet.server.draw.application.query.model`

Créer des `record`s pour les queries, par exemple :

- `GetNextDrawQuery`
- `GetNextDrawsQuery`
- `ListDrawsQuery`
- `ListTodayDrawsQuery`
- `ListLast7DaysDrawsQuery`
- `ListTodayResultsQuery`
- `ListLast7DaysResultsQuery`
- `ListLastDaysResultsQuery`
- `GetPublicHomePageDrawsQuery`
- `GetPublicDrawsSummaryQuery`
- `GetPublicResultsSummaryQuery`
- `GetDrawDetailsQuery`
- `GetDrawResultQuery`
- `GetDrawSettlementStatusQuery`
- `ListUnsettledResultedDrawsQuery`

Ces records doivent refléter les paramètres déjà utilisés dans les services existants (`tenantId`, `channelId`, dates, pagination, etc.).

---

## 6. Use cases Query – impl

**Package** : `com.tchalanet.server.draw.application.query.handler`

Refactorer les services/UseCases de lecture existants pour qu’ils :

- soient dans ce package,
- soient annotés avec `@UseCase`,
- implémentent les interfaces de query handlers dans `port.in`,
- consomment les Query records de `application.query.model`.

Exemples :

- `GetNextDrawUseCaseImpl` → `@UseCase`, `implements GetNextDrawQueryHandler`
- `GetNextDrawsUseCaseImpl`
- `ListDrawsUseCaseImpl`
- `ListTodayResultsUseCaseImpl`
- `ListLast7DaysResultsUseCaseImpl`
- `GetPublicHomePageUseCaseImpl`

Ne pas changer la logique métier, juste le câblage et les annotations.

---

## 7. Ports OUT – `draw.application.port.out`

**Package** : `com.tchalanet.server.draw.application.port.out`

Garder / adapter :

- `ExternalDrawResultPort`
- `FindFetchableDrawIdsPort`
- `FindSettleableDrawIdsPort`

Créer (interfaces vides ou basiques pour l’instant) :

- `DrawReaderPort`
- `DrawWriterPort`
- `DrawChannelReaderPort`
- `DrawChannelWriterPort`
- `PublicDrawsCachePort` (pour Redis/Caffeine)

Les impls seront dans `draw.infra.persistence`, `draw.infra.external`, `draw.infra.cache`.

---

## 8. Batch – `draw.infra.batch`

**Objectif** : les batch steps **appellent les port IN (handlers)**, pas des services concrets.

- **CloseDrawsTasklet** : dépend de `CloseDueDrawsCommandHandler`.
  - `execute` : `handler.handle();` puis `RepeatStatus.FINISHED`.
- **Job** `close_due_draws` (`CloseDueDrawsJobConfig`) : step `close_due_draws_step` avec tasklet.

- **FetchDrawResultsJobConfig**

  - `ItemReader<UUID>` = `FetchableDrawIdsReader` (utilise `FindFetchableDrawIdsPort`).
  - `ItemWriter<UUID>` : boucle sur les IDs et appelle `FetchAndApplyExternalResultCommandHandler.handle(drawId)`.

- **SettleDrawsJobConfig**
  - `ItemReader<UUID>` = `SettleableDrawIdsReader` (utilise `FindSettleableDrawIdsPort`).
  - `ItemWriter<UUID>` : boucle sur les IDs et appelle `SettleDrawCommandHandler.handle(drawId)`.

---

## 9. Web – `draw.infra.web`

Règles minimales pour ce refactor :

- Les controllers REST dépendent des port IN (`CommandHandler`, `QueryHandler`) et des modèles (`Command`/`Query`), **pas** des impls concrètes / services.
- Ne change pas les endpoints HTTP (path/méthode) pour l’instant.
- Adapte uniquement les injections de dépendances et les types.

**Précision importante (DTO HTTP)** : les controllers doivent exposer une API HTTP basée sur des DTO HTTP situés dans `draw.infra.web.model` (ou `draw.infra.web.dto` selon la convention choisie). Concrètement :

- En entrée, utiliser `web.model.[X]Request` (ex. `CreateDrawRequest`, `FetchResultsRequest`).
- En sortie, renvoyer `web.model.[X]Response` (ex. `DrawSummaryResponse`, `DrawDetailsResponse`).

Les controllers **ne** doivent pas appeler directement les objets `application.command.model` ou les repositories : ils doivent mapper les `web.model.*Request` en `application.command.model.*` (Commands) ou `application.query.model.*` (Queries), puis appeler les ports IN (`CommandHandler` / `QueryHandler`). Ce mapping se fait dans le controller (ou via mappers dédiés dans `infra.web.mapper`).

### Audit & annotation `@AuditLog`

Pour l'audit nous prioriserons l'utilisation de l'annotation `@AuditLog` sur les méthodes pertinentes (controllers ou use cases). L'aspect lié effectuera la collecte du contexte via SpEL et construira un `AuditEvent` via `AuditEventFactory` (ou un command léger) puis délèguera l'écriture au `LogAuditEvent` handler.

- L'annotation `@AuditLog` doit être documentée et appliquée sur les méthodes métiers critiques (ex : override de résultat, annulation de ticket, update de plan).
- L'aspect ne doit pas effectuer d'appels bloquants ou volumineux : il construit l'event et délègue.

### Records vs Lombok

- Priorité : utiliser `record` pour les DTO immuables (Commands, Queries, Response DTO simples).
- Si l'objet nécessite des annotations/framework (JPA entity, builders complexes), utiliser une `class` et Lombok pour réduire le boilerplate.

Cette convention aide à garder le code lisible et les responsabilités claires.

---

## 10. Règles pour Copilot

- Ne supprime pas de classes sans certitude. Si tu vois un doublon, garde la classe principale et mets `// TODO` sur l’ancienne.
- Ne modifie pas la logique métier interne des méthodes. Le refactor est principalement :
  - déplacement de classes,
  - renommage,
  - implémentation des bonnes interfaces,
  - ajout de `@UseCase` et `@TchTx`.

**Respecte strictement les packages** :

- Commands → `application.command.model`
- Command usecases → `application.command.handler`
- Queries → `application.query.model`
- Query usecases → `application.query.handler`
- Ports in/out → `application.port.in` / `application.ports.out`

---

> Fichier source : `Refactor-draw-domain` (extrait)
