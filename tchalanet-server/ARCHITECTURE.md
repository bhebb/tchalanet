# Architecture applicative Tchalanet (modular monolith / hexagonal)

Ce document décrit l’architecture cible du backend Tchalanet, afin de servir de **référence technique** pendant la refactorisation.

L’objectif est de passer d’un découpage principalement technique (api, services, repository, model, dto, …) à un **modular monolith orienté domaines et use cases**, inspiré de l’architecture hexagonale / clean architecture.

---

## 1. Principes généraux

1. **Découpage par domaine métier** (bounded contexts) plutôt que par couches techniques :

   - `tenant` : configuration tenant, thème, plan d’abonnement, autonomie, limites.
   - `user` : utilisateurs applicatifs (vendeur, admin, superadmin) et préférences.
   - `ticket` : création / validation / consultation des tickets de jeu.
   - `draw` : tirages, horaires, résultats.
   - (plus tard) `sales`, `reporting`, etc.
   - `common` : éléments réellement transverses et partagés (jamais de “poubelle”).

2. **Séparation claire Domain / Application / Infrastructure / Web** dans chaque domaine :

   - `domain.model` : modèle métier pur (pas d’annotations Spring / JPA si possible).
   - `domain.ports` : interfaces nécessaires au domaine (repositories, services externes, horloge, contexte…).
   - `domain.usecase` : cas d’usage applicatifs (orchestration métier). Un use case = une classe.
   - `infra.*` : implémentations techniques des ports (persistence, messaging, billing, …).
   - `web` : API HTTP (controllers, DTO, mappers HTTP).

3. **Hexagonal** :

   - Le domaine dépend uniquement de **ports** et de types de domaine.
   - Les couches `infra` et `web` dépendent du domaine, jamais l’inverse.

4. **Progressivité** :
   - On n’essaie pas de tout déplacer d’un coup.
   - Pour chaque domaine, on commence par introduire les packages et ports, puis on migre use cases et modèles progressivement.

---

## 2. Arbre de packages cible

Tout est sous `com.tchalanet.server`.

```text
com.tchalanet.server
 ├─ common
 │   ├─ domain
 │   │   ├─ model       # Types vraiment transverses (TenantId, UserId, Money...)
 │   │   ├─ ports       # Ports cross-cutting (ex: ClockPort global, EventBusPort...)
 │   │   └─ usecase     # Use cases globaux s’il y en a (rare)
 │   ├─ error           # Exceptions et mappers d’erreur transverses
 │   ├─ web             # Infra web globale (exception handlers, resolvers globaux)
 │   ├─ config          # Configuration Spring globale (CORS, security, OpenAPI, Jackson...)
 │   ├─ security        # Sécurité globale (filtres, config SecurityFilterChain partagée)
 │   └─ context         # Contexte request/tenant/user et utilitaires associés
 │
 ├─ tenant
 │   ├─ domain
 │   │   ├─ model       # Tenant, Plan, Subscription, Theme, limites, policies…
 │   │   ├─ ports       # TenantRepository, SubscriptionRepository, BillingPort, ...
 │   │   └─ usecase     # ConfigureTenantThemeUseCase, ChangePlanUseCase, ...
 │   ├─ infra
 │   │   └─ persistence # Entités JPA, Spring Data, mappers JPA pour tenant
 │   └─ web             # REST tenant/subscription/theme + DTO/mappers HTTP
 │
 ├─ user
 │   ├─ domain
 │   │   ├─ model       # User, UserPreference, rôles applicatifs, ...
 │   │   ├─ ports       # UserRepository, UserPreferenceRepository, ...
 │   │   └─ usecase     # UpsertUserFromJwtUseCase, UpdateUserPreferenceUseCase, ...
 │   ├─ infra
 │   │   └─ persistence # Entités JPA / JDBC pour user + impl des repos
 │   └─ web             # /me, profils, préférences, etc.
 │
 ├─ ticket
 │   ├─ domain
 │   │   ├─ model       # Ticket, TicketId, TicketStatus, etc.
 │   │   ├─ ports       # TicketRepository, TicketSearchPort, ...
 │   │   └─ usecase     # CreateTicketUseCase, VerifyTicketUseCase, ...
 │   ├─ infra
 │   │   └─ persistence # Entités JPA ticket + repos
 │   └─ web             # REST ticket + DTO/mappers
 │
 ├─ draw
 │   ├─ domain
 │   │   ├─ model       # Draw, DrawId, DrawSchedule, Result, ...
 │   │   ├─ ports       # DrawRepository, ExternalResultPort, ...
 │   │   └─ usecase     # ScheduleDrawUseCase, CloseDrawUseCase, ...
 │   ├─ infra
 │   │   └─ persistence # Entités JPA draw + repos
 │   └─ web             # REST draw + DTO/mappers
 │
 └─ (plus tard) sales, reporting, ...
```

