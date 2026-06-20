# Request Context — Contexte universel d'exécution

> **Statut** : NORMATIVE  
> **Scope** : `tchalanet-server` — `common.context`, `common.context.web`, `common.context.tenant`, `common.context.system`  
> **Remplace** : `docs/conventions/api/request_context_usage.md` · `docs/conventions/user-contexte-operational.md` (sections pipeline et tenant)  
> **Dernière mise à jour** : 2026-05-30

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

## Pipeline HTTP

```
BearerTokenAuthenticationFilter
  -> IdentityProviderApi (maps already verified token to ExternalAuthenticatedUser)
  -> UserBootstrapFilter
  -> TchContextFilter
       -> ApiScopeResolver
       -> TenantContextResolver
       -> ActorContextResolver
       -> ActorAuthorizationContextResolver ← remplace les hints token par rôles/permissions DB
       -> OperationalContextResolver      ← attache l'operational context si présent (ne valide pas)
       -> TchContextBinder.bind(finalCtx) ← bind request attribute, ThreadLocal, MDC, RLS
```

### Responsabilités

**`BearerTokenAuthenticationFilter`**
- Authentification Spring / provider configuré
- Ne construit pas le contexte Tchalanet complet

**`UserBootstrapFilter`**
- Consomme `ExternalAuthenticatedUser` et résout l'utilisateur applicatif
- Enrichit : `appUserId`, `actorUserId`, statut user
- Ne décide pas : tenant effectif, scope API, override tenant, operational context

**`ActorAuthorizationContextResolver`**
- Charge les rôles et permissions effectives depuis les tables Tchalanet
- Remplace les rôles/permissions hints du token avant l'exécution des handlers
- Refuse implicitement toute élévation non confirmée; un override `SUPER_ADMIN` non confirmé est
  rejeté explicitement par `TchContextFilter`

**`TchContextFilter`**
- Point central de construction du `TchRequestContext` HTTP
- Résout scope, tenant effectif, acteur
- Attache l'operational request context si présent en headers
- Bind en request attribute, ThreadLocal, MDC, bridge RLS/datasource
- Nettoie le ThreadLocal et MDC en `finally`

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

Résout : `actorUserId`, `appUserId`, rôles/authorities, `isSystem`, `isSuperAdmin`.

Distingue :
- `actor` = qui exécute maintenant (`APP_USER` ou `SELLER_TERMINAL` ou `SYSTEM`)
- `sellerTerminalId` = l'acteur terrain original (préservé pour offline replay et audit)

```
Offline replay :
  actorType      = SYSTEM
  sellerTerminalId = SellerTerminal original (préservé pour audit métier)
```

### `OperationalContextResolver`

Pour un acteur `SELLER_TERMINAL` : l'Operational Context est intrinsèque — `sellerTerminalId` est résolu
depuis le contexte d'authentification Firebase. Pas de headers séparés requis.

Pour un acteur `APP_USER` en mode Admin POS : l'Operational Context vient de la sélection explicite.

**Ne fait pas la validation métier lourde à ce stade.**  
Règle : *Operational request context is attached early. Operational context is validated late, per action.*

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
    commandBus.execute(new RunTenantReconciliationCommand(businessDate));
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

    var op = ctx.trustedOperationalContextRequired();
    var command = new SellTicketCommand(
        ctx.effectiveTenantIdRequired(),
        ctx.actorUserIdRequired(),
        op.terminalId(),
        op.outletId(),
        op.salesSessionId(),
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
