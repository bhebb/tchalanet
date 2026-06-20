# Outlet — Domaine fonctionnel

## Rôle

Groupement géographique **optionnel** pour les SellerTerminals.

Un SellerTerminal peut être rattaché à un Outlet (ex: agence, kiosque, point de vente physique) ou opérer sans Outlet.

> **Changement important** : l'Outlet n'est plus un prérequis opérationnel pour vendre. Dans l'ancien modèle Seller+Terminal+Session, l'Outlet était obligatoire pour ouvrir une session. Ce n'est plus le cas.

## Usage actuel

- Groupement et reporting par lieu
- Filtrage dans les rapports admin
- Association optionnelle à un SellerTerminal

## Cross-apps

### Web

- Pages : `/app/admin/outlets`

### API

```text
GET   /api/v1/admin/outlets
POST  /api/v1/admin/outlets
GET   /api/v1/admin/outlets/{id}
PUT   /api/v1/admin/outlets/{id}
```

## Pointeurs (source of truth near-code)

- Backend : `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/outlet/`