Rappel important :

- **`common` ne doit contenir que ce qui est utilisé par au moins 2 domaines.**
- Si un type n’est consommé que dans `ticket`, il reste dans `ticket`, pas dans `common`.

---

## 3. Mapping ancien → nouveau (règles de migration)

### 3.1. Packages techniques existants

| Ancien package                     | Nouveau package cible                                       |
| ---------------------------------- | ----------------------------------------------------------- |
| `api`                              | `<domaine>.web`                                             |
| `services`                         | `<domaine>.domain.usecase`                                  |
| `model` (entités JPA)              | `<domaine>.infra.persistence`                               |
| `model` (modèle métier pur)        | `<domaine>.domain.model`                                    |
| `repository`                       | `<domaine>.infra.persistence` (implémentent des ports)      |
| `dto` (HTTP)                       | `<domaine>.web` (souvent sous-paquet `dto`)                 |
| `dto` (métier / value objects)     | `<domaine>.domain.model`                                    |
| `mapper` (HTTP)                    | `<domaine>.web` (souvent sous-paquet `mapper`)              |
| `mapper` (JPA ↔ domaine)           | `<domaine>.infra.persistence`                               |
| `adapter` (ex: NoopBillingAdapter) | `<domaine>.infra.*` (ex: `tenant.infra.billing`)            |
| `port`                             | `<domaine>.domain.ports`                                    |
| `error` (global)                   | `common.error`                                              |
| `filter` (HTTP)                    | `common.security`                                           |
| `config`, `properties`             | `common.config` (ou config spécifique de domaine si besoin) |
| `resolver` (MVC)                   | `common.web`                                                |
| `context`                          | `common.context`                                            |
| `constants`                        | `common.domain.model` ou `<domaine>.domain.model`           |

### 3.2. Exemples concrets (à partir de classes réelles)

- `com.tchalanet.server.services.DashboardService`
  → `com.tchalanet.server.tenant.domain.usecase.GetDashboardUseCase` (ou plusieurs use cases plus petits).

- `com.tchalanet.server.services.UserService`
  → `com.tchalanet.server.user.domain.usecase.UpsertUserFromJwtUseCase`
  → (éventuellement) `GetCurrentUserUseCase`.

- `com.tchalanet.server.services.UserPreferenceService`
  → `com.tchalanet.server.user.domain.usecase.UpdateUserPreferenceUseCase`.

- `com.tchalanet.server.services.ThemeQueryService`
  → `com.tchalanet.server.tenant.domain.usecase.GetTenantThemesUseCase`.

- `com.tchalanet.server.model.Subscription`, `Plan`, `SubscriptionStatus`, `BillingProvider`
  → `com.tchalanet.server.tenant.domain.model.Subscription` / `Plan` / `SubscriptionStatus` / `BillingProvider`.

- `com.tchalanet.server.model.Theme`
  → `com.tchalanet.server.tenant.domain.model.Theme`.

- `com.tchalanet.server.model.UserPreference`
  → `com.tchalanet.server.user.domain.model.UserPreference`.

- `com.tchalanet.server.filter.RequestUserContextFilter`
  → `com.tchalanet.server.common.security.RequestUserContextFilter`.

