# Tchalanet — Backend Web API Conventions (Server)

## Status

**NORMATIVE**

---

## Objectif du document

Ce document définit **uniquement** les conventions pour les **APIs REST custom** (controllers MVC) :

1. responsabilités et style des controllers,
2. règles DTO (request/response),
3. usage des Typed IDs dans le Web layer,
4. dispatch CQRS via CommandBus / QueryBus,
5. pagination (usage, sans redéfinir le standard),
6. réponses 2xx vs erreurs 4xx/5xx (sans redéfinir les formats),
7. **audit logs** : où et comment les déclencher (sans redéfinir l’implémentation).

Ce document **NE COUVRE PAS** :

- structure des paths & scopes → voir `routing_and_path.md`
- format détaillé `ApiResponse<T>` → voir `api_response.md`
- pagination détaillée → voir `pagination.md`
- contexte & tenant resolution → voir `context.md`
- RLS → voir `rls.md`
- règles de permissions/roles → voir `security_permissions.md`
- conventions handlers/commands/queries → voir `command_query_handlers.md`
- détails audit (format, storage, Envers, policies) → voir `audit.md`

---

## 0) Scope

S’applique à :

- `features/*/infra/web/**`
- `core/*/infra/web/**`
- `catalog/*/infra/web/**`

Ne s’applique pas à :

- Spring Data REST (`/_sdr/**`) → voir `routing_and_path.md`

---

## 1) Règles de routing (rappel minimal)

- Les controllers déclarent des **paths logiques** : `/public/**`, `/platform/**`, `/admin/**`, `/tenant/**`
- **Ne jamais** inclure `/api/v1` dans `@RequestMapping` (injecté via `spring.mvc.servlet.path`)

Voir : `routing_and_path.md`

---

## 2) Controllers — responsabilités (MUST)

### 2.1 Ce qu’un controller fait

Un controller fait uniquement :

- validation d’input (Bean Validation)
- mapping request → command/query
- injection du contexte (`@CurrentContext TchRequestContext`)
- dispatch via `CommandBus` / `QueryBus`
- mapping view → DTO response
- **déclaration des audit logs (si action auditée)**
- retour HTTP (2xx / errors)

### 2.2 Ce qu’un controller ne fait pas

Interdit :

- logique métier
- calculs financiers
- transitions d’état
- appels directs à des repositories/adapters
- “manual auth” métier (if role then…) dans le controller
- écrire soi-même dans une table d’audit depuis le controller

---

## 3) Audit logs (MUST)

### 3.1 Principe

Les actions sensibles doivent produire un **audit log fonctionnel** (qui a fait quoi, sur quoi, quand, dans quel tenant, avec quels paramètres utiles).

Le controller (ou le handler) **déclare** l’intention d’audit ; l’infrastructure d’audit **écrit** l’événement.

### 3.2 Où déclencher l’audit

Règle canonique :

- **Audit orienté “action utilisateur” (HTTP)** : déclenché au niveau controller via annotation.
- **Audit orienté “invariant métier” (use case)** : déclenché au niveau handler si l’action peut être appelée autrement que via HTTP (batch, event replay).

On évite de doubler : un use case ne doit pas produire 2 logs identiques (controller + handler). Si l’API est l’unique entrypoint, controller est suffisant.

### 3.3 Comment déclarer l’audit (convention)

Utiliser l’annotation projet `@AuditLog(...)` (ou équivalent existant) sur la méthode controller/handler.

Champs attendus (exemples, à adapter à ton implémentation) :

- `action` (ex: `OUTLET_CREATE`, `TICKET_VOID`, `PAYOUT_APPROVE`)
- `entity` (ex: `outlet`, `ticket`, `payout`)
- `idExpression` (SpEL) pour extraire l’ID (wrapper ou string)
- `detailsExpression` (SpEL) pour champs utiles (montant, reason, channel…)

Les détails d’implémentation, storage, Envers, et politique d’audit sont dans `audit.md`.

### 3.4 Ce qui doit être audité (minimum)

Audit obligatoire (liste minimale) :

- ventes ticket, annulation/void, remboursements
- approbations (autonomy/limits)
- payouts (request/approve/pay/reject)
- changements de config tenant (limits, commissions, outlets, users/roles)
- overrides SUPER_ADMIN (tenant override, bypass RLS, operations platform)

---

## 4) Dispatch CQRS (MUST)

- Tous les use cases passent par **CommandBus / QueryBus**
- Pas d'appel direct à un service si un handler existe déjà

Voir : `command_query_handlers.md`

---

## 5) DTO Web (MUST)

### 5.1 DTOs ne leakent jamais les entités

- Les DTOs web ne doivent pas exposer des JPA entities ou types persistence.
- On expose des “views” stables (records) et on mappe via MapStruct.

### 5.2 Naming

- Request DTO : `XxxRequest`
- Response DTO : `XxxResponse`
- Query params complexes : `XxxFilter` (optionnel)

### 5.3 Mapping

- MapStruct recommandé pour mapping stable DTO <-> views
- Les conversions “web string → typed id” se font via converters dans `common.web.converter`

---

## 6) IDs dans la couche web (MUST)

### 6.1 Typed IDs partout

- `@PathVariable TicketId id`
- `@RequestParam OutletId outletId`

Interdit :

