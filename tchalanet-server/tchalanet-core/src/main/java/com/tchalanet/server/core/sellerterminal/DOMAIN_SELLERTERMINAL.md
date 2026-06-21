# Domain `core.sellerterminal`

> **Owner** : `core.sellerterminal`  
> **Status** : NORMATIVE  
> **Dernière mise à jour** : 2026-06-20

---

## Rôle

`core.sellerterminal` est le domaine de l'acteur de vente unique de Tchalanet.

Un `SellerTerminal` est :
- une identité Firebase permanente (email fictif + PIN)
- une unité de vente et de facturation
- l'acteur opérationnel POS — il est lui-même le contexte opérationnel

Ce domaine remplace les anciens concepts `Terminal`, `SalesSession`, `Seller` qui sont retirés.

---

## Statuts

| Statut | Signification |
|---|---|
| `PENDING` | Créé, identité Firebase pas encore provisionnée |
| `ACTIVE` | Actif, peut s'authentifier et vendre |
| `BLOCKED` | Bloqué temporairement par un admin (réversible) |
| `DISABLED` | Désactivé définitivement — ne peut plus être utilisé |

---

## Entité `SellerTerminal`

```java
SellerTerminal(
    SellerTerminalId id,
    TenantId tenantId,
    String terminalCode,         // code court unique dans le tenant
    String displayName,          // nom affiché (vendeur / kiosque)
    String firstName,
    String lastName,
    String phoneNumber,
    BigDecimal commissionRate,
    SellerTerminalStatus status,
    Instant activatedAt,
    Instant lastSeenAt,
    boolean mustChangePin,       // true après provisioning ou reset admin
    Instant pinResetAt
)
```

**Invariants :**
- `terminalCode` unique dans un tenant
- `mustChangePin = true` bloque toutes les actions de vente
- Un SellerTerminal `DISABLED` ne peut pas être réactivé
- La transition `ACTIVE → BLOCKED` est réversible (`BLOCKED → ACTIVE`)
- La transition `* → DISABLED` est définitive

---

## Identité Firebase

L'identité externe est gérée séparément dans `seller_terminal_external_identity`.

Le PIN est le password Firebase — il n'est **jamais** stocké en clair dans Tchalanet.

Flux de provisioning :
```
CreateSellerTerminalCommand
  → SellerTerminal créé (PENDING ou ACTIVE)
  → SellerTerminalIdentityProvisionPort.provision()
    → Firebase user créé avec email fictif + PIN initial
  → mustChangePin = true
```

---

## Commandes

| Commande | Permission requise | Effet |
|---|---|---|
| `CreateSellerTerminalCommand` | `seller_terminal.manage` | Crée + provisionne Firebase |
| `UpdateSellerTerminalCommand` | `seller_terminal.manage` | Mise à jour profil |
| `BlockSellerTerminalCommand` | `seller_terminal.block` | `ACTIVE → BLOCKED` |
| `UnblockSellerTerminalCommand` | `seller_terminal.block` | `BLOCKED → ACTIVE` |
| `DisableSellerTerminalCommand` | `seller_terminal.manage` | `* → DISABLED` (définitif) |
| `ResetSellerTerminalPinCommand` | `seller_terminal.pin.reset` | Génère PIN temp + Firebase reset + `mustChangePin=true` |
| `ChangeSellerTerminalPinCommand` | `ACTOR_SELLER_TERMINAL` (self) | Firebase reset + `mustChangePin=false` |
| `SetSellerTerminalCommissionRateCommand` | `seller_terminal.manage` | Taux de commission |
| `ResetSellerTerminalAccessCommand` | `seller_terminal.manage` | Reset accès legacy |

---

## Requêtes (queries)

| Query | Usage |
|---|---|
| `GetSellerTerminalQuery` | Lecture admin par ID |
| `GetSellerTerminalMeQuery` | Lecture self (acteur `SELLER_TERMINAL`) |
| `ListSellerTerminalsQuery` | Liste paginée (admin) |
| `GetSellerTerminalForSaleValidationQuery` | Validation lors d'une vente |
| `GetSellerTerminalCommissionStatsQuery` | Stats commission |
| `GetCurrentOperationalContextQuery` | Contexte opérationnel courant (admin POS) |

---

## Ports out

| Port | Implémentation |
|---|---|
| `SellerTerminalReaderPort` | JPA |
| `SellerTerminalWriterPort` | JPA |
| `SellerTerminalIdentityProvisionPort` | Firebase Admin SDK |

`SellerTerminalIdentityProvisionPort` expose :
- `provision(id, email, pin)` — crée l'utilisateur Firebase
- `resetPin(id, newPin)` — change le password Firebase
- `hasExternalIdentity(id)` — vérifie si l'identité Firebase existe

---

## Sécurité

- Endpoints `/api/v1/admin/seller-terminals/**` → `ACTOR_APP_USER` + permission
- Endpoints `/api/v1/tenant/seller-terminal/me/**` → `ACTOR_SELLER_TERMINAL` (self)
- `mustChangePin = true` → `GET /cashier/home` retourne `requiredStep: MUST_CHANGE_PIN`
- Le PIN temporaire (reset admin) est retourné **une seule fois** dans la réponse et n'est jamais stocké

---

## Permissions

| Code | Accordée à | Description |
|---|---|---|
| `seller_terminal.manage` | `TENANT_ADMIN` | Créer, modifier, désactiver |
| `seller_terminal.block` | `TENANT_ADMIN` | Bloquer / débloquer |
| `seller_terminal.pin.reset` | `TENANT_ADMIN` | Reset PIN admin |

---

## Tables DB

| Table | Contenu |
|---|---|
| `seller_terminal` | Entité principale (statut, code, nom, commission, mustChangePin, pinResetAt) |
| `seller_terminal_external_identity` | Lien vers Firebase UID |
| `seller_terminal_aud` | Audit Hibernate Envers |

---

## Frontières

`core.sellerterminal` ne doit pas :
- contenir de logique de vente (→ `core.sales`)
- appeler `features.cashier`
- écrire des permissions ou des rôles (→ `platform.accesscontrol`)
- exposer le PIN Firebase en clair dans des logs ou réponses persistées

---

## Références

- Auth POS : `tchalanet-docs/docs/01-architecture/flows/authentication-flow.md §4`
- Provisioning flow : `tchalanet-docs/docs/02-functional/flows/seller-onboarding.md`
- Role flows : `tchalanet-server/docs/conventions/context/role-flows.md §Acteur: SellerTerminal`
- Permissions : `platform.accesscontrol.api.PermissionKeys`
