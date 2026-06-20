# Authentication Flow — Tchalanet Server

> **Status**: NORMATIVE  
> **Scope**: `tchalanet-server` — authentification, identity bootstrap, request context, RLS  
> **Audience**: backend developers, reviewers, agents IA, web/mobile/POS integrators  
> **Last reviewed**: 2026-06-20

---

## 1. Vue d'ensemble

Tchalanet utilise **Firebase** comme fournisseur d'identité unique pour tous les acteurs.  
Il n'y a pas de Keycloak ni d'autre IdP en production.

Deux types d'acteurs coexistent dans le même pipeline :

| Acteur | Firebase UID mappé vers | Authority Spring | Accès |
|---|---|---|---|
| `APP_USER` | `app_user.firebase_uid` | `ACTOR_APP_USER` | Web (portail admin), Mobile (gestionnaire) |
| `SELLER_TERMINAL` | `seller_terminal.firebase_uid` | `ACTOR_SELLER_TERMINAL` | POS mobile (Flutter) |

---

## 2. Pipeline global

```text
Client (web / mobile POS)
  ↓ Authorization: Bearer <firebase-id-token>
Spring Security Resource Server
  ↓ JwtDecoder (Firebase public keys)
  ↓ IdentityProviderApi.mapVerifiedToken()    → résout le type d'acteur
TchAccessContextPipelineFilter
  ↓ BootstrappedActor → ACTOR_*, ROLE_*, PERM_* authorities
TchContextFilter
  ↓ construit TchRequestContext
Controller / CommandBus / QueryBus
  ↓ business logic
RlsAwareDataSource
  ↓ applique variables PostgreSQL de session
PostgreSQL RLS
```

---

## 3. Path APP_USER (portail web / mobile gestionnaire)

### 3.1 Authentification

1. L'utilisateur s'authentifie via Firebase (email/password, Google, etc.).
2. Firebase retourne un `id_token` (JWT signé Firebase).
3. Le client envoie `Authorization: Bearer <id_token>`.

### 3.2 Bootstrap côté serveur

`TchAccessContextPipelineFilter` :
1. Vérifie la signature JWT Firebase.
2. Extrait `firebase_uid` (= `sub`).
3. Cherche `app_user` par `firebase_uid` — crée si absent (premier login).
4. Valide le statut `app_user` (`ACTIVE`, sinon 403).
5. Charge roles et permissions depuis `platform.accesscontrol`.
6. Publie authorities : `ACTOR_APP_USER`, `ROLE_<code>`, `PERM_<key>`.

### 3.3 TchRequestContext résultant

```
actorType       = APP_USER
appUserId       = <UUID app_user>
sellerTerminalId = null
roleCodes       = {"TENANT_ADMIN", ...}
permissionKeys  = {"seller_terminal.manage", ...}
```

### 3.4 Endpoints accessibles

```text
/api/v1/admin/**       → tenant admin (scope ADMIN)
/api/v1/platform/**    → super admin seulement
/api/v1/public/**      → anonyme + authentifié
```

---

## 4. Path SELLER_TERMINAL (POS mobile Flutter)

### 4.1 Authentification

1. Le SellerTerminal est provisionné par un admin tenant avec un PIN initial.
2. `SellerTerminalIdentityProvisioningApi` crée un utilisateur Firebase avec email fictif et PIN comme password.
3. Le SellerTerminal se connecte via Firebase avec `terminalCode@tenant.tchalanet` + PIN.
4. Firebase retourne un `id_token`.
5. Le POS mobile envoie `Authorization: Bearer <id_token>`.

### 4.2 Premier login — changement de PIN obligatoire

Quand `mustChangePin = true` (après provisioning ou reset admin) :
- `GET /tenant/seller-terminal/me` expose `mustChangePin: true`
- `GET /tenant/cashier/home` retourne `requiredStep: MUST_CHANGE_PIN`
- Les actions de vente sont bloquées jusqu'à `POST /tenant/seller-terminal/me/change-pin`

### 4.3 Bootstrap côté serveur

`TchAccessContextPipelineFilter` :
1. Vérifie la signature JWT Firebase.
2. Extrait `firebase_uid` (= `sub`).
3. Cherche `seller_terminal` par `firebase_uid`.
4. Valide statut (`ACTIVE`, sinon 403 ; `BLOCKED` → 403 ; `DISABLED` → 403).
5. Charge permissions du SellerTerminal.
6. Publie authorities : `ACTOR_SELLER_TERMINAL`, `PERM_<key>`.

### 4.4 TchRequestContext résultant

