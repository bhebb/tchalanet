# TCHALANET — PLAYBOOK (SERVER)

> Note de fusion (2026-01-17): Ce document fusionne l'ancien ENGINEERING_PLAYBOOK (principes et règles) avec le guide opératoire PLAYBOOK (comment faire). Il devient la source unique pour l’IA et les développeurs.

## 0. Principes fondamentaux (non négociables)

- Pas de vérité absolue: l’architecture guide sans dogmatisme
- Lisibilité > abstraction
- Domain first: le code raconte l’histoire métier
- Les décisions importantes sont documentées (ADR / spec), jamais devinées

## 1. Stack technique officielle (SERVER)

Runtime

- Java 25
- Spring Boot 4.x
- PostgreSQL 18
- Flyway
- Redis (optionnel)
- Caffeine (L1 cache)

Librairies clés

- MapStruct (mapping)
- Lombok (boilerplate)
- Spring Batch
- Spring Security OAuth2 (Keycloak)
- SpringDoc OpenAPI
- AssertJ (tests)
- JUnit 5

## 2. Conventions Java & Spring (OBLIGATOIRES)

### 2.1 Java

Autorisé / Recommandé

- `var` quand lisible
- `record` pour commands, queries, events, DTOs, wrappers d’ID

Interdit

- UUID dans le domaine (sauf entities JPA / repos)
- Optional comme champ
- logique métier dans controllers
- utilitaires `static` métier

### 2.2 Tests

- AssertJ uniquement; pas de `org.junit.jupiter.api.Assertions.*`
- Grouper avec `assertAll(...)`
- `@Nested` si plusieurs scénarios; `@DisplayName` recommandé

Exemple

```java
@Nested
class WhenTicketIsPending {
  @Test
  @DisplayName("Should reject sale when ticket is pending")
  void shouldRejectSaleWhenTicketIsPending() {
    assertAll(
      () -> assertThat(result.status()).isEqualTo(REJECTED),
      () -> assertThat(result.reason()).isNotNull()
    );
  }
}
```

## 3. Typage fort (WRAPPERS OBLIGATOIRES)

Règle d’or: hors persistence, on n’utilise jamais `UUID`.
Exemple

```java
public record TenantId(UUID value) {
  public TenantId {
    if (value == null) throw new IllegalArgumentException("TenantId is null");
  }
}
```

UUID autorisé: JPA Entity, Spring Data Repo, SQL/Flyway. Interdit: Domain/Application.

## 4. Organisation des packages (SERVER)

```
server/
├─ common/   # transversal technique
├─ catalog/  # référentiels / lookup / tables quasi-statiques
├─ core/     # domaines critiques (argent, tirages, légalité)
└─ features/ # orchestration UI / BFF
```

## 5. Règles de placement (LE PLUS IMPORTANT)

### 5.1 common/

- Rôle: infra, cross-cutting, zéro logique métier
- Exemples: security, context, cache, web (ApiResponse, paging), tx (AfterCommit), time, error, bus
- Interdits: règles métier, dépendance vers core

### 5.2 catalog/

- Données de référence, tables utilisées par plusieurs domaines, peu/pas de logique métier
- Exemples: games, result_slot, pricing tables, tenant_game
- Structure suggérée: api/, domain/, infra/, internal/

### 5.3 core/ (hexagonal strict)

- Si argent/fraude/légalité → core
- Structure:

```
core/<domain>/
├─ domain/model/
├─ application/ (command/query/event)
├─ port/ (in/out)
└─ infra/ (persistence/event/batch/web)
```

### 5.4 features/

- Rôle: orchestration, composition de domaines, BFF, PageModel, dashboards
- Peut appeler: core, catalog, common
- Ne contient pas: règles critiques, calculs financiers

## 6. Controllers — quand/où ?

Autorisé: `features/*/web`, `core/*/infra/web` (si critique et stable)
Interdit: `domain`, `application`
Règle: Controller = mapping + validation + appel handler (CommandBus/QueryBus)

## 7. Events & AfterCommit

Principe: le domaine publie, l’infra réagit, publication après commit.

```java
AfterCommit.run(() -> publisher.publish(event));
```

Cas typiques: Sale → Ledger; Payout → Ledger; DrawResult → Sales; Payout → Notification

## 8. Cache (règles)

- Caffeine = L1, Redis = L2
- TTL métier = Redis; TTL technique = Caffeine
- Déclarer un CacheSpecProvider par domaine

## 9. Sécurité & Contexte

- Vérités: `TchRequestContext`, `TchContextFilter`, RLS via `set_config`
- Le code fourni est canonique; à référencer explicitement

