---
name: feature-module
description: Use when creating or modifying anything in the features/ layer — multi-domain orchestration, BFF, vertical slices, XxxOrchestrator, feature XxxService, XxxRequest, XxxResponse, composition of core/ and catalog/, or new UI/navigation-oriented endpoints.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Module Feature — Tchalanet

## Rôle

Orchestration, BFF, et composition multi-domaines.
Une feature = un capability produit. Un slice = une zone UI/navigation.

## Structure interne

```
features/<feature_key>/
└─ <slice_key>/
   ├─ web/       ← XxxController (boundary HTTP, thin)
   ├─ app/       ← XxxService / XxxOrchestrator (logique d'orchestration)
   ├─ model/     ← XxxRequest, XxxResponse, XxxView, XxxItem
   ├─ mapper/    ← XxxMapper / XxxWebMapper
   ├─ dynamic/   ← providers/plug-ins (optionnel, si ≥ 3 classes)
   └─ shared/    ← helpers internes (optionnel, si ≥ 3 classes)
```

**Règle des 3** : un sous-package n'est créé que si ≥ 3 classes l'occupent.

## Rôle de chaque couche

### web/ — Controller (thin)

```java
// ✅ Validation + mapping + délégation uniquement, 0 logique
@RestController
@RequiredArgsConstructor
public class SalesSummaryController {

    private final SalesSummaryOrchestrator orchestrator;
    private final SalesSummaryWebMapper mapper;

    @GetMapping("/tenant/sales/summary")
    public ApiResponse<SalesSummaryResponse> getSummary(@Valid SalesSummaryRequest req) {
        return ApiResponse.ok(orchestrator.getSummary(mapper.toQuery(req)));
    }
}
```

### app/ — Orchestrateur / Service

```java
// ✅ Compose core/ + catalog/, pas de logique métier directe
@Service
@RequiredArgsConstructor
public class SalesSummaryOrchestrator {

    private final CommandBus commandBus;         // pour les commandes core/
    private final DrawCalendarCatalog catalog;   // lecture catalog/
    private final TicketReaderPort ticketReader; // lecture core/ si autorisé

    public SalesSummaryResponse getSummary(SalesSummaryQuery query) {
        var calendar = catalog.findActiveCalendar();
        var tickets = ticketReader.findByPeriod(query.from(), query.to());
        return SalesSummaryResponse.of(calendar, tickets);
    }
}
```

### model/ — Request / Response

```java
// ✅ Nommage strict
public record SalesSummaryRequest(@NotNull LocalDate from, @NotNull LocalDate to) {}
public record SalesSummaryResponse(BigDecimal total, int count, List<SalesSummaryItem> items) {}
public record SalesSummaryItem(TicketId ticketId, Amount amount, LocalDateTime placedAt) {}

// ❌ Jamais XxxDto, XxxModel (ambigu)
// ❌ Jamais exposer une JpaEntity ou un aggregate domain dans le response
```

## Nommage

| Rôle            | Pattern                                                                             |
| --------------- | ----------------------------------------------------------------------------------- |
| Controller      | `XxxController`, `PublicXxxController`, `AdminXxxController`, `TenantXxxController` |
| Orchestrateur   | `XxxOrchestrator` (multi-domain)                                                    |
| Service feature | `XxxService` (mono-domain)                                                          |
| Request         | `XxxRequest`                                                                        |
| Response        | `XxxResponse`, `XxxItemResponse`                                                    |
| Mapper web      | `XxxWebMapper`                                                                      |

## Routes — conventions

```java
// Prefix global /api/v1 configuré dans spring.mvc.servlet.path
// ❌ Ne JAMAIS répéter /api/v1 dans les @RequestMapping

// Scopes
@RequestMapping("/public/...")    // non authentifié
@RequestMapping("/tenant/...")    // scope tenant
@RequestMapping("/admin/...")     // admin platform
@RequestMapping("/platform/...")  // plateforme centrale
@RequestMapping("/_sdr/...")      // service-to-service

// Ressources en nouns pluriels
/tickets, /draws, /outlets, /payouts

// Actions non-CRUD explicites
@PostMapping("/tickets/{id}/approve")
@PostMapping("/payouts/{id}/settle")
@PostMapping("/tickets/{id}/void")
```

## Réponses HTTP obligatoires

```java
// ✅ Tous les endpoints JSON retournent ApiResponse<T>
return ApiResponse.ok(result);
return ApiResponse.created(newId);

// ✅ Collections paginées
return ApiResponse.ok(TchPage.of(page));

// ✅ Erreurs — ProblemDetail (jamais wrappé dans ApiResponse)
// géré automatiquement par l'exception handler global
```

## Ce qui est INTERDIT dans features/

```java
// ❌ Logique métier directe (règles business, invariants)
// → déléguer à un CommandHandler dans core/

// ❌ Accès direct aux JpaRepository de core/
// → passer par les output ports

// ❌ features/ dépend de features/ (cross-feature interdit)
// → extraire dans common/ si besoin partagé

// ❌ XxxDto, XxxModel comme noms de classes
// ❌ /api/v1 dans les @RequestMapping des controllers
```

## Checklist avant toute nouvelle feature/slice

- [ ] Controller thin : validation + mapping + délégation uniquement
- [ ] Orchestrateur/Service compose `core/` et `catalog/`, pas de logique métier
- [ ] Modèles nommés `XxxRequest`, `XxxResponse`, `XxxItem` (jamais `XxxDto`)
- [ ] Route sans `/api/v1`, avec le bon scope (`/tenant/`, `/admin/`, etc.)
- [ ] Retourne `ApiResponse<T>` ou `TchPage<T>`
- [ ] Règle des 3 respectée pour les sous-packages
