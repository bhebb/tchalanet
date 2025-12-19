# DOMAIN_ACCESS_CONTROL.md — Domaine AccessControl

## 1) Scope & responsabilité

### Responsabilité principale

Déterminer **ce qu’un utilisateur a le droit de faire** dans un **tenant donné**, via :

- son appartenance (`tenant_user`)
- son rôle principal (`app_role`)
- la hiérarchie des rôles
- les permissions associées (`permission`, `role_permission`)

### Ce que le domaine fait

- Gère la relation **user ↔ tenant ↔ rôle** (membership).
- Gère les rôles applicatifs (globaux ou spécifiques tenant).
- Gère le catalogue de permissions (capacités métier atomiques).
- Gère l’association rôle → permissions.
- Résout les **permissions effectives** d’un user dans un tenant.
- Fournit une API applicative simple : _“ce user peut-il faire X ?”_

### Ce que le domaine ne fait pas

- Authentification / login (Keycloak).
- Gestion du profil utilisateur (`core.user`).
- Feature flags (Unleash).
- Audit applicatif transverse.
- Configuration tenant/outlet (heures, limites, etc.).
- Session / POS / terminal.

---

## 2) Modèle métier (V1)

> En V1, AccessControl est **DB-centric + CQRS**.  
> Pas de gros aggregate riche : on travaille sur des snapshots cohérents.

### Concepts clés

- **Permission**  
  Capacité métier atomique, ex : `ticket.sell`, `payout.execute`.
- **Role**  
  Ensemble de permissions, éventuellement avec un parent (héritage).
- **TenantUser (membership)**  
  Lien entre `user` et `tenant` avec :
  - rôle principal
  - niveau d’autonomie
  - statut (actif / supprimé)

### Enum

```java
AutonomyLevel = NONE | PARTIAL | FULL
```