- `com.tchalanet.server.filter.DbTenantRlsFilter`
  → `com.tchalanet.server.common.security.DbTenantRlsFilter`.

- `com.tchalanet.server.error.ProblemRestException`
  → `com.tchalanet.server.common.error.ProblemRestException`.

- `com.tchalanet.server.port.BillingResult` (+ futur `BillingPort`)
  → `com.tchalanet.server.tenant.domain.ports.BillingResult` / `BillingPort`.

---

## 4. Conventions de nommage & stéréotypes Spring

### 4.1. Use cases

- Localisation : `*.domain.usecase`.
- Nom : `XxxUseCase` (verbe métier au présent + complément, ex : `CreateTicketUseCase`, `ChangePlanUseCase`).
- Annotation : stéréotype dédié, par exemple :

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface UseCase {
}
```

- Interface vs impl :
  - On peut définir une interface `CreateTicketUseCase` et une impl `CreateTicketUseCaseImpl` annotée `@UseCase`.
  - Les contrôleurs dépendent de l’**interface**.

### 4.2. Ports de domaine

- Localisation : `*.domain.ports`.
- Nom : `XxxRepository`, `XxxPort`, `XxxClient` (selon le rôle).
- Pas d’annotations Spring / JPA.
- Exemple :

```java
public interface TicketRepository {
    Optional<Ticket> findById(TicketId id);
    Ticket save(Ticket ticket);
}
```

### 4.3. Infrastructure (implémentations techniques)

- Localisation : `*.infra.persistence`, `*.infra.messaging`, `*.infra.billing`, etc.
- Concrètement :
  - Entités JPA : `TicketJpaEntity`, `TenantJpaEntity`…
  - Repositories Spring Data : `TicketJpaRepository extends JpaRepository<...>`.
  - Adaptateurs d’API externe : `KeycloakUserClient`, `BillingHttpClient`, etc.

### 4.4. Web (API HTTP)

- Localisation : `*.web`.
- Contenu :
  - `@RestController`, `@Controller`.
  - DTO HTTP (`TicketResponse`, `CreateTicketRequest`, etc.).
  - Mappers HTTP (`TicketHttpMapper`).

Les contrôleurs **ne dépendent pas** directement de `repository` ni d’entités JPA : ils parlent uniquement en termes de use cases + DTO HTTP.

---

## 5. Stratégie de migration progressive

### 5.1. Ordre recommandé

1. **Stabiliser `common`** :

   - Créer / documenter `common.error`, `common.web`, `common.context`, `common.security`, `common.config`.
   - Déplacer les filtres globaux, exceptions transverses, résolveurs globaux.

2. **Introduire les packages de domaines** (sans déplacer le code tout de suite) :

   - `tenant.domain.{model,ports,usecase}`, `tenant.infra.persistence`, `tenant.web`.
   - même chose pour `user`, `ticket`, `draw`.

3. **Choisir un service pilote** simple à transformer en use case\*\* (par ex. `DashboardService`).

4. **Pour chaque domaine** : appliquer la même séquence :
   - Créer les interfaces de ports (`*.domain.ports`).
   - Créer les modèles métier (`*.domain.model`) à partir de `model.*`.
   - Créer les use cases (`*.domain.usecase`) à partir de `services.*`.
   - Créer les impls infra (`*.infra.persistence`, etc.) qui implémentent les ports.
   - Adapter les contrôleurs existants pour appeler les use cases.
   - Enfin, déplacer DTO / mappers dans `*.web` et supprimer progressivement les packages techniques historiques (`services`, `model`, `repository`, `dto`, `mapper`).

### 5.2. Bridges et @Deprecated pendant la migration

Pour ne pas casser le build ni tout refactorer d’un coup :

- On peut garder des classes “pont” dans les anciens packages (ex: `services.SubscriptionService`) qui délèguent aux nouveaux use cases, annotées `@Deprecated`.
- Idem pour certains DTO ou mappers.

À la fin de la migration, ces classes seront supprimées quand tous les appels auront été migrés vers les nouveaux packages.

---

## 6. Exemples de squelette (ticket & tenant)

### 6.1. Ticket – squelette minimal

```java
// ticket.domain.model
public record TicketId(String value) {}

