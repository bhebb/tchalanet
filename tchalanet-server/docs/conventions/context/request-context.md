# Request Context — Contexte universel d'exécution

> **Statut** : NORMATIVE  
> **Scope** : `tchalanet-server` — `common.context`, `common.context.web`, `common.context.tenant`, `common.context.system`  
> **Remplace** : `docs/conventions/api/request_context_usage.md` · `docs/conventions/user-contexte-operational.md` (sections pipeline et tenant)  
> **Dernière mise à jour** : 2026-05-30

---

## Identité vs Autorisation — séparation des responsabilités

Ces deux notions sont intentionnellement séparées dans Tchalanet.

### Identité (Firebase)

L'identity provider répond à une seule question : **est-ce que ce token est authentique et de qui ?**

Firebase retourne un `firebase_uid` et un token vérifié cryptographiquement. C'est tout. Firebase ne sait pas si l'acteur est bloqué, quel tenant il appartient à, ni quels droits il a. C'est intentionnel.

```
Firebase → firebase_uid vérifié (authentification)
                ↓
     IdentityProviderApi.mapVerifiedToken()
                ↓
        TchActorIdentity (qui appelle ?)
```

### Autorisation (Tchalanet DB)

Tchalanet résout ensuite, indépendamment du token : **que peut faire cet acteur ?**

```
TchActorIdentity (firebase_uid)
        ↓
ActorContextResolver    → résout l'entité : app_user ou seller_terminal
        ↓
ActorAuthorizationContextResolver → charge rôles + permissions depuis DB
        ↓
TchRequestContext       → source de vérité pour toute décision d'autorisation
```

**Conséquence clé** : les droits sont toujours DB-side. Un changement de rôle ou un blocage prend effet à la prochaine requête sans renouveler le token Firebase.

---

## Acteur vs Rôle — deux niveaux distincts

### `TchActorType` — qui appelle au niveau machine

```java
APP_USER         // humain connecté (admin, super-admin, operateur)
SELLER_TERMINAL  // terminal POS physique
SYSTEM           // batch, scheduler, event replay interne
```

`actorType` détermine **comment** l'autorisation est évaluée, pas **ce qu'il peut faire** directement.

### Rôle (`TchRole`) — attribut organisationnel, APP_USER uniquement

```java
SUPER_ADMIN   // admin cross-tenant, accès platform
TENANT_ADMIN  // admin d'un tenant
OPERATOR      // superviseur opérationnel interne
SYSTEM        // technique — ne correspond pas à un humain
```

Les rôles sont des étiquettes coarsées sur un `APP_USER`. Ils servent à dériver des permissions via la matrice `role_permission`.

