# Domaine Catalog ResultSlot

> Référentiel global des « slots » de résultats (identifiants de créneaux: jour/heure/clé).
> C'est un **Catalogue** (données de référence), pas un domaine métier complexe.

---

## 1. Rôle du catalogue

**Responsabilité principale**

> Fournir un lookup stable des créneaux de résultats (slots) disponibles sur la plateforme pour la planification et l'ingestion.

**Ce que le catalogue fait**

- Référence les slots (clé, libellé, jour/heure, fuseau).
- Expose une API de lecture (`ResultSlotCatalog`) pour les autres domaines.
- Fournit un service d'administration (`ResultSlotAdminService`) pour le CRUD.

**Ce que le catalogue NE fait PAS**

- ❌ Pas de `QueryBus` / `CommandBus` (interdit pour les catalogues).
- ❌ Pas de `ports` (infrastructure interne directe).
- ❌ Pas de logique de tirage ou de publication.

---

## 2. Modèle de données (Internal Persistence)

**Entité JPA** : `ResultSlotEntity`

- `slotKey` (String, unique) — ex: "NY_MID"
- `provider` (String) — ex: NY / FL
- `timezone` (ZoneId)
- `drawTime` (LocalTime)
- `daysOfWeek` (EnumSet/String)
- `active` (boolean)
- `labelKey` (String) — ex: `slot.ny_mid.label`

---

## 3. API Publique (Read-only)

**Localisation** : `catalog.resultslot.api`

### ResultSlotCatalog (Interface)

- `List<ResultSlotView> listActive()`
- `Optional<ResultSlotView> getByKey(String slotKey)`
- `ResultSlotView requireByKey(String slotKey)`

### ResultSlotView (Record)

Localisé dans `catalog.resultslot.api.model`.
Contient toutes les propriétés nécessaires à l'affichage et au filtrage.

---

## 4. Implémentation Interne

### Read Side (`internal.read`)

- `ResultSlotCatalogImpl` : Implémente `ResultSlotCatalog`.
- Accède directement au `ResultSlotRepository`.
- Gère le caching `@Cacheable`.

### Write Side (`internal.write`)

- `ResultSlotAdminService` : Gère les modifications (Create/Update/ToggleStatus).
- Utilisé uniquement par les contrôleurs d'administration.
- Gère l'éviction du cache `@CacheEvict`.

### Mapping (`internal.mapper`)

- `ResultSlotMapper` (MapStruct) : `Entity` ↔ `View`.

---

## 5. Administration (Web)

**Localisation** : `catalog.resultslot.internal.web`

- `ResultSlotAdminController` : Exposition des endpoints CRUD pour le back-office.
- Sécurisé par rôles `SUPER_ADMIN` / `TENANT_ADMIN`.

---

## 6. Cache

Stratégie de cache recommandée :

- `catalog:resultslot:active` (liste complète)
- `catalog:resultslot:by_key::{key}` (entrée unique)

TTL : Long (~24h) car les slots changent rarement. Invalidation manuelle via le `AdminService`.

---

## 7. Dépendances

- **Autorisé** : `core` et `features` appellent `ResultSlotCatalog`.
- **Interdit** :
  - Personne ne doit dépendre de `internal.*`.
  - Le catalogue ne doit pas dépendre du `core`.
  - Pas d'usage de `application.port`.
