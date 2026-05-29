# RFC — Architecture à intensité variable pour `core/`

**Projet** : Tchalanet  
**Statut proposé** : RFC / discussion avec Claude  
**But** : réduire le boilerplate dans `core/` sans fragiliser les frontières Clean Architecture, CQRS, RLS et multi-tenant.

---

## 1. Position de départ

Tchalanet utilise une architecture `core/` orientée Clean Architecture / Hexagonal / CQRS.

Cette architecture reste nécessaire pour les domaines critiques :

- vente de tickets ;
- annulation / void ;
- payout ;
- tirage / résultats ;
- settlement ;
- limites ;
- promotion appliquée ;
- commission agent ;
- offline sync ;
- sécurité opérationnelle.

Mais l'application contient aussi beaucoup de lectures simples, listes admin, dashboards et historiques. Pour ces cas, appliquer systématiquement le même niveau de cérémonie crée trop de classes pass-through : `Port`, `Adapter`, `Entity`, `Domain Model`, `Mapper`, `View`, etc.

La décision proposée est donc :

```text
Un seul style architectural pour core : Hexagonal / Ports & Adapters.
Mais une intensité variable selon le type de use case.
```

---

## 2. Décision proposée

Tchalanet adopte une **architecture à intensité variable** dans `core`.

Le principe central :

```text
Les handlers restent protégés de l'infrastructure.
Mais les reads simples peuvent utiliser des projections directes derrière un ReadPort minimal.
```

Donc :

```text
Interdit : QueryHandler -> JpaRepository
Autorisé : QueryHandler -> ReadPort -> JpaAdapter -> ProjectionRepository
```

La réduction de boilerplate se fait **dans l'infrastructure**, pas en laissant l'application manipuler JPA.

---

## 3. Non-négociables

### 3.1 Aucun handler ne dépend de JPA

Un `CommandHandler` ou un `QueryHandler` ne doit jamais injecter ni manipuler directement :

- `JpaRepository` ;
- `@Entity` JPA ;
- mapper de persistance ;
- `EntityManager` ;
- repository Spring Data ;
- DTO ou type technique d'infrastructure.

Le handler dépend de ports, du bus, de modèles de lecture stables, de policies pures ou de services applicatifs.

### 3.2 La couche application ne dépend pas de `infra`

Aucun package `internal/application/**` ne doit importer :

```text
..infra.persistence..
..infra.web..
..infra.cache..
..infra.batch..
..infra.scheduler..
```

Cette règle doit rester protégée par ArchUnit.

### 3.3 RLS reste la source de vérité tenant-scoped

Pour les endpoints tenant-scoped, le `tenantId` vient du `TchRequestContext` et de la session RLS en base.

Par défaut, on ne fait pas :

```java
WHERE t.tenantId = :tenantId
```

On fait plutôt :

```java
WHERE t.deletedAt IS NULL
```

RLS applique le filtrage `tenant_id`.

Exceptions possibles :

- batch multi-tenant explicite ;
- super-admin override ;
- seed/startup ;
- replay/event processing documenté ;
- flow public volontairement multi-tenant ;
- correction ops explicitement auditée.

### 3.4 Les DTO web ne deviennent pas des projections core

Une projection directe Spring Data peut cibler un record stable du domaine :

```text
core.<domain>.api.query.model.*
core.<domain>.api.model.*
```

Elle ne doit pas cibler :

```text
infra.web.*Response
features.*.model.*
DTO orienté écran spécifique
contrat BFF temporaire
```

Si la forme est trop orientée écran, elle appartient à `features/` qui orchestre via `QueryBus`.

---

## 4. Classification des use cases

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                              Architecture à intensité variable                               │
├──────────────────────┬──────────────────────┬──────────────────────┬──────────────────────┤
│ Niveau 1             │ Niveau 2             │ Niveau 3             │ Niveau 4             │
│ Query projection     │ Query métier         │ Command simple       │ Command critique     │
├──────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┤
│ Handler              │ Handler              │ Handler              │ Handler              │
│   └── ReadPort       │   └── ReadPorts      │   └── WritePort      │   └── Ports          │
│        └── Adapter   │        / QueryBus    │        └── Adapter   │        └── Domain    │
│             └── Repo │                      │             └── JPA  │             └── Event│
│ Projection directe   │ Orchestration read   │ Mutation simple      │ Invariants forts     │
└──────────────────────┴──────────────────────┴──────────────────────┴──────────────────────┘
```

---

## 5. Niveau 1 — Query projection simple

### Objectif

Afficher des listes, historiques, summaries ou tableaux de bord sans logique métier associée.

Exemples :

- liste de tickets ;
- historique des payouts ;
- liste de sessions ;
- dashboard vendeur simple ;
- résumé outlet ;
- table admin read-only.

### Flux canonique

```text
QueryHandler
  -> ReadPort minimal
      -> JpaAdapter
          -> ProjectionRepository
              -> Record stable de lecture
