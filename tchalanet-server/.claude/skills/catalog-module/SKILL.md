---
name: catalog-module
description: Use when creating or modifying anything in the catalog/ layer — referentials, lookups, configuration, calendars, XCatalog, XView, XSummaryView, XRow, XAdminService, XCatalogImpl, read-only projections, or referential cache.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Module Catalog — Tchalanet

## Rôle

Référentiels **read-mostly** : données de configuration, lookups, calendriers.
Consommé par `features/` en lecture. Jamais de side-effects.

## Structure interne obligatoire

```
catalog/<name>/
├─ api/
│  ├─ XCatalog.java              ← interface read-only (le contrat public)
│  └─ model/
│     ├─ XView.java              ← projection complète
│     ├─ XSummaryView.java       ← projection légère (listes, dropdowns)
│     ├─ XRow.java               ← projection use-case spécifique
│     └─ XSearchCriteria.java    ← critères de recherche
└─ internal/
   ├─ read/                      ← XCatalogImpl (implements XCatalog)
   ├─ write/                     ← XAdminService (CRUD admin + cache eviction)
   ├─ mapper/                    ← Entity → View (MapStruct)
   ├─ persistence/               ← JPA entities + repositories
   ├─ cache/                     ← noms de cache (catalog:<name>:active, ...)
   └─ web/                       ← controllers admin (SUPER_ADMIN / TENANT_ADMIN only)
```

## Règles strictes

```java
// ✅ L'interface XCatalog est READ-ONLY
public interface DrawCalendarCatalog {
    Optional<DrawCalendarView> findById(DrawCalendarId id);
    List<DrawCalendarSummaryView> findAllActive();
}

// ✅ XAdminService gère le CRUD + invalide le cache
@Service
public class DrawCalendarAdminService {
    public void create(DrawCalendarCommand cmd) {
        // persist + evict cache
        cacheManager.evict("catalog:draw-calendar:active");
    }
}
```

## Noms de cache — convention

```
catalog:<name>:active      ← liste active
catalog:<name>:id:<uuid>   ← entrée individuelle
catalog:<name>:all         ← tout (rare)
```

## Ce qui est INTERDIT dans catalog/

```java
// ❌ Domain events
domainEventPublisher.publish(...); // jamais dans catalog/

// ❌ Side-effects métier
// catalog/ ne déclenche pas de logique business

// ❌ Dépendance vers core/ ou features/
import com.tchalanet.server.core.*; // interdit

// ❌ Logique métier dans XCatalogImpl
// XCatalogImpl = lecture + mapping uniquement

// ❌ Exposer JpaEntity dans l'API
// Toujours mapper vers XView ou XSummaryView
```

## Accès depuis features/

```java
// ✅ features/ consomme catalog/ via l'interface publique uniquement
@RequiredArgsConstructor
public class SomeOrchestrator {
    private final DrawCalendarCatalog drawCalendarCatalog; // ← interface api/
    // jamais DrawCalendarCatalogImpl directement
}
```

## Nommage

| Rôle                   | Pattern                                          |
| ---------------------- | ------------------------------------------------ |
| Interface publique     | `XCatalog` (ex: `DrawCalendarCatalog`)           |
| Implémentation lecture | `XCatalogImpl`                                   |
| Service admin/write    | `XAdminService` (ex: `DrawCalendarAdminService`) |
| Projection complète    | `XView`                                          |
| Projection légère      | `XSummaryView`                                   |
| Projection spécifique  | `XRow`                                           |
| Controller admin       | `AdminXxxController`                             |

## Checklist avant tout nouveau catalog

- [ ] Interface `XCatalog` dans `api/` — read-only uniquement
- [ ] `XCatalogImpl` dans `internal/read/`
- [ ] `XAdminService` dans `internal/write/` avec cache eviction
- [ ] Projections dans `api/model/` : `XView`, `XSummaryView` minimum
- [ ] Noms de cache documentés dans `internal/cache/`
- [ ] Aucun domain event
- [ ] Aucune dépendance vers `core/` ou `features/`
- [ ] Controllers admin : SUPER_ADMIN ou TENANT_ADMIN uniquement