## 10. Sales / Payout / Ledger — Règles transverses

États canoniques

- Ticket: TicketSaleStatus (SOLD | PENDING_APPROVAL | VOID | REJECTED), TicketResultStatus (NOT_RESULTED | WON | LOST | OVERRIDDEN), TicketSettlementStatus (UNSETTLED | SETTLED)
- Payout: REQUESTED → APPROVED → PAID (ou REJECTED)
  Asynchronisme: AfterCommit obligatoire; split payment autorisé; Ledger = source de vérité financière

## 11. Contrat IA (AI Agents Contract)

Vous êtes un agent IA contribuant au projet Tchalanet.
Vous DEVEZ:

- Respecter `ARCHITECTURE.md` et ce PLAYBOOK
- Utiliser les wrappers d’ID (TenantId, TicketId, etc.)
- Ne jamais introduire UUID dans le domaine
- Utiliser CommandBus / QueryBus
- Publier les événements de domaine APRÈS commit
- Suivre l’architecture hexagonale dans `core`
- Utiliser AssertJ pour les tests
  Vous NE DEVEZ PAS:
- Ajouter de logique dans les controllers
- Bypasser RLS
- Inventer des patterns d’architecture
- Ignorer les frontières de domaine existantes

---

# Guide opératoire (comment ajouter une feature)

> Objectif : ajouter une feature sans casser l’architecture, sans sur-réglementer, et sans oublier les briques (context, RLS, migrations, cache, OpenAPI, tests).

---

## 0) Les 3 fichiers “source de vérité”

Quand tu changes quelque chose d’important, tu le notes ici :

1. `VERSIONS.md` : runtimes/build/services/containers (pas de changements silencieux)
2. `AGENTS.md` : règles impératives pour IA (normatif, court)
3. `ARCHITECTURE.md` : patterns, frontières, compromis (explicatif)

---

## 1) Choisir où mettre le code (Common/Core/Feature/Catalog)

### 1.1 Arbre de décision (rapide)

**A. Ça touche l’argent / ticket / ventes / tirages / fraude / conformité ?**

➡️ `core/<domain>`

**B. C’est une page / un BFF / un agrégateur multi-domaines (Home, dashboards, pagemodel) ?**

➡️ `features/<slice>`

**C. C’est un référentiel / catalogue / mapping stable / table “lookup” (peu de logique) ?**

➡️ `catalog/<name>` (ou `core/<domain>` si critique et fortement métier)

**D. C’est technique transversal (context, paging, error, cache infra, tx, time) ?**

➡️ `common/`

> Il n’y a pas une vérité absolue :
>
> mais tout écart doit être **cohérent** et **documenté** (ARCHITECTURE).

---

## 2) Ajouter une API REST (controller)

### 2.1 Choisir le scope / path

Rappels :

- PUBLIC : `/api/v1/public/**`
- TENANT : `/api/v1/tenant/**`
- ADMIN : `/api/v1/admin/**`
- PLATFORM : `/api/v1/platform/**`
- SDR : `/_sdr/**`

✅ Le controller doit vivre dans :

- `features/<slice>/...infra.web` si orchestration / BFF / pages
- `core/<domain>/infra.web` si API métier pure de ce domaine
- `catalog/<name>/infra.web` si admin CRUD catalog / read API interne

### 2.2 Controller : règles

- Pas de logique métier (validation/mapping + appel handler/service)
- Retour 2xx via `ApiResponse<T>`
- Erreurs via `ProblemRest` / exceptions dédiées
- Paging via `@TchPaging TchPageRequest` (voir `PAGINATION.md`)
- Context via `@CurrentContext TchRequestContext`

### 2.3 Exemple de controller (template)

```java
@RestController
@RequestMapping("/tenant/draws")
@RequiredArgsConstructor
public class DrawQueryController {

   private final ListDrawsHandler listDraws;

   @GetMapping
   public ApiResponse<TchPage<DrawItemDto>> list(
      @CurrentContext TchRequestContext ctx,
      @TchPaging(allowedSort = {"occurredAt","status"}, defaultSort = {"occurredAt,desc"})
          TchPageRequest page,
      @RequestParam(required = false) String channel) {
     var q = new ListDrawsQuery(ctx.effectiveTenantUuidRequired(), channel, page.pageable());
     var out = listDraws.handle(q);
     return ApiResponse.success(out);
   }
}
```

## 3) Ajouter un Use Case (handler) + modèles (command/query)

### 3.1 Où le mettre

core/<domain>/application/command|query/...

features/<slice>/... si orchestration pure

catalog/<name>/... si lookup/crud référentiel

### 3.2 Checklist handler

