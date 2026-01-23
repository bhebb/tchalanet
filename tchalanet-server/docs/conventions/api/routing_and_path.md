# Tchalanet — Routing & API Paths (v1)

## Status

**NORMATIVE**

---

## Objectif du document

Ce document définit **uniquement** :

1. la structure des **paths HTTP**,
2. la notion de **scope API**,
3. la règle de **routing path → scope**,
4. où et comment **déclarer un nouvel endpoint**.

Ce document **NE COUVRE PAS** :

- le contenu du `TchRequestContext` → voir `context.md`
- la sécurité Spring (roles, annotations) → voir `security_permissions.md`
- le fonctionnement interne du RLS → voir `rls.md`
- le format des réponses HTTP → voir `api_response.md`
- la pagination → voir `pagination.md`

Chaque sujet a **un seul document responsable**.

---

## 0) Préfixe API global (IMPORTANT)

Tous les endpoints HTTP du serveur sont servis sous un préfixe **versionné**, injecté automatiquement par Spring MVC.

```yaml
spring:
  mvc:
    servlet:
      path: ${app.base-path}/${app.api-version} # ex: /api/v1
```

### Conséquences (règle non négociable)

- Les controllers **NE DOIVENT JAMAIS** contenir `/api/v1` dans leurs annotations.
- Les paths déclarés dans les controllers sont des **paths logiques**.
- Le préfixe `/api/v1` est ajouté automatiquement au runtime.

### Exemple canonique

```java
@RestController
@RequestMapping("/tenant/tickets")
class TicketController {
  // ...
}
```

Exposé automatiquement sous :

```
/api/v1/tenant/tickets
```

### ❌ Interdit :

```java
@RequestMapping("/api/v1/tenant/tickets")
```

---

## 1) Scopes API (classification canonique)

Chaque endpoint appartient exactement à un scope.

| Scope        | Path logique   | Usage                       |
| ------------ | -------------- | --------------------------- |
| **PUBLIC**   | `/public/**`   | APIs publiques              |
| **PLATFORM** | `/platform/**` | APIs globales plateforme    |
| **ADMIN**    | `/admin/**`    | Administration d'un tenant  |
| **TENANT**   | `/tenant/**`   | APIs métier opérationnelles |
| **SDR**      | `/_sdr/**`     | Spring Data REST interne    |

Au runtime, les paths exposés sont :

- `/api/v1/public/**`
- `/api/v1/platform/**`
- `/api/v1/admin/**`
- `/api/v1/tenant/**`
- `/api/v1/_sdr/**`

---

## 2) Signification des scopes

### PUBLIC

- Auth non obligatoire (ou optionnelle).
- Peut utiliser un default tenant (décision v1).
- **Exemples** : home publique, vérification de ticket publique.

### PLATFORM

- APIs globales plateforme.
- En général réservées au `SUPER_ADMIN`.
- Tenant optionnel ou absent.

### ADMIN

- Back-office d'un tenant.
- Auth obligatoire.
- Tenant requis.
- **Rôles typiques** : `TENANT_ADMIN`, `SUPER_ADMIN`.

### TENANT

- APIs métier opérationnelles.
- Auth obligatoire.
- Tenant requis.
- **Rôles métiers** (cashier, operator, manager…).

### SDR

- Endpoints générés par Spring Data REST.
- Toujours internes.
- Toujours restreints (ex : `SUPER_ADMIN`).
- Jamais utilisés pour des APIs métier critiques.

---

## 3) Spring Data REST (SDR)

SDR est explicitement isolé sous un préfixe interne unique.

```yaml
spring:
  data:
    rest:
      enabled: true
      base-path: /_sdr
      detection-strategy: annotated
```

### Règles

- Tous les endpoints SDR sont exposés sous :

  ```
  /api/v1/_sdr/**
  ```

- Seuls les repositories annotés `@RepositoryRestResource` sont exposés.

- Le path d'un repository SDR doit être un seul segment.

  - ✅ `path = "outlets"`
  - ❌ `path = "platform/outlets"`

- Les APIs métier ne doivent jamais être exposées via SDR.

---

## 4) Où déclarer un nouvel endpoint

### 4.1 Endpoints custom (cas normal)

- Créer un controller REST classique.
- Choisir un seul scope.
- Utiliser un path logique sans `/api/v1`.

**Exemple** :

```java
@RestController
@RequestMapping("/admin/outlets")
class OutletAdminController {
  // ...
}
```

Exposé sous :

```
/api/v1/admin/outlets
```

### 4.2 Endpoints SDR (cas exceptionnel)

```java
@RepositoryRestResource(path = "outlets", exported = true)
interface OutletRepository extends JpaRepository<OutletEntity, UUID> {}
```

Exposé sous :

```
/api/v1/_sdr/outlets
```

---

## 5) Résolution du scope (routing interne)

Le scope est déterminé uniquement à partir du path HTTP.

### Règles générales :

- `/api/v1/public/**` → `PUBLIC`
- `/api/v1/platform/**` → `PLATFORM`
- `/api/v1/admin/**` → `ADMIN`
- `/api/v1/tenant/**` → `TENANT`
- `/api/v1/_sdr/**` → `PLATFORM` (interne)

Le scope est ensuite utilisé par :

- la construction du contexte,
- la résolution du tenant,
- la sécurité,
- l'activation du RLS.

(Détails dans `context.md` et `rls.md`.)

---

## 6) Swagger / OpenAPI groups

Les groupes OpenAPI sont définis par scope.

### Patterns recommandés (sans `/api/v1`) :

- **public** → `/public/**`
- **platform** → `/platform/**`
- **admin** → `/admin/**`
- **tenant** → `/tenant/**`
- **sdr** → `/_sdr/**`

**SDR doit toujours être dans un groupe séparé.**

---

## 7) Règles non négociables

- Tout endpoint appartient à un seul scope.
- Aucun endpoint métier critique sous `/_sdr`.
- Aucun controller ne déclare `/api/v1`.
- Aucun nouveau scope sans mise à jour de ce document.

---

## 8) Checklist avant PR

- [ ] Path logique (`/public|platform|admin|tenant|_sdr`)
- [ ] Scope unique et clair
- [ ] Aucun `/api/v1` codé en dur
- [ ] SDR utilisé uniquement pour référentiels simples
- [ ] Groupe Swagger correct

---

## Documents liés

- `context.md` — Contexte & résolution du tenant
- `security_permissions.md` — Sécurité & rôles
- `rls.md` — Row Level Security
- `web_api.md` — Controllers & DTOs
- `api_response.md` — Enveloppe HTTP
- `pagination.md` — Pagination canonique

---

## Résumé

- **Routing** = paths + scopes
- **Versioning** via configuration, pas via annotations
- Zéro logique métier
- Zéro sécurité détaillée
- Zéro contexte/RLS interne
- **Un seul rôle** : définir où vit une API et comment on y accède.
