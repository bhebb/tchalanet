# `com.tchalanet.server.common`

Ce module contient les **éléments réellement transverses** au backend Tchalanet.

> Règle d’or : si une classe n’est utilisée que dans un seul domaine (`tenant`, `ticket`, `user`, `draw`, …),  
> elle doit rester dans ce domaine, **pas** dans `common`.

---

## Structure

- `common.domain`

  - `model` : types métiers génériques (ex: `TenantId`, `UserId`, `Money`) utilisés par plusieurs domaines.
  - `ports` : ports transverses (ex: horloge système, bus d’événements, générateur d’UUID).
  - `usecase` : use cases transverses (rares : ex. tâches techniques globales).

- `common.error`

  - Exceptions génériques (ex: `DomainException`, `NotFoundException`, `ValidationException`).
  - Mapping global vers les réponses HTTP (ex: base pour les `ProblemDetails`).

- `common.web`

  - Composants web globaux : `GlobalExceptionHandler`, résolveurs MVC génériques, wrappers de réponses (`ApiResponse`).
  - Ne contient pas d’API métier : celles vivent dans `<domaine>.web`.

- `common.config`

  - Configuration Spring globale (CORS, Jackson, OpenAPI, config Actuator, etc.).
  - `@ConfigurationProperties` partagés (ex: `AppProperties`).

- `common.security`

  - Configuration de sécurité globale (Spring Security, Keycloak, filtres JWT).
  - Filtres HTTP transverses : ex. `TenantContextFilter`, `UserContextFilter`, RLS DB, etc.

- `common.context`
  - Représentation du contexte actuel : `TenantContext`, `UserContext`, utilitaires pour les récupérer.
  - Ne dépend pas des domaines (`ticket`, `tenant`, …).

---

## Ce qu’on met **dans** `common`

- Types métiers **vraiment transverses** (identifiants, valeurs génériques).
- Infrastructure transversale (sécurité, contextes, config globale).
- Gestion des erreurs et mappage HTTP globaux.
- Ports techniques utilisés par plusieurs domaines (ex: `ClockPort`, `EventPublisherPort`).

## Ce qu’on ne met **pas** dans `common`

- Les entités et value objects spécifiques à un domaine (`Ticket`, `Draw`, `Theme`, `Subscription`, …).
- Les repositories métiers (`TicketRepository`, `TenantRepository`, …).
- Les use cases métier (`CreateTicketUseCase`, `ConfigureTenantThemeUseCase`, …).
- Les contrôleurs REST liés à un domaine.

Si un type commence à être utilisé dans 2 domaines ou plus, on peut alors **le déplacer** de `<domaine>.domain.model` vers `common.domain.model` au moment où le besoin apparaît.