- `UUID` brut dans signatures controller (hors cas fichiers/streams)
- Parse manuel (`UUID.fromString`) dans controllers

Voir : `typed_ids.md`

### 6.2 Converters

- Les converters `String -> XxxId` vivent dans `common.web.converter`

---

## 7) Réponses HTTP (MUST)

### 7.1 2xx JSON : ApiResponse<T>

- Contrat : tout endpoint JSON “success” renvoie `ApiResponse<T>`
- Migration : legacy DTO brut auto-wrap via `ApiResponseBodyAdvice`
- Nouveau code : retourner `ApiResponse<T>` explicitement (target convention)

Voir : `api_response.md`

### 7.2 4xx/5xx : ProblemDetail

- Erreurs = `ProblemDetail` (`application/problem+json`)
- Lever `ProblemRest.*(...)`
- Interdit : erreurs wrappées

Voir : `api_response.md` + `audit.md` (erreurs auditables)

---

## 8) Pagination (MUST)

- List endpoints : `ApiResponse<TchPage<XxxResponse>>`
- Controller : `@TchPaging(...) TchPageRequest pageReq`
- Handler : `TchPage<T>` (pas `Page<T>`)

Voir : `pagination.md`

---

## 9) Permissions & Security (MUST)

- `@Secured` ou `@PreAuthorize`
- Fine-grain via `TchPermissionEvaluator` / use case dédié

Voir : `security_permissions.md`

---

## 10) Endpoints “print” / fichiers (PDF, ESC-POS)

- `byte[]` / `Resource` autorisés
- Non wrappés (pas JSON)
- Headers :
  - `Cache-Control: no-store`
  - `Content-Disposition: inline; filename="..."`

---

## 11) Exemples canoniques

### 11.1 Command auditée (TENANT)

```java
@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
class TicketCommandController {

  private final CommandBus commandBus;

  @PostMapping("/{ticketId}/void")
  @AuditLog(
      action = "TICKET_VOID",
      entity = "ticket",
      idExpression = "#ticketId",
      detailsExpression = "{ 'reason': #req.reason() }"
  )
  public ApiResponse<VoidTicketResponse> voidTicket(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TicketId ticketId,
      @Valid @RequestBody VoidTicketRequest req
  ) {
    var cmd = new VoidTicketCommand(ctx.effectiveTenantIdRequired(), ticketId, req.reason());
    var out = commandBus.send(cmd);
    return ApiResponse.success(out);
  }
}
```

**Note** : l'annotation `@AuditLog` est canonique pour "audit action utilisateur".  
Les détails (format, stockage, Envers, RLS de la table d'audit) sont dans `audit.md`.

---

## 12) Checklist avant PR (Web API)

- [ ] Path logique (sans `/api/v1`) conforme à `routing_and_path.md`
- [ ] Controller thin (pas de logique métier)
- [ ] Dispatch via bus
- [ ] Typed IDs (pas de `UUID` brut)
- [ ] Actions sensibles auditées via `@AuditLog` (controller ou handler)
- [ ] 2xx JSON = `ApiResponse<T>` (legacy auto-wrap OK)
- [ ] 4xx/5xx = `ProblemDetail` via `ProblemRest`
- [ ] Pagination standard si liste
- [ ] Sécurité via annotations
- [ ] Sécurité : `@PreAuthorize` actif sur tout controller admin/ops
- [ ] Print/file endpoints non wrappés + headers

---

## 13) Règle ArchUnit — sécurité des scopes protégés

### 13.1 Principe

Tout `@RestController` dont le path `@RequestMapping` commence par `/admin/`, `/platform/`
ou `/_sdr/` **DOIT** porter `@PreAuthorize` :

- **au niveau classe** (couvre toutes les méthodes) — forme canonique recommandée, **ou**
- **sur chaque méthode handler publique** individuellement.

La règle est vérifiée automatiquement à chaque build par `SecurityArchTest`
(package `com.tchalanet.server.arch`).

### 13.2 Prefixes couverts

| Prefix         | Scope                 | Autorité minimale attendue      |
| -------------- | --------------------- | ------------------------------- |
| `/admin/**`    | Administration tenant | `SUPER_ADMIN`                   |
| `/platform/**` | Platform / ops        | `SUPER_ADMIN`                   |
| `/_sdr/**`     | Spring Data REST      | `TENANT_ADMIN` ou `SUPER_ADMIN` |

### 13.3 Whitelister un endpoint public dans ces scopes

Si un endpoint doit être public **dans** l'un de ces scopes (cas exceptionnel),
il **DOIT** porter `@PreAuthorize("permitAll()")` explicitement.
Le bypass silencieux (absence d'annotation) est interdit.

```java
// ✅ Whitelist explicite obligatoire
@GetMapping("/health-internal")
@PreAuthorize("permitAll()")
public ApiResponse<String> healthCheck() { ... }
```

### 13.4 Comportement en cas de violation

La règle `SecurityArchTest.protectedScopeControllersMustHavePreAuthorize` échoue
avec un message indiquant le controller violateur et la liste des méthodes non sécurisées.
**Ce test bloque le build CI** — aucune PR ne doit le contourner.

---

## Documents liés

- `routing_and_path.md`
- `api_response.md`
- `pagination.md`
- `typed_ids.md`
- `security_permissions.md`
- `command_query_handlers.md`
- `context.md`
- `rls.md`
- `audit.md`