```

### Règles

- Le handler reste dans `application/query/handler`.
- Le port est minimal et public.
- L'adapter est package-private.
- Le repository Spring Data est package-private.
- Aucun aggregate domaine n'est requis.
- Aucun mapper MapStruct n'est requis si la projection est déjà le bon record.
- Pas de `tenantId` dans le port/query par défaut pour les flows tenant-scoped.
- Pagination standard avec `TchPage<T>`.

### Exemple canonique

#### API record stable

```java
package com.tchalanet.server.core.sales.api.query.model;

import com.tchalanet.server.common.types.id.TicketId;
import java.math.BigDecimal;

public record TicketSummaryView(
    TicketId id,
    String ticketCode,
    BigDecimal totalAmount,
    TicketSaleStatus saleStatus
) {}
```

#### Query

```java
package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.api.query.model.TicketSummaryView;
import org.springframework.data.domain.Pageable;

public record ListTicketsQuery(
    Pageable pageable
) implements Query<TchPage<TicketSummaryView>> {}
```

#### ReadPort

```java
package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.api.query.model.TicketSummaryView;
import org.springframework.data.domain.Pageable;

public interface TicketReadPort {
    TchPage<TicketSummaryView> findSummaries(Pageable pageable);
}
```

#### Handler

```java
package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.api.query.model.TicketSummaryView;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReadPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTicketsQueryHandler
    implements QueryHandler<ListTicketsQuery, TchPage<TicketSummaryView>> {

    private final TicketReadPort readPort;

    @Override
    public TchPage<TicketSummaryView> handle(ListTicketsQuery query) {
        return readPort.findSummaries(query.pageable());
    }
}
```

#### Projection intermédiaire si Typed IDs posent problème en JPQL

JPQL ne convertit pas toujours automatiquement un `UUID` JPA vers un wrapper `TicketId`. Dans ce cas, utiliser une projection infra intermédiaire package-private.

```java
package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import java.math.BigDecimal;
import java.util.UUID;

record TicketSummaryProjection(
    UUID id,
    String ticketCode,
    BigDecimal totalAmount,
    TicketSaleStatus saleStatus
) {}
```

#### Repository package-private

```java
package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface TicketProjectionRepository extends JpaRepository<TicketEntity, UUID> {

    // RLS applique automatiquement tenant_id.
    // On garde seulement les filtres fonctionnels, ex: soft delete.
    @Query("""
        SELECT new com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketSummaryProjection(
            t.id,
            t.ticketCode,
            t.totalAmount,
            t.saleStatus
        )
        FROM TicketEntity t
        WHERE t.deletedAt IS NULL
    """)
    Page<TicketSummaryProjection> findSummaries(Pageable pageable);
}
```

#### Adapter package-private

```java
package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPages;
import com.tchalanet.server.core.sales.api.query.model.TicketSummaryView;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReadPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TicketReadJpaAdapter implements TicketReadPort {

    private final TicketProjectionRepository repository;

    @Override
    public TchPage<TicketSummaryView> findSummaries(Pageable pageable) {
        return TchPages.map(repository.findSummaries(pageable), p ->
            new TicketSummaryView(
                TicketId.of(p.id()),
                p.ticketCode(),
                p.totalAmount(),
                p.saleStatus()
            )
        );
    }
}
```

---

## 6. Niveau 2 — Query métier avec règles

### Objectif

Lire des données en appliquant une évaluation métier ou opérationnelle, sans mutation.

Exemples :

- validation terminal/outlet/session pour une action ;
- éligibilité payout ;
- contexte opérationnel vendeur ;
- permissions combinées avec données métier ;
- readiness d'un draw ;
- affichage qui dépend d'états provenant de plusieurs domaines.

### Flux canonique

```text
QueryHandler
  -> ReadPorts multiples
  -> QueryBus.ask(...)
  -> policies pures si nécessaire
  -> View stable