record pour le command/query model

handler @UseCase

@TchTx sur command qui écrit

Lecture seule : pas forcément de transaction

Publier DomainEventPublisher si événement métier

Si “publish après commit” → AfterCommit.run(...)

### 3.3 Exemple query

```java
public record GetTicketStatusQuery(UUID tenantId, String publicCode) {}

@UseCase
@RequiredArgsConstructor
public class GetTicketStatusHandler implements QueryHandler<GetTicketStatusQuery, TicketStatusView> {

  private final TicketReaderPort reader;

  @Override
  public TicketStatusView handle(GetTicketStatusQuery q) {
    return reader.findStatusByPublicCode(q.tenantId(), q.publicCode())
        .orElseThrow(() -> ProblemRest.notFound("ticket.not_found", "Ticket not found"));
  }
}
```

### 3.4 Exemple command

```java
public record CancelTicketCommand(UUID tenantId, UUID ticketId, String reason) {}

@UseCase
@RequiredArgsConstructor
public class CancelTicketHandler implements CommandHandler<CancelTicketCommand, CancelResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx
  public CancelResult handle(CancelTicketCommand c) {
    var res = writer.cancel(c.tenantId(), c.ticketId(), c.reason());
    AfterCommit.run(() -> events.publish(new TicketCancelledEvent(c.tenantId(), c.ticketId())));
    return res;
  }
}
```

## 4) Ajouter un Port + Adapter (persistence)

### 4.1 Port (interface) — côté domain/application

Chemin : core/<domain>/port/out/...

Lire = ReaderPort

Écrire = WriterPort / RepositoryPort

Signatures orientées domain / use-case, pas JPA

Ex :

```java
public interface TicketReaderPort {
  Optional<TicketStatusView> findStatusByPublicCode(UUID tenantId, String publicCode);
}
```

### 4.2 Adapter JPA — côté infra

Chemin : core/<domain>/infra/persistence/...

Entité JPA = \*JpaEntity

Mapper MapStruct = \*Mapper

Repo Spring Data = \*JpaRepository

Ex :

```java
@Repository
@RequiredArgsConstructor
public class TicketReaderAdapter implements TicketReaderPort {

  private final TicketJpaRepository repo;
  private final TicketMapper mapper;

  @Override
  public Optional<TicketStatusView> findStatusByPublicCode(UUID tenantId, String publicCode) {
    return repo.findByTenantIdAndPublicCode(tenantId, publicCode).map(mapper::toStatusView);
  }
}
```

## 5) Ajouter une Entity + Repository

### 5.1 Entity : règles

Champ tenant_id si tenant-scoped

Champ deleted_at si soft delete

Index utiles

Envers si audit sur cette table

Pas de logique métier complexe dans l’entité JPA

### 5.2 Repository : règles

Requêtes tenant-safe (toujours filtrer tenant si nécessaire)

Idéalement laisser RLS faire, mais le filtrage explicite est OK pour clarté

Préférer Optional<> / paging

## 6) Ajouter une migration Flyway

### 6.1 Checklist migration

db/migration/Vxx\_\_<name>.sql

Ajouter table / colonnes / index

Ajouter contraintes multi-tenant

Ajouter tenant_id + deleted_at si applicable

Ajouter dans la liste de tables RLS (via script policies) OU gérer via tes générateurs

### 6.2 Exemple table tenant-scoped

-- V52\_\_create_page_model.sql
create table if not exists page_model (
id uuid primary key,
tenant_id uuid,
logical_page_id text not null,
scope text not null,
role text,
status text not null,
schema_version int not null,
page_model_json jsonb not null,
version int not null default 1,
created_at timestamptz not null default now(),
updated_at timestamptz not null default now(),
deleted_at timestamptz
);

create index if not exists idx_page_model_tenant on page_model(tenant_id);
create index if not exists idx_page_model_lookup on page_model(tenant_id, logical_page_id, role, status);

### 6.3 RLS

Assure-toi que la table soit incluse dans ta migration RLS (ex: V40\_\_rls_policies.sql)

tenant_id requis pour tables tenant-scoped

deleted_at requis si tu veux la visibilité soft delete

## 7) Utiliser le Contexte (tenant/user/roles)

### 7.1 Principes

TchContextFilter est la source de vérité request-scoped

@CurrentContext pour injection dans controller

RLS se base sur app.current_tenant et app.deleted_visibility

SUPER_ADMIN peut override via header/query

### 7.2 Pattern recommandé

Controller lit le contexte

Handler prend tenantId explicitement dans command/query

Adapter DB n’invente jamais un tenantId

