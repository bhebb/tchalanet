# Authorization & Permissions (Server)

## Status

**NORMATIVE**

---

## Objectif

Définir **où** vit la décision d'autorisation, **comment** l'exprimer dans le web layer, et **comment** l'évaluer de manière uniforme (tenant-safe, RLS-safe, testable).

---

## 1) Principe (non négociable)

- La **décision** d'autorisation appartient au domaine `core.accesscontrol`.
- La couche web **exprime** un besoin (requirement), elle ne "décide" pas.

**Conséquences** :

- ❌ pas de `if (role == ...)` ou `if (hasPerm...)` codé à la main dans les controllers
- ✅ controllers = mapping/validation + annotations de sécurité + bus

---

## 2) Méthode de sécurité Spring (MUST)

Choisir une seule voie "canonique" :

### Option A — Spring Method Security

- `@PreAuthorize(...)` + `hasPermission(...)` (recommandé)
- ou une meta-annotation `@RequiredPermission("perm.key")`

**Règles** :

- **Controllers MUST** déclarer leurs requirements via annotations.
- **Aucune** logique d'autorisation manuelle dans les controllers.

---

## 3) Permission evaluation (source unique)

### 3.1 `TchPermissionEvaluator` (single adapter)

`TchPermissionEvaluator` est l'unique adaptateur côté Spring Security.

Il **DOIT** :

- extraire le principal (via `Authentication`) **ou** via `TchRequestContext`
- normaliser la clé permission
- appeler `CheckUserPermissionsHandler` avec des wrappers :
  - `(TenantId, UserId, List<PermissionKey>)`
- retourner `false` si deny (pas d'exception "métier" exposée au web)

Il **NE DOIT PAS** :

- appeler directement des repositories JPA
- bypasser le bus
- inventer du caching sans `CacheSpecProvider`

---

## 4) Tenant resolution (rules)

- Le tenant effectif vient de `TchRequestContext` (support override SUPER_ADMIN).
- Le client **ne fournit jamais** un tenant "source de vérité".
- Dans le security layer / evaluator :
  - utiliser `TenantId.nullableOf(ctx.tenantUuid())`
  - utiliser `UserId.nullableOf(ctx.appUserId())` (ou l'identifiant canonique de ton modèle)

Si le tenant est requis et absent :

- la requête doit être bloquée au niveau filtre/contexte (403) **avant** le handler.

---

## 5) Patterns recommandés (exemples)

### 5.1 Controller (tenant scope)

```java
@RestController
@RequestMapping("/tenant/tickets")
public class TicketController {

  @PreAuthorize("hasPermission('ticket.read')")
  @GetMapping
  public TicketListDto list(/* ... */) {
    // mapping + QueryBus
  }
}
```

### 5.2 Admin scope

```java
@RestController
@RequestMapping("/admin/outlets")
public class OutletAdminController {

  @PreAuthorize("hasPermission('outlet.admin')")
  @PostMapping
  public OutletDto create(/* ... */) {
    // mapping + CommandBus
  }
}
```

---

## 6) Checklist PR (security)

- [ ] controller exprime les requirements via annotations
- [ ] aucune décision d'autorisation "if/else" dans le controller
- [ ] evaluator appelle `CheckUserPermissionsHandler` (wrappers)
- [ ] tenant/user proviennent du `TchRequestContext` (pas du client)
- [ ] deny = `false` (pas d'exception technique leak)
