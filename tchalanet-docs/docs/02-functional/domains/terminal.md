# SellerTerminal — Domaine fonctionnel

> **Remplace** : les anciens concepts `Terminal`, `SalesSession`, `Seller` (retirés).  
> **Voir** : `tchalanet-docs/docs/00-guidelines/glossary.md#sellerterminal`

## Rôle

Un `SellerTerminal` est l'acteur de vente unique dans Tchalanet. Il combine en une seule entité :
- l'identité vendeur (machann/seller)
- l'équipement (device POS)
- les droits de vente

Les sessions POS, le binding device et les concepts `Outlet`-comme-prérequis sont supprimés.

## Cycle de vie

```
Provisioning admin
  → INACTIVE (Firebase créé, PIN temporaire)
  → Premier login + change-pin
  → ACTIVE (peut vendre)
  → BLOCKED (suspendu par admin, réversible)
  → DISABLED (désactivé définitivement)
```

## Cross-apps

### Web (portail admin)

- Pages : `/app/admin/seller-terminals`
- Actions : créer, modifier, bloquer/débloquer, désactiver, reset PIN

### Mobile POS (Flutter)

- Authentification Firebase (PIN-based)
- Flow premier login → changement PIN obligatoire
- Vente directe sans session ni binding

## API

```text
# Admin
GET    /api/v1/admin/seller-terminals
POST   /api/v1/admin/seller-terminals
GET    /api/v1/admin/seller-terminals/{id}
PUT    /api/v1/admin/seller-terminals/{id}
PATCH  /api/v1/admin/seller-terminals/{id}/block
PATCH  /api/v1/admin/seller-terminals/{id}/unblock
PATCH  /api/v1/admin/seller-terminals/{id}/disable
POST   /api/v1/admin/seller-terminals/{id}/pin-reset

# SellerTerminal (self)
GET    /api/v1/tenant/seller-terminal/me
POST   /api/v1/tenant/seller-terminal/me/change-pin
```

## Pointeurs (source of truth near-code)

- Backend : `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/sellerterminal/`
- Flow provisioning : `tchalanet-docs/docs/02-functional/flows/seller-onboarding.md`
- Flow auth POS : `tchalanet-docs/docs/01-architecture/flows/authentication-flow.md#4-path-seller_terminal`