```

### Règles

- Reste read-only.
- Ne publie pas d'event.
- Ne fait pas de mutation.
- Peut orchestrer plusieurs lectures.
- Peut appeler `QueryBus.ask(...)`.
- Ne doit pas appeler `CommandBus.execute(...)`.
- Si les règles deviennent trop proches d'un workflow ou d'un écran composite, challenger `features/`.

---

## 7. Niveau 3 — Command simple

### Objectif

Modifier une donnée simple, non financière, sans transition métier lourde.

Exemples possibles :

- renommer un label administratif ;
- changer un flag non critique ;
- mise à jour de description ;
- préférence simple ;
- configuration non financière et sans impact settlement.

### Arbitrage obligatoire avant création dans `core`

Avant de créer une command simple dans `core`, vérifier :

1. Est-ce vraiment du `core` ?
2. Est-ce plutôt `catalog` ?
3. Est-ce plutôt `platform` ?
4. Est-ce un endpoint admin CRUD simple ?
5. Est-ce que cette mutation a un impact financier ou opérationnel caché ?

### Conditions d'acceptation

Une command simple dans `core` est autorisée seulement si :

- aucune conséquence financière directe ;
- aucune transition métier sensible ;
- aucun event cross-domain requis ;
- aucun calcul de payout, commission, promotion, limite ou settlement ;
- aucune règle complexe de sécurité ou d'éligibilité ;
- pas mieux placée dans `catalog` ou `platform`.

### Flux canonique

```text
CommandHandler
  -> WritePort
      -> JpaAdapter
          -> Entity simple
```

### Règles

- Le handler reste transactionnel via `@TchTx`.
- Le handler ne manipule pas JPA.
- Le port/adaptateur reste obligatoire.
- Pas de `GenericJpaRepositoryAdapter` global dans `common` sans ADR explicite.
- Si le nombre d'opérations simples augmente dans un même domaine, créer des helpers locaux dans `infra`, pas une abstraction globale prématurée.

---

## 8. Niveau 4 — Command critique

### Objectif

Protéger les opérations où une erreur peut provoquer une perte financière, un litige, une fraude, une mauvaise décision de limite ou un mauvais payout.

Exemples :

- sell ticket ;
- void/cancel ticket ;
- payout approve/pay/reject ;
- draw result apply ;
- settlement ;
- offline sync acceptance/rejection ;
- limit exposure update ;
- promotion application ;
- agent commission ;
- session close si impact financier.

### Flux canonique

```text
CommandHandler
  -> load snapshots / ports
  -> Domain Model / Policy pure Java
  -> state transition
  -> save via ports
  -> AfterCommit.run(event publication)
