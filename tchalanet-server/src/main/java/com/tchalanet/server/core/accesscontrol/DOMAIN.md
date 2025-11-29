# Domaine AccessControl

## 1. Rôle du domaine

**Responsabilité principale**

Déterminer ce qu’un utilisateur a le droit de faire dans un tenant donné.

**Ce que le domaine fait**

- Gère la relation user ↔ tenant ↔ rôle (membership).
- Résout la hiérarchie des rôles (héritage).
- Agrège les permissions effectives pour un utilisateur.
- Expose une API métier simple : « ce user peut‑il faire X ? »

**Ce que le domaine ne fait pas**

- Authentification (Keycloak).
- Feature flags (Unleash).
- Audit applicatif.
- Configuration visuelle / thèmes.
- Gestion de profil utilisateur (app_user) hors du tenant.

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `TenantMembership` (snapshot de `tenant_user`).
- `Role` (snapshot de `app_role`).
- `Permission` (VO, string ex: `"ticket.create"`).
- `EffectivePermissions` (ensemble des permissions d’un user dans un tenant).

### Invariants métier

- Un user n’a qu’un rôle principal par tenant.
- Les permissions sont déduites du rôle et de sa hiérarchie.
- Si l’utilisateur n’a pas de ligne `tenant_user`, il n’a aucun droit dans ce tenant.

> Valeur métier clé : une _permission_ est une capacité métier atomique (ex : `ticket.create`, `draw.override`, `session.open`).

## 3. Cas d’utilisation (ports d’entrée)

Les ports d’entrée ont été déplacés dans le package `application.port.in` (convention ARCHITECTURE.md). Les interfaces public‑API du domaine (use cases) disponibles pour les controllers/adapters sont :

- `com.tchalanet.server.accesscontrol.application.port.in.CheckUserPermissionsUseCase`

  - Description : vérifie si un utilisateur possède toutes les permissions demandées.
  - Paramètres : `tenantId`, `userId`, liste de permissions.
  - Résultat : lève `PermissionsDeniedException` si manquant.

- `com.tchalanet.server.accesscontrol.application.port.in.GetEffectivePermissionsUseCase`

  - Description : calcule toutes les permissions d’un utilisateur dans un tenant.
  - Utilité : debug, affichage dashboard admin, UI avancées.

- `com.tchalanet.server.accesscontrol.application.port.in.RoleAdminUseCase`

  - Description : gestion CRUD des rôles applicatifs (global + tenant).

- `com.tchalanet.server.accesscontrol.application.port.in.PermissionAdminUseCase`
  - Description : consultation et administration du mapping rôle → permissions.

> Remarque : les anciens ports situés sous `accesscontrol.domain.ports` ont été dépréciés et migrés vers `application.port.*`. Si des références subsistent dans d'autres modules, elles doivent être remplacées par les nouveaux packages.

## 4. Ports de sortie (dépendances externes)

Les ports de sortie sont placés sous `application.port.out` :

- `com.tchalanet.server.accesscontrol.application.port.out.TenantUserDirectoryPort`

  - Parle à : table `tenant_user`.
  - Rôle : récupérer le rôle principal + autonomie d’un user dans un tenant.

- `com.tchalanet.server.accesscontrol.application.port.out.PermissionCatalogPort`
  - Parle à : `app_role`, `role_permission` (+ hiérarchie).
  - Rôle : retourner toutes les permissions accordées par un rôle (y compris parents).

Les implémentations concrètes vivent dans `accesscontrol.infra.adapter` / `accesscontrol.infra.persistence`.

> Remarque : `accesscontrol` est le point d’entrée unique pour l’accès aux tables `app_role` / `role_permission`.

## 5. REST / DTOs

Conventions REST pour ce domaine :

- Les DTOs d'entrée (requests) : `com.tchalanet.server.accesscontrol.infra.web.dto.*Request` (ex : `UpsertRoleRequest`).
- Les DTOs de sortie (responses) : `com.tchalanet.server.accesscontrol.infra.web.dto.*Response` (ex : `RoleAdminResponse`, `PermissionResponse`).
- Les controllers dans `infra.web` convertissent `*Request` → `Command` (records in `application.command.model`) et `domain` / `application` results → `*Response`.

Exemples :

- `AccessControlAdminController` : appelle `RoleAdminUseCase` / `PermissionAdminUseCase` et renvoie `RoleAdminResponse` / `PermissionResponse`.

## 6. Architecture & règles

- Les ports n’appartiennent plus au package `domain` : ils vivent sous `application.port.in` et `application.port.out` (ARCHITECTURE.md).
- Le `domain` contient uniquement le modèle métier pur et les exceptions.
- `infra` contient les controllers, mappers, JPA entities et adapters qui implémentent les ports.

## 7. Intégration avec les autres domaines

Identique à l’ancien document : `ticket`, `draw`, `session`, `tenantconfig`, `pagemodel` appellent désormais `application.port.in.CheckUserPermissionsUseCase`.

## 8. Migration / notes opérationnelles

- Les anciens fichiers `accesscontrol/domain/ports/*` ont été remplacés par des placeholders @Deprecated pendant la migration. Ils doivent être supprimés définitivement une fois que tous les modules auront été mis à jour pour importer `application.port.*`.
- Avant suppression finale, lancer une recherche globale pour s'assurer qu'aucun import ne référence `accesscontrol.domain.ports`.

---

*Fichier mis à jour : `accesscontrol/DOMAIN.md` — reflète la migration des ports et la convention DTO `*Response`.\*
