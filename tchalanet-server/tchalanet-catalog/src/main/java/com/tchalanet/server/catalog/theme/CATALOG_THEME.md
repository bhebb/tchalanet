> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/catalog.md` (theme overview)

# Domaine Theme

## 1. Rôle du domaine

Le domaine **Theme** est responsable de la **gestion des thèmes visuels par tenant** dans Tchalanet.

Il permet de :

- Créer et modifier des thèmes (draft ou publiés)
- Publier un thème comme thème actif d’un tenant
- Archiver un thème
- Conserver l’historique des versions via **Hibernate Envers**
- Fournir le thème actif au frontend (via tenant.active_theme_id)

Le domaine **ne fait pas** :

- Le rendu UI
- L’application CSS côté frontend
- La gestion des permissions (AccessControl)
- La gestion des presets globaux (référencés par `basePresetId`)

---

## 2. Concepts métier

### Theme (agrégat racine)

```java
Theme(
  UUID id,
  UUID tenantId,
  String basePresetId,
  String label,
  ThemeMode mode,
  short density,
  Map<String, Object> palette,
  Map<String, Object> tokens,
  Map<String, String> cssVars,
  ThemeStatus status,
  int version,
  Instant createdAt,
  Instant updatedAt
)
```

### ThemeMode

- `LIGHT`
- `DARK`
- `SYSTEM`

### ThemeStatus

- `DRAFT` : thème modifiable, non actif
- `PUBLISHED` : thème actif ou publiable
- `ARCHIVED` : thème obsolète (lecture seule)

---

## 3. Versioning & concurrence

Deux notions distinctes :

### 3.1 Optimistic Locking (technique)

- Géré via `@Version` (hérité de `BaseTenantEntity`)
- **Non exposé** dans le domaine
- Sert uniquement à la concurrence JPA

### 3.2 Version métier du thème

- Champ `theme_version`
- Incrémenté à chaque publication
- Sert à :

  - Identifier une version publiée
  - Revenir à une version précédente via Envers

➡️ **Le domaine ne dépend pas du champ `@Version`**

---

## 4. Historique (Hibernate Envers)

- Chaque modification d’un thème est auditée
- Les anciennes versions sont stockées dans les tables `_aud`
- Permet :

  - rollback vers une version précédente
  - comparaison historique

➡️ Pas de table custom d’historique nécessaire

---

## 5. Cycle de vie d’un thème

```text
DRAFT ── publish ──▶ PUBLISHED ── archive ──▶ ARCHIVED
  ▲          │
  └── edit ──┘
```

Règles :

- Un thème **publié reste modifiable** (nouvelle révision Envers)
- La publication incrémente `theme_version`
- Un seul thème publié actif par tenant

---

## 6. Règles de publication

### PublishThemeCommand

Responsabilités :

- Vérifier que le thème appartient bien au tenant cible
- Passer le statut à `PUBLISHED`
- Incrémenter `theme_version`
- Mettre à jour `tenant.active_theme_id`

➡️ La publication est **la seule opération** qui modifie le tenant

---

## 7. Multi-tenant & Super Admin

### Règle clé

> **Toutes les opérations Theme utilisent `effectiveTenantUuid()`**

- Tenant Admin : `originalTenant == effectiveTenant`
- Super Admin : `effectiveTenant` forcé via override

➡️ Le domaine Theme **ne connaît pas** la notion de Super Admin
➡️ La sécurité est assurée par AccessControl

---

## 8. Ports & architecture

### Ports de sortie

- `ThemeReaderPort`
- `ThemeWriterPort`

Implémentation :

- `ThemePersistenceAdapter`

### Repositories

- `JpaThemeRepository` (interne)
- `ThemeRestRepository` (Spring Data REST pour CRUD basique)

---

## 9. API & Handlers

### Queries

- `ListThemesQuery`
- `GetThemeByIdQuery`

### Commands

- `PublishThemeCommand`
- `ArchiveThemeCommand`

⚠️ Création / mise à jour basique : via Spring Data REST
⚠️ Actions métier critiques : via CommandBus uniquement

---

## 10. Sécurité & permissions

Exemples de permissions :

- `theme.read`
- `theme.publish`
- `theme.archive`

Les vérifications sont faites :

- en amont (Controller / Aspect)
- jamais dans le domaine

---

## 11. Décisions clés (résumé)

- Thème publié **modifiable**
- Historique géré par Envers
- `tenant.active_theme_id` mis à jour au publish
- `effectiveTenant` = source de vérité
- Pas de logique Super Admin dans le domaine
- Optimistic lock hors domaine

---

_Document : CATALOG_THEME.md — état validé du domaine Theme_