```

### Règles

- `@TchTx` obligatoire.
- Domain services/policies purs, sans Spring, sans repository, sans bus.
- Events publiés after-commit uniquement.
- Écritures cross-domain interdites dans la même transaction critique.
- Listeners idempotents.
- Retours = result DTO / view, jamais entity/domain aggregate exposé.
- RLS + audit + idempotency selon le use case.

---

## 9. Règles de visibilité Java

### Ports applicatifs

```text
application/port/out/*
```

- `public`.
- Nécessaire pour être implémenté par `infra`.
- Ne doit pas exposer de types JPA.

### Adapters infrastructure

```text
infra/persistence/adapter/*
```

- package-private par défaut.
- Annotés Spring (`@Component`, `@Repository`, etc.) si nécessaire.
- Importables uniquement par Spring, pas par le code application.

### Repositories Spring Data

```text
infra/persistence/repository/*
```

- package-private.
- Utilisés uniquement par adapters.
- Jamais injectés dans handlers.

### Entities JPA

```text
infra/persistence/entity/*
```

- visibilité la plus restreinte compatible avec JPA/Hibernate.
- Jamais exposées hors infra.
- Jamais utilisées dans API, commands, queries, handlers, features.

### Mappers persistence

- package-private si possible.
- utilisés par adapters.
- jamais injectés dans handlers.

---

## 10. Où placer les cas simples : core, catalog, platform ou features ?

### `catalog/`

Utiliser `catalog` pour :

- référentiels read-mostly ;
- définitions ;
- metadata ;
- lookup data ;
- presets ;
- paramètres déclaratifs globaux ;
- game definitions ;
- result slots ;
- i18n keys ;
- theme presets.

### `platform/`

Utiliser `platform` pour :

- services transversaux stateful ;
- audit ;
- accesscontrol ;
- identity ;
- tenant config ;
- tenant theme ;
- communication ;
- notification ;
- document ;
- idempotence.

### `features/`

Utiliser `features` pour :

- payloads orientés écran ;
- dashboards composites ;
- BFF ;
- orchestration multi-domaines ;
- response model spécifique UI ;
- page-level aggregation.

### `core/`

Utiliser `core` pour :

- invariants métier ;
- lifecycle critique ;
- argent ;
- tickets ;
- payout ;
- settlement ;
- draw ;
- offline sync ;
- limit policy ;
- promotion appliquée ;
- agent / commission si impact métier.

---

## 11. Règles ArchUnit à ajouter ou vérifier

### Application ne dépend pas de infra

```text
noClasses()
  .that().resideInAPackage("..core..internal.application..")
  .should().dependOnClassesThat().resideInAnyPackage(
      "..core..internal.infra.."
  )
```

### Handlers ne dépendent pas de Spring Data / JPA

```text
Classes ending with Handler must not depend on:
- org.springframework.data.repository..*
- org.springframework.data.jpa.repository..*
- jakarta.persistence..*
- org.hibernate..*
- ..JpaEntity
- ..Entity
- ..Repository from infra.persistence
```

### Repositories restent dans infra.persistence

```text
Classes assignable to Repository must reside in ..infra.persistence..
```

### Entities restent dans infra.persistence

```text
Classes annotated with @Entity must reside in ..infra.persistence..
```

### Projections core ne ciblent pas web/features

```text
Repositories in core infra.persistence must not return types from:
- ..infra.web..
- ..features..
```

---

## 12. Checklist pour Claude / agent IA

Avant de coder un use case, classer le besoin.

### Questions de classification

1. Est-ce une lecture ou une écriture ?
2. Est-ce tenant-scoped avec RLS ?
3. Est-ce une projection simple ?
4. Est-ce une lecture avec règles métier ?
5. Est-ce une mutation simple non financière ?
6. Est-ce une mutation critique ?
7. Est-ce mieux placé dans `catalog`, `platform` ou `features` ?

### Si Niveau 1

- [ ] `QueryHandler` dépend d'un `ReadPort`.
- [ ] `ReadPort` ne prend pas `tenantId` par défaut.
- [ ] Repository est package-private.
- [ ] Adapter est package-private.
- [ ] Pas d'aggregate domaine.
- [ ] Pas de MapStruct obligatoire.
- [ ] Projection vers record stable `api/query/model` ou projection infra + mapping léger.
- [ ] Pagination avec `TchPage<T>` si liste.

### Si Niveau 2

- [ ] Query read-only.
- [ ] Pas de mutation.
- [ ] Pas d'event.
- [ ] Peut orchestrer `ReadPorts` et `QueryBus.ask`.
- [ ] Si c'est très écran/BFF, déplacer vers `features`.

### Si Niveau 3

- [ ] Mutation non financière.
- [ ] Pas de lifecycle critique.
- [ ] Pas d'event cross-domain.
- [ ] Pas mieux placé dans `catalog/platform`.
- [ ] `CommandHandler -> WritePort -> JpaAdapter`.
- [ ] `@TchTx` sur handler.

### Si Niveau 4

- [ ] Domain model ou policy pure.
- [ ] `@TchTx` obligatoire.
- [ ] Ports explicites.
- [ ] Events after-commit.
- [ ] Idempotency si retry/offline/side effects.
- [ ] Audit/security/RLS vérifiés.
- [ ] Aucun cross-domain write synchrone critique.

---

## 13. Formulation courte de la décision

```text
Tchalanet adopte une architecture à intensité variable dans core.

Les handlers restent isolés de l'infrastructure : aucun JpaRepository, aucune Entity JPA,
aucun mapper persistence dans application.

Les reads simples utilisent un shortcut contrôlé :
QueryHandler -> ReadPort -> JpaAdapter -> ProjectionRepository -> View record.

Les writes critiques restent strictement Clean Architecture / DDD / CQRS :
CommandHandler -> ports -> domain model/policy -> persistence -> events after commit.

Le tenant-scoping des endpoints tenant se fait par TchRequestContext + RLS.
Le tenantId n'est pas filtré manuellement dans les queries tenant-scoped par défaut.

Les projections directes ciblent des records stables de core api/model ou api/query/model,
jamais des DTO web ni des modèles features/BFF.
```

---

## 14. Points à challenger avec Claude

1. Le Niveau 1 garde-t-il assez de protection sans créer trop de classes ?
2. Le `ReadPort` minimal est-il utile ou trop pass-through ?
3. Faut-il autoriser une projection directe vers `api/query/model`, ou toujours passer par projection infra + mapping léger pour Typed IDs ?
4. Où tracer la frontière exacte entre Niveau 2 et `features` ?
5. Quels exemples réels de Tchalanet sont Niveau 3 sans risque caché ?
6. Faut-il interdire complètement les commands simples dans `core` sauf exception documentée ?
7. Quelles règles ArchUnit doivent être ajoutées tout de suite ?
8. Faut-il écrire cette décision dans `docs/conventions/clean_architecture.md`, `docs/modules/core.md`, ou comme ADR séparé ?

---

## 15. Recommandation de placement documentaire

Option recommandée :

```text
docs/03-adr/ADR-00X-core-variable-intensity.md
```

Puis mise à jour synthétique dans :

```text
docs/modules/core.md
docs/conventions/clean_architecture.md
docs/conventions/command_query_handlers.md
```

Ne pas remplacer les règles Clean Architecture existantes. Ajouter cette RFC comme **précision pragmatique** pour éviter le boilerplate sur les reads simples.