```
actorType        = SELLER_TERMINAL
sellerTerminalId = <UUID seller_terminal>
appUserId        = null
roleCodes        = {}     (les SellerTerminals n'ont pas de rôles)
permissionKeys   = {"ticket.sell", ...}
```

### 4.5 Endpoints accessibles

```text
/api/v1/tenant/seller-terminal/**   → profil, change-pin
/api/v1/tenant/cashier/**           → home POS, sell, payout
/api/v1/public/**                   → vérification ticket
```

L'endpoint `/api/v1/admin/**` est interdit aux SellerTerminals.

---

## 5. SecurityConfig — responsabilités

`SecurityConfig` est responsable de **l'authentification uniquement** :
- désactiver sessions serveur
- valider JWT Firebase (signature, issuer, audience)
- déléguer la conversion à `IdentityProviderApi`
- laisser `TchAccessContextPipelineFilter` construire les authorities

Ne pas placer de logique métier dans `SecurityConfig`.

### 5.1 Autorisation coarse-grained

```java
.requestMatchers("/api/v1/public/**").permitAll()
.requestMatchers("/api/v1/tenant/**").authenticated()
.requestMatchers("/api/v1/admin/**").authenticated()
.requestMatchers("/api/v1/platform/**").hasAuthority("ACTOR_APP_USER")
```

Les permissions fines appartiennent à `core.accesscontrol` via method security.

---

## 6. ApiScope

`ApiScope` est dérivé du chemin de la requête uniquement :

```text
PUBLIC    → /api/v1/public/**
TENANT    → /api/v1/tenant/**
ADMIN     → /api/v1/admin/**       (tenant-scoped admin)
PLATFORM  → /api/v1/platform/**
SDR       → /api/v1/_sdr/**
```

`ApiScope` n'est pas un rôle utilisateur.

---

## 7. TchRequestContext

Champs clés :

| Champ | APP_USER | SELLER_TERMINAL |
|---|---|---|
| `actorType` | `APP_USER` | `SELLER_TERMINAL` |
| `appUserId` | UUID | null |
| `sellerTerminalId` | null | UUID |
| `roleCodes` | {"TENANT_ADMIN", ...} | {} |
| `permissionKeys` | {"seller_terminal.manage", ...} | {"ticket.sell", ...} |
| `externalSubject` | firebase_uid | firebase_uid |
| `tenantId` | depuis JWT claim | depuis seller_terminal.tenant_id |

---

## 8. Tenant resolution

Le tenant ne vient **jamais** du payload client :

| Acteur | Source du tenant |
|---|---|
| `APP_USER` | JWT claim `tenant_code` |
| `SELLER_TERMINAL` | `seller_terminal.tenant_id` (DB lookup) |
| `SUPER_ADMIN` | Header `X-Tenant-Id` (override explicite) |

---

## 9. Super Admin et overrides sensibles

| Header | Signification | Autorisé pour |
|---|---|---|
| `X-Tenant-Id` | override tenant effectif | `SUPER_ADMIN` uniquement |
| `X-Deleted-Visibility` | visibilité soft-deleted | `SUPER_ADMIN` uniquement |

Un non-super-admin qui envoie ces headers reçoit `403`.

---

## 10. RLS integration

`RlsAwareDataSource` applique avant toute instruction SQL :

| Variable | Source |
|---|---|
| `app.current_tenant` | `ctx.tenantIdSafe()` |
| `app.deleted_visibility` | `ctx.deletedVisibilitySafe()` |
| `app.api_scope` | `ctx.apiScope()` |
| `app.is_super_admin` | `ctx.isSuperAdmin()` |

---

## 11. Contrat client HTTP

### Client standard (web/mobile gestionnaire + POS)

```http
Authorization: Bearer <firebase-id-token>
X-Request-ID: <uuid>              optional
Idempotency-Key: <uuid>           requis pour commandes critiques
```

Ne pas envoyer :
```http
X-Tenant-Id
X-Deleted-Visibility
```

### Client Platform (SUPER_ADMIN uniquement)

```http
X-Tenant-Id: <tenantCode|tenantUuid>
X-Deleted-Visibility: active|deleted|all
```

---

## 12. Règles absolues

- Les controllers ne parsent pas le JWT.
- Les controllers ne résolvent pas le tenant UUID.
- Les handlers n'utilisent pas `SecurityContextHolder`.
- Le payload client n'est jamais la source de vérité du tenant.
- Le PIN d'un SellerTerminal n'est jamais loggué ni stocké en clair en DB.
- RLS est obligatoire pour toutes les tables tenant-scoped.