public enum TicketStatus {
    PENDING, VALIDATED, CANCELED
}

public class Ticket {
    private final TicketId id;
    private final TicketStatus status;
    // TODO: autres champs (joueur, combinaisons, montants...)

    public Ticket(TicketId id, TicketStatus status) {
        this.id = id;
        this.status = status;
    }

    public TicketId id() { return id; }
    public TicketStatus status() { return status; }
}
```

```java
// ticket.domain.ports
public interface TicketRepository {
    Optional<Ticket> findById(TicketId id);
    Ticket save(Ticket ticket);
}

public interface TicketSearchPort {
    List<Ticket> searchTicketsByTenant(String tenantId);
}
```

```java
// ticket.domain.usecase
public interface CreateTicketUseCase {
    Ticket createTicket(/* TODO: paramètres métier (tenantId, payload, ...) */);
}

public interface VerifyTicketUseCase {
    boolean verifyTicket(TicketId id);
}
```

### 6.2. Tenant – squelette minimal

```java
// tenant.domain.model
public record TenantId(String value) {}

public class Tenant {
    private final TenantId id;
    // TODO: autres champs (nom, statut, limites, thème actif...)

    public Tenant(TenantId id) {
        this.id = id;
    }

    public TenantId id() { return id; }
}

public class AutonomyPolicy {
    // TODO: définir les règles d’autonomie du tenant (changement de plan seul, limites, ...)
}
```

```java
// tenant.domain.usecase
public interface ConfigureTenantThemeUseCase {
    void configureTheme(TenantId tenantId /*, autres paramètres métier (themeId, palette...) */);
}
```

Ces classes sont volontairement incomplètes : elles servent de squelette pour guider la migration.

---

## 7. Règle d’or "common vs domaine"

- Si un élément n’est utilisé que dans un seul domaine (par ex. `TicketStatus`), il doit **rester dans ce domaine** (`ticket.domain.model`).
- `common` ne doit contenir que ce qui est **vraiment partagé** (erreurs bas niveau, contexte, sécurité, types génériques…).

# Use cases, ports & adapters dans Tchalanet

Ce document explique les concepts clés de l’architecture applicative Tchalanet et comment migrer progressivement depuis l’ancien modèle `controller → service → repository` vers le modèle **domain/usecase/port/adapter**.

---

## 1. Use Case

### 1.1 Définition

Un **use case** représente une **action métier concrète** que l’application peut réaliser.

- 1 use case = 1 classe dans `*.domain.usecase`.
- Il orchestre :
  - le modèle métier (`*.domain.model`)
  - les ports (`*.domain.ports`)
  - éventuellement plusieurs domaines
- Il ne connaît rien de HTTP, de JPA, ni de la façon dont les données sont stockées.

Exemples de use cases pour Tchalanet :

- `CreateTicketUseCase`
- `VerifyTicketUseCase`
- `ConfigureTenantThemeUseCase`
- `UpdateUserPreferenceUseCase`
- `ScheduleDrawUseCase`

### 1.2 Exemple de squelette

```java
// ticket.domain.usecase
@UseCase
public class CreateTicketUseCase {

    private final TicketRepository ticketRepository;
    private final TenantContext tenantContext;

    public CreateTicketUseCase(TicketRepository ticketRepository,
                               TenantContext tenantContext) {
        this.ticketRepository = ticketRepository;
        this.tenantContext = tenantContext;
    }

    public Ticket handle(CreateTicketCommand command) {
        // TODO: logique métier (autonomie, limites, tirage, etc.)
        // 1. valider la requête
        // 2. construire le Ticket (domain.model)
        // 3. persister via TicketRepository (port)
        // 4. retourner le Ticket créé
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

Ce document doit rester vivant : mets-le à jour au fur et à mesure des décisions d’architecture.

```