**`SELLER_TERMINAL` n'a pas de rôle.** Son autorisation est évaluée directement depuis :
- son statut (`ACTIVE` / `BLOCKED` / `PENDING` / `DISABLED`)
- son flag `mustChangePin`
- ses permissions directes (accordées à l'entité, pas via un rôle)
- l'authority Spring `ACTOR_SELLER_TERMINAL` publiée par le pipeline

---

## SellerTerminal — identité machine, pas identité personne

C'est la distinction la plus importante du modèle.

Un `SellerTerminal` est une **identité POS physique**, pas un utilisateur humain.

```
terminal "TERM-0042" appartient au tenant "haitiloto"
  ↳ authentifié via  :  term-0042@haitiloto.tchalanet  /  PIN 6 chiffres
  ↳ lié à Firebase   :  firebase_uid = "abc123..."
  ↳ transactions      :  scoped à TERM-0042, pas à une personne
```

**Le terminal peut changer de mains.** Seller A gère TERM-0042 pendant 3 mois, Seller B prend sa place. C'est normal et attendu :
- l'identité Firebase reste celle de TERM-0042 (pas de la personne)
- le tenant sait qui possède ce terminal, pas qui l'utilise à l'instant T
- les limites, tickets, audits sont tous attachés à TERM-0042

**Le PIN est la clé de délégation opérationnelle.** Quand un terminal change de mains, l'admin reset le PIN via `POST /admin/seller-terminals/{id}/pin-reset` → `mustChangePin=true`. Le nouvel opérateur change le PIN au premier login. L'identité du terminal dans Tchalanet ne change pas.

> Ce modèle est similaire à une caisse enregistreuse physique : la caisse a une identité fiscale (numéro de série), pas l'employé qui l'opère. Ce qui compte pour le comptable, c'est la caisse 42, pas qui était derrière le comptoir.

---

## Définition

Le Request Context (`TchRequestContext`) est le contexte universel d'exécution.

Il existe pour **tous** les flows d'entrée :
- utilisateur connecté (`APP_USER` : Admin, Super-admin)
- SellerTerminal POS (`SELLER_TERMINAL`)
- public anonyme
- batch / scheduler
- startup / seed
- event replay si nécessaire

Il répond à :
- Qui appelle ?
- Quel tenant est effectif ?
- Quel scope HTTP / système ?
- Quel user / actor ?
- Quelle locale / timezone ?
- Quel requestId / correlationId ?

> **Nom canonique :** `Request Context` / `TchRequestContext`  
> Ne pas l'appeler uniquement "User Context" — il existe aussi pour `PUBLIC`, `SYSTEM`, `BATCH`, `STARTUP`, `PLATFORM`.

---

## Pipeline HTTP — séquence réelle des filtres

```
Client  ──────────────────────────────────────────────────────────────────────
        Authorization: Bearer <firebase-jwt>
        X-Tch-Client-Type: POS          ← SELLER_TERMINAL uniquement (hint, ne donne pas accès)
        [X-Tenant-Id]                   ← APP_USER seulement (interdit pour SELLER_TERMINAL)

Spring Security ───────────────────────────────────────────────────────────────
  BearerTokenAuthenticationFilter
    → JWT decode (signature Firebase vérifée)
    → SecurityConfig.convert()
        → IdentityProviderApi.mapVerifiedToken(VerifiedExternalToken)
        → ExternalAuthenticatedUser stocké dans JwtAuthenticationToken.details
        → JwtAuthenticationToken.authorities = []  (vide ici — populé plus tard)

  SensitiveIdentityVerificationFilter          (opérations sensibles uniquement : PIN change etc.)

  TchAccessContextPipelineFilter               (skippé pour /public/**, /actuator/health, /swagger-ui)
    ↓
    1. IdentityBootstrapStep.bootstrap()       [= UserBootstrapFilterImpl]
        → lit ExternalAuthenticatedUser depuis JwtAuthenticationToken.details
        → lit X-Tch-Client-Type (ExpectedActorTypeResolver)

        SI X-Tch-Client-Type == "POS"  → SELLER_TERMINAL path :
            SellerTerminalIdentityLookup.findByExternalIdentity(provider, issuer, firebase_uid)
            → absent : 403 "terminal.external_identity_not_linked"
            → !isActive() : 403 "terminal.not_active"
            → BootstrappedActor.sellerTerminal(sellerTerminalId, tenantId, ...) → request attr

        SINON                          → APP_USER path :
            ExternalIdentityAppUserResolver.resolve(externalUser)
            → absent : 403 "external_identity.not_linked"
            → status != ACTIVE : 403 "user.not_active"
            → BootstrappedActor.appUser(appUserId, ...) → request attr

        ⚠ PAS de fallback entre les deux : une requête POS sans mapping terminal
          ne tombe pas sur APP_USER — elle reçoit 403.

    2. AccessResolutionStep.resolve()          [= AccessResolutionStepImpl]
        → lit BootstrappedActor depuis request attr

        SELLER_TERMINAL :
            → rejette X-Tenant-Id et X-Tch-Tenant-Override : 403 "terminal.tenant_selection_not_allowed"
            → ResolvedAccessContext(SELLER_TERMINAL, sellerTerminalId, tenantId_from_DB,
                                    SELLER_TERMINAL_PERMISSIONS)  ← permissions hardcodées, pas DB
            → enrichit Spring auth :
                ACTOR_SELLER_TERMINAL
                PERM_seller_terminal.me.read
                PERM_seller_terminal.sell
                PERM_seller_terminal.ticket.read_own
                PERM_seller_terminal.ticket.reprint_own

        APP_USER :
            → AccessControlSnapshotResolver.resolvePlatform(userId) + resolveTenant(userId, tenantId)
            → ResolvedAccessContext(APP_USER, userId, effectiveTenantId, roleCodes, permissionKeys)
            → enrichit Spring auth :
                ACTOR_APP_USER + ROLE_TENANT_ADMIN + PERM_seller_terminal.manage …

        → RESOLVED_ACCESS stocké en request attr

  TchContextFilter                             (before AuthorizationFilter)
    → lit RESOLVED_ACCESS depuis request attr

    SI présent (authentifié) — handleResolvedAccess() :
        → vérifie X-Tch-Override-Reason si superAdmin override
        → contextFactory.createFromResolvedAccess(req, scope, resolvedAccess)
        → tenantContextResolver.hydrateResolvedTenant() ← métadonnées tenant seulement (code/tz/currency)
        → contextBinder.bind()  ← RLS actif à partir d'ici
        → OperationalContextResolver.resolve() ou OperationalContextHeaderParser.parseHint()
        → contextBinder.bind()  ← bind final avec operational context
        → chain.doFilter()

    SI absent (public/anonyme) — handlePublicOrLegacy() :
        → tenant public par défaut si allowed
        → actorContextResolver.attachBootstrappedAppUserId()  ← chemin legacy/public
        → chain.doFilter()

  AuthorizationFilter
    → Spring évalue @PreAuthorize("hasPermission(null, '...')") via les authorities enrichies
```

### Header critique — `X-Tch-Client-Type: POS`

Ce header est **requis** pour toute requête SELLER_TERMINAL. Il indique au `UserBootstrapFilterImpl` quel resolver utiliser.

```
Header                    Valeur        Effet
X-Tch-Client-Type         POS           → résoudre comme SELLER_TERMINAL
X-Tch-Client-Type         WEB           → résoudre comme APP_USER (même résultat qu'absent)
(absent)                                → résoudre comme APP_USER
```

**Ce header est un hint de sélection, pas une preuve d'identité.** Il ne donne aucun accès. L'accès est prouvé par le firebase JWT + le mapping en DB. Un client qui envoie `POS` sans avoir de mapping `seller_terminal.firebase_uid` reçoit 403.

---

### Permissions SELLER_TERMINAL — hardcodées

Les permissions d'un SELLER_TERMINAL ne sont pas chargées depuis la DB. Elles sont définies statiquement dans `AccessResolutionStepImpl` :

```java
static final Set<String> SELLER_TERMINAL_PERMISSIONS = Set.of(
    "seller_terminal.me.read",
    "seller_terminal.sell",
    "seller_terminal.ticket.read_own",
    "seller_terminal.ticket.reprint_own"
);
```

Si un endpoint requiert une permission différente pour un SellerTerminal, elle doit être ajoutée ici.

---

### Champs deprecated dans `TchRequestContext`

Ces champs sont encore présents dans le record mais ne doivent plus être utilisés :

```java
// @deprecated — use actorType() + externalSubject() instead
String keycloakUserId

// @deprecated — use roleCodes() instead
Set<TchRole> systemRoles

// @deprecated — use permissionKeys() instead
Set<String> customRoles
```

`ActorContextResolver.attachBootstrappedAppUserId()` vérifie encore `keycloakUserId()` — c'est du code legacy sur le chemin public uniquement (`handlePublicOrLegacy`). Nouveau code : toujours `actorType()`, `roleCodes()`, `permissionKeys()`.

---

## Resolvers

### `ApiScopeResolver`

Déduit le scope depuis le path HTTP :

| Path prefix | Scope |
|---|---|
| `/public/**` | `PUBLIC` |
| `/tenant/**` | `TENANT` |
| `/admin/**` | `ADMIN` |
| `/platform/**` | `PLATFORM` |
| `/_sdr/**` | `SDR` |

Ne décide pas du tenant à lui seul.

### `TenantContextResolver`

| Scope | Tenant effectif |
|---|---|
| `PUBLIC` anonymous | Tenant public par défaut (`tchalanet`) |
| `PUBLIC` authenticated | Tenant JWT si présent, sinon tenant public |
| `TENANT` | Tenant requis depuis contexte authentifié |
| `ADMIN` | Tenant requis depuis contexte authentifié |
| `PLATFORM` | Pas de tenant par défaut |
| `SDR` | Selon policy explicite |
| `SUPER_ADMIN` + override | Tenant override explicite + audit obligatoire |

**Règles critiques :**
- Le tenant ne vient **jamais** du body client comme source de vérité
- L'override super-admin doit être explicite, auditable, avec raison obligatoire

Headers override :
```
X-Tch-Tenant-Override: <tenant-code-or-id>
X-Tch-Override-Reason: Support / correction / investigation
```

### `ActorContextResolver`

Utilisé uniquement dans le chemin `handlePublicOrLegacy` (public/anonyme). Pour les requêtes authentifiées, l'acteur est résolu par `AccessResolutionStepImpl` via `BootstrappedActor`.

Dans le chemin legacy il attache `appUserId` depuis l'attribut `BOOTSTRAPPED_APP_USER_ID` posé par `UserBootstrapFilterImpl`.

### `OperationalContextResolver`

Résout le contexte opérationnel **après** que le RLS est bindé (pour permettre les lookups DB si nécessaire).

Pour `SELLER_TERMINAL` : `sellerTerminalId` est déjà dans `TchRequestContext` depuis `ResolvedAccessContext`. Pas de header séparé.

Pour `APP_USER` Admin POS : sélection explicite via `POST /cashier/operational-context/select`.  
L'`X-Tch-Operational-Source` header peut indiquer la source de l'operational context (`ADMIN_SELECTION`, `CLIENT_CLAIM`, etc.).

**Ne valide pas le contexte opérationnel à ce stade.** La validation métier est faite par action.

```
X-Tch-Operational-Source: ADMIN_SELECTION   ← APP_USER a sélectionné un SellerTerminal
X-Tch-Operational-Source: CLIENT_CLAIM      ← claim client (trust = WEAK)
```

Voir : `tchalanet-server/docs/conventions/context/operational-context.md`

---

## Tenant policy par scope

| Scope | Comportement |
|---|---|
| `PUBLIC` anonymous | Bind tenant public par défaut |
| `PUBLIC` authenticated + tenant claim | Préférer tenant JWT |
| `PUBLIC` authenticated sans claim | Fallback tenant public |
| `TENANT` | Tenant requis |
| `ADMIN` | Tenant requis ou override autorisé |
| `PLATFORM` | Pas de tenant par défaut |
| `SDR` | Selon policy explicite |
| `SUPER_ADMIN` + override | Tenant depuis override explicite + auditable |

---

## Batch / Scheduler

Un batch n'a pas de HTTP request. Il ne passe pas par `TchContextFilter`.

Il construit explicitement un contexte via `BatchTchContextBinder` :

```java
batchContextBinder.runWithTenantContext(tenantId, jobRunId, () -> {
    commandBus.execute(new RunTenantDrawSettlementCommand(businessDate));
});
```

| Type | Scope | Actor | Tenant |
|---|---|---|---|
| Batch tenant-scoped | `TENANT` ou `SYSTEM_TENANT` | `SYSTEM` | Explicite dans les paramètres du job |
| Batch platform | `PLATFORM` | `SYSTEM` | Absent sauf travail volontairement tenant-scoped |
| Scheduler tick | — | Lance batch ou commande | Bind contexte explicite — pas de logique métier dans le scheduler |

---

## Events / Async

Le ThreadLocal ne traverse pas automatiquement : async, scheduler, batch, retry, new thread, event replay.

Les events doivent porter les faits nécessaires :

```
tenantId          — si tenant-scoped
actorUserId       — si actor-sensible
sellerTerminalId  — si seller-terminal-sensible (ex. offline replay)
correlationId     — si traçabilité requise
occurredAt        — toujours pour replay
```

Le listener reconstruit ou bind le contexte nécessaire avant d'appeler une command.

---

## Accès direct — règles

### Autorisé uniquement pour

- `TchContextFilter` (binding HTTP)
- `BatchTchContextBinder` / startup binders
- RLS datasource bridge
- Audit infrastructure
- Idempotency infrastructure
- Tests

### Dans le code applicatif ordinaire

Le controller reçoit le contexte via `@CurrentContext TchRequestContext ctx`.  
Il extrait et mappe uniquement les champs nécessaires vers la command/query.

```java
@PostMapping("/tickets")
public ApiResponse<SellTicketResponse> sell(
    @CurrentContext TchRequestContext ctx,
    @Valid @RequestBody SellTicketRequest request) {

    var command = new SellTicketCommand(
        ctx.effectiveTenantIdRequired(),
        ctx.sellerTerminalIdRequired(),   // résolu depuis Firebase UID (SELLER_TERMINAL)
                                          // ou sélection explicite Admin POS (APP_USER)
        request.lines(),
        request.idempotencyKey()
    );
    return ApiResponse.success(mapper.toResponse(commandBus.execute(command)));
}
```

**Les handlers reçoivent les champs nécessaires via commands/queries — pas `TchContext.current()` directement.**

Interdit dans le code applicatif ordinaire :
- Parser le JWT
- Lire `SecurityContext` directement
- Résoudre le tenant depuis le payload de la requête
- Manipuler le MDC
- Appeler `set_config`
- Modifier `TchContext` pour faire fonctionner du code downstream

---

## RLS

Le RLS lit le contexte canonique courant via le datasource bridge.

| Context | RLS mapping |
|---|---|
| Public (tenant par défaut) | `tenant_id` + scope `PUBLIC` |
| Tenant / Admin | `tenant_id` + scope tenant/admin |
| Platform sans tenant | Pas de tenant + scope `PLATFORM` |
| Super-admin override | Tenant override + flag super-admin |
| Batch tenant | `tenant_id` + scope tenant |
| Batch platform | Pas de tenant + scope platform |

Le RLS est la dernière ligne de défense, pas de la logique de routing.

---

## PageModel — cas particulier

| Flow | Règle |
|---|---|
| Template seed (`PageModelTemplateSeedRunner`) | Startup catalog/global — pas de tenant par défaut |
| Tenant seed (`PageModelOnboardingRunner`) | Contexte tenant startup explicite pour le tenant par défaut |
| Runtime public PageModel | Utiliser le contexte HTTP déjà bindé |

Les dynamic providers sont des Spring singleton beans.  
`load(...)` s'exécute pendant la requête HTTP après résolution du document.  
→ Ne pas effacer ou remplacer le contexte HTTP avant que les providers s'exécutent.

---

## Checklist PR

- [ ] HTTP requests : `TchContextFilter` est le seul producteur de contexte HTTP
- [ ] `UserBootstrapFilter` enrichit l'acteur uniquement — tenant policy séparée
- [ ] Tenant policy par scope respecte la matrice ci-dessus
- [ ] Startup tenant work : contexte tenant startup explicite
- [ ] Temporary tenant switch : restaure le contexte précédent
- [ ] PageModel runtime providers : contexte HTTP original préservé
- [ ] Scheduler / batch / event : pas de dépendance au ThreadLocal HTTP ambient
- [ ] RLS session variables : mappées au contexte effectif
- [ ] Handlers : reçoivent les champs nécessaires via command/query, pas `TchContext.current()`
