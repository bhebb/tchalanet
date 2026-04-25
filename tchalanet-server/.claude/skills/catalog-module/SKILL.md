---
name: catalog-module
description: Use when creating or modifying anything in the catalog/ layer — platform referentials, configuration, lookups, XCatalog, XView, XSummaryView, XAdminService, XCatalogImpl, projections, or referential cache. Catalog modules support full CRUD via SUPER_ADMIN (/platform/**) but never contain domain events or complex business invariants.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Module Catalog — Tchalanet

> Source de vérité : `openspec/context/75-catalog-rules.md`

## Rôle

Données de référence de la **plateforme** : configuration globale, lookups,
référentiels, calendriers. Modifiées uniquement par SUPER_ADMIN.
Consommées en lecture par `core/` et `features/`.

**La vraie distinction catalog/ vs core/ :**

|                    | catalog/                  | core/                       |
| ------------------ | ------------------------- | --------------------------- |
| Qui écrit          | SUPER_ADMIN uniquement    | TENANT_ADMIN + rôles métier |
| Scope              | Plateforme globale        | Domaine tenant              |
| Events             | ❌ Jamais                 | ✅ Domain events            |
| Invariants         | Simples (unicité, format) | Complexes (machine à états) |
| Fréquence écriture | Rare                      | Temps réel                  |
| Cache              | Toujours                  | Limité                      |
| Routes write       | `/platform/**`            | `/admin/**` ou `/tenant/**` |

## Structure interne obligatoire

```
catalog/<name>/
├─ api/
│  ├─ XCatalog.java              ← interface read-only (contrat public)
│  └─ model/
│     ├─ XView.java              ← projection complète
│     ├─ XSummaryView.java       ← projection légère (listes, dropdowns)
│     ├─ XRow.java               ← projection use-case spécifique
│     └─ XSearchCriteria.java    ← critères de recherche
└─ internal/
   ├─ read/                      ← XCatalogImpl (implements XCatalog)
   ├─ write/                     ← XAdminService (CRUD + cache eviction)
   ├─ mapper/                    ← Entity → View (MapStruct)
   ├─ persistence/               ← JPA entities + repositories
   ├─ cache/                     ← noms de cache
   └─ web/                       ← controllers SUPER_ADMIN (/platform/**)
```

## L'interface XCatalog — toujours read-only

```java
// ✅ L'interface publique API est READ-ONLY
// C'est le contrat consommé par core/ et features/
public interface PageModelTemplateCatalog {
    Optional<PageModelTemplateView> findByLogicalId(String logicalId);
    List<PageModelTemplateSummaryView> findAllActive();
}

// ✅ Le write passe par XAdminService dans internal/write/
// Jamais exposé dans l'interface api/
@Service
public class PageModelTemplateAdminService {
    public PageModelTemplateView create(PageModelTemplateCommand cmd) {
        var entity = mapper.toEntity(cmd);
        var saved = repo.save(entity);
        cacheManager.evict("catalog:pagemodel-template:active");
        return mapper.toView(saved);
    }
}
```

## Controllers — scope /platform/\*\* uniquement

```java
// ✅ Controllers catalog = SUPER_ADMIN via /platform/**
@RestController
@RequestMapping("/platform/page-model-templates")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformPageModelTemplateController {

    @GetMapping
    public ApiResponse<List<PageModelTemplateSummaryView>> list() { ... }

    @PostMapping
    public ApiResponse<PageModelTemplateView> create(@RequestBody @Valid ...) { ... }

    @PutMapping("/{id}")
    public ApiResponse<PageModelTemplateView> update(@PathVariable UUID id, ...) { ... }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) { ... }
}

// ❌ Jamais /admin/** ou /tenant/** dans catalog/
// Ces scopes appartiennent à core/ et features/
```

## Noms de cache — convention

```
catalog:<name>:active       ← liste active
catalog:<name>:id:<uuid>    ← entrée individuelle
catalog:<name>:all          ← tout (rare)
catalog:<name>:locale:<lang> ← si i18n
```

## Ce qui est INTERDIT dans catalog/

```java
// ❌ Domain events — jamais dans catalog/
domainEventPublisher.publish(...);

// ❌ Logique métier complexe
// Validation simple (format, unicité) OK
// Machine à états, règles business → core/

// ❌ Dépendance vers core/ ou features/
import com.tchalanet.server.core.*;    // interdit
import com.tchalanet.server.features.*; // interdit

// ❌ Exposer JpaEntity dans l'API
// Toujours mapper vers XView ou XSummaryView

// ❌ Transactions cross-tenant
// catalog/ opère au niveau plateforme (pas par tenant)
// Exception : tables avec BaseTenantEntity (ex: PricingOddsEntity)
//   dans ce cas RLS s'applique normalement
```

## Exemples de catalogs existants dans Tchalanet

| Catalog              | Rôle                            | Tables                |
| -------------------- | ------------------------------- | --------------------- |
| `pagemodeltemplate/` | Templates JSON de pages         | `page_model_template` |
| `plan/`              | Plans d'abonnement              | `plan`                |
| `pricing/`           | Cotes borlette                  | `pricing_odds`        |
| `drawchannel/`       | Canaux de tirage (Miami, NY...) | `draw_channel`        |
| `game/`              | Jeux disponibles                | `game`                |
| `theme/`             | Thèmes visuels                  | `theme_preset`        |
| `i18n/`              | Surcharges traductions          | `i18n_override`       |
| `settings/`          | Paramètres plateforme           | `setting`             |
| `resultslot/`        | Créneaux de résultats           | `result_slot`         |

## Accès depuis core/ et features/

```java
// ✅ Toujours via l'interface api/ — jamais l'implémentation directe
@RequiredArgsConstructor
public class SomeCommandHandler {
    private final PageModelTemplateCatalog templateCatalog; // ← api/
    // jamais PageModelTemplateCatalogImpl directement
}
```

## Nommage

| Rôle                   | Pattern                 |
| ---------------------- | ----------------------- |
| Interface publique     | `XCatalog`              |
| Implémentation lecture | `XCatalogImpl`          |
| Service write          | `XAdminService`         |
| Controller platform    | `PlatformXxxController` |
| Projection complète    | `XView`                 |
| Projection légère      | `XSummaryView`          |
| Projection spécifique  | `XRow`                  |

## Checklist avant tout nouveau catalog

- [ ] Interface `XCatalog` dans `api/` — read-only uniquement
- [ ] `XCatalogImpl` dans `internal/read/`
- [ ] `XAdminService` dans `internal/write/` avec cache eviction
- [ ] Projections dans `api/model/` : `XView`, `XSummaryView` minimum
- [ ] Noms de cache dans `internal/cache/`
- [ ] Controller dans `internal/web/` — route `/platform/**` uniquement
- [ ] `@PreAuthorize("hasRole('SUPER_ADMIN')")` sur le controller
- [ ] `ApiResponse<T>` sur tous les endpoints
- [ ] Aucun domain event
- [ ] Aucune dépendance vers `core/` ou `features/`
- [ ] Cache eviction dans `XAdminService` après chaque write