## 8) Ajouter de l’Audit (annotation + Envers)

### 8.1 AuditLog annotation

Utiliser @AuditLog(entity=..., action=..., idExpression=..., detailsExpression=...) sur controller ou handler si tu veux log d’action.

Audit “actions” : utile pour trails fonctionnels

Envers : utile pour historiser les modifications DB

### 8.2 Envers

Config déjà en place via hibernate.envers.\*

Ajoute Envers sur une entité si besoin (selon ta convention)

Assure-toi que la table audit_event est RLS-safe si tenant-scoped

## 9) Cache : comment décider et implémenter

### 9.1 Quand cacher ?

✅ À cacher :

pages publiques (home, pageModel résolu)

tenant config / theme

news

search queries

draws summary (TTL court)

❌ À éviter :

opérations critiques d’argent

actions très sensibles de permissions (TTL trop long)

### 9.2 Comment déclarer un cache

Ajouter un CacheSpecProvider dans le module concerné

TTL L2 = métier ; L1 = technique (court)

### 9.3 @Cacheable

Toujours utiliser cacheNames = "..." du module

Clés paramétrées via builder si besoin

## 10) OpenAPI / Springdoc

### 10.1 Groupes

Tes groupes OpenAPI sont déjà en place :

public/platform/admin/tenant/sdr

### 10.2 Tagging SDR

Ton OpenApiGroupsConfig retag automatiquement /\_sdr/\*\* en SDR • <Resource>

### 10.3 Sécurité swagger

Ton OpenApiConfig expose :

bearerAuth + oauth2 (Keycloak)

Checklist quand tu ajoutes des routes :

respecter scope path

vérifier que swagger les montrent dans le bon group

## 11) Spring Data REST (SDR) — quand et comment

Usage acceptable

CRUD admin “simple” sur référentiels (catalog)

endpoints sous /\_sdr/\*\*

sécurité : roles admin/superadmin

Checklist

spring.data.rest.base-path: /\_sdr

detection-strategy: annotated (important)

ajouter les entités exposées avec @RepositoryRestResource uniquement

ne jamais exposer des entités core critiques via SDR

## 12) Tests (unitaires) — standard Tchalanet

Règles

JUnit5 + AssertJ

@Nested par scénario

assertAll(...) pour regroupements

pas de mocks abusifs : tester la logique, pas Spring

Template

```java
class ResolveCurrentLangTest {

  @Nested
  @DisplayName("When URL lang is provided")
  class WhenUrlLangProvided {

    @Test
    @DisplayName("Should use URL lang when allowed")
    void shouldUseUrlLangWhenAllowed() {
      // given

      var input = "...";

      // when

      var res = resolve(input);

      // then

      assertThat(res).isEqualTo("fr");
    }
  }
}
```

Guidelines :

- méthodes de test en camelCase Java-compatibles
- ajouter `@DisplayName("...")` lisible pour le test
- laisser une ligne vide après les marqueurs `// given`, `// when`, `// then` dans les exemples

## 13) “Ajouter un nouveau domaine” (core)

Checklist :

créer core/<domain>/domain

créer application/command|query

créer port/out

créer infra/persistence (adapter + repo + mapper)

migrations Flyway

RLS policies (tenant_id + deleted_at)

endpoints REST (si exposés)

tests unitaires

doc (ARCHITECTURE si pattern nouveau)

## 14) “Ajouter une nouvelle feature BFF” (features)

Checklist :

features/<slice>/...infra.web (controller)

DTO/mapper

appels handlers core

agrégation (ApiResponse)

cache (si pertinent)

tests (unitaires + e2e si besoin)

## 15) PageModel — playbook opérationnel

Ajouter un nouveau widget dynamique

définir widget id + type dans schema JSON

ajouter provider côté backend (dans la feature pagemodel ou slice cible)

provider appelle les domaines nécessaires (draw/news/plans/etc.)

réponse merge dans dynamic

tests : résolution page + fallback + lang

mettre à jour doc “widgets registry”

## 16) Checklist “PR prête”

Avant merge :

compile backend (./mvnw -q -DskipTests=false test)

migrations Flyway ok

aucune fuite de tenant (RLS ok)

API scope correct

Swagger affiche route au bon endroit

tests AssertJ only

doc mise à jour si décision/pattern/version

## 17) Frontend Web — playbook rapide

Quand tu ajoutes une feature web :

respecter Angular 20 + Material

mobile-first (breakpoints 480/768/1024)

tokens CSS vars (pas de couleurs hardcodées)

i18n namespaces en snake_case

widgets rendus via WidgetRenderer

## 18) Exemples d’écarts acceptables (et comment les documenter)
