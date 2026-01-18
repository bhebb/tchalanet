# Feature PublicDraw (BFF)

> BFF public pour afficher informations de tirages par slot (timezone/drawTime/labels) et résultats publiés. Ce module s’appuie uniquement sur des façades globales (ResultSlotCatalog, DrawResultCatalog, NextDrawCalculator) et n’accède jamais directement aux repos/entités internes.
> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/publicdraw.md`

---

## 1) Purpose

- Home : afficher les derniers résultats par canal + next draw (prochaine échéance).
- Historique : recherche / filtrage (provider, slotKey, période) et pagination.
- Détail (optionnel) : afficher un draw précis (slot + date).

---

## 2) API HTTP publique

- GET `/public/results/latest`

  - Query params : `limitPerChannel` (int, default = 1)
  - Réponse : `ApiResponse<List<PublicLatestDrawResultsResponse>>`
  - Usage : écran Home (dernier résultat + next draw)

- GET `/public/results`

  - Query params : `slotKey` (opt), `provider` (opt), `from` (yyyy-MM-dd, opt), `to` (yyyy-MM-dd, opt), `page` (int>=0, default=0), `size` (int>0, default=20), `sort` (opt)
  - Réponse : `ApiResponse<TchPage<PublicDrawResultItemResponse>>`
  - Usage : écran Historique

- GET `/public/results/{slotKey}/{drawDate}`
  - Path params : `slotKey`, `drawDate` (yyyy-MM-dd)
  - Réponse : `ApiResponse<PublicDrawResultItemResponse>` ou 404
  - Usage : détail (optionnel)

---

## 3) Contraintes & dépendances autorisées

- Interdiction : accès direct aux repositories JPA/JDBC de `drawresult` ou `resultslot`.
- Autorisé :
  - `com.tchalanet.server.core.resultslot.application.ResultSlotCatalog` (listActive(), findBySlotKey(...), ResultSlotView)
  - `com.tchalanet.server.catalog.api.drawresult.DrawResultCatalog` (findById, findByDateRange, findByResultSlotIdAndOccurredAt, findRefBySlotKeyAndDate)
  - `NextDrawCalculator` (service utilitaire — idéalement dans core.common.time ou core.resultslot.service)
- Publicdraw contient uniquement du code read-only / projection + transformations DTO.

---

## 4) DTOs recommandés

- `PublicLatestDrawResultsResponse`

  - slotKey: String
  - provider: String
  - timezone: String (ex: "America/New_York")
  - drawTime: String (HH:mm local au slot)
  - nextScheduledAt: Instant | ZonedDateTime (nullable)
  - nextDrawLabel: String (nullable)
  - nextIsOpen: Boolean (nullable)
  - nextIsClosingSoon: Boolean (nullable)
  - results: List<PublicDrawResultItemResponse>
  - labelKey: String (recommandé) — stable, à traduire côté front

- `PublicDrawResultItemResponse`
  - id: UUID
  - occurredAt: Instant | ZonedDateTime
  - numbersMain: List<Integer>
  - numbersExtra: List<Integer>
  - quality: String
  - source: String
  - status: String
  - provider: String
  - slotKey: String
  - slotTimezone: String
  - slotDrawTime: String
  - metadata: Map<String,Object> (optionnel)

Remarque : fournir à la fois `labelKey` (stable, front traduit) et un `channelLabel` déjà formaté pour faciliter l'UI.

---

## 5) Cache (latence UI)

- TTLs conseillés :
  - `publicdraw.latest::{limit}` — TTL 30–60s
  - `publicdraw.history::{criteria_hash}` — TTL 30s
- Clés proposées :
  - latest global : `publicdraw.latest::{limit}`
  - latest per-slot : `publicdraw.latest::slot::{slotKey}::{limit}`
  - history : `publicdraw.history::slot={slotKey}|prov={provider}|from={from}|to={to}|p={page}|s={size}`
- Eviction :
  - Quand `draw_result` est modifié (writer adapter), évincer `publicdraw.latest` et `publicdraw.history` pour le `slotKey` impacté (ou global si unsure).
  - Implémentation : utiliser `@CacheEvict` sur l'adapter writer ou publier `DrawResultChanged` et laisser un listener évincer.

---

## 6) Architecture recommandée (packages)

```
features/publicdraw
- application
  - query
    - model
      - GetLatestPublicDrawResultsQuery
      - ListPublicDrawResultsQuery
      - (GetPublicDrawResultQuery) optional
    - handler
      - GetLatestPublicDrawResultsQueryHandler
      - ListPublicDrawResultsQueryHandler
- infra
  - web
    - PublicDrawResultController (endpoints)
    - mapper (mappage domain -> DTO)
  - persistence (optionnel: projection SQL / JDBC pour performance)
    - PublicDrawResultJdbcRepository (projections, latest per channel)
```

---

## 7) Rôles & responsabilités

- Controller : parse params, créer Query, renvoyer `ApiResponse`
- Handler : orchestrer appels à `ResultSlotCatalog` + `DrawResultCatalog` + `NextDrawCalculator`, construire DTOs
- Mapper : transformer domain -> `PublicDrawResultItemResponse`
- Projection adapter (optionnel) : SQL optimisé pour latest-per-channel ou historique paginé

---

## 8) Stratégies d'implémentation pratiques

- Latest per channel (impl simple) :
  - `slots = resultSlotCatalog.listActive()`
  - pour chaque slot : calculer la date/occurredAt locale, appeler `drawResultCatalog.findRefBySlotKeyAndDate(slotKey, drawDate)` ou `findByResultSlotIdAndOccurredAt` si disponible
  - récupérer le `DrawResult` complet si besoin et mapper
- SQL projection (optimisé) : utiliser `ROW_NUMBER() OVER (PARTITION BY slot_key ORDER BY occurred_at DESC)` et limiter par `rn <= :limitPerChannel`.

---

## 9) Exemples de signatures (Java)

- Query records

```java
public record GetLatestPublicDrawResultsQuery(int limitPerChannel) implements Query<List<PublicLatestDrawResultsResponse>> {}
public record ListPublicDrawResultsQuery(String slotKey, String provider, LocalDate from, LocalDate to, int page, int size) implements Query<TchPage<PublicDrawResultItemResponse>> {}
```

- Controller (exemple)

```java
@GetMapping("/public/results/latest")
public ApiResponse<List<PublicLatestDrawResultsResponse>> latest(@RequestParam(defaultValue="1") int limitPerChannel) {
  var res = queryBus.send(new GetLatestPublicDrawResultsQuery(limitPerChannel));
  return ApiResponse.success(res);
}
```

---

## 10) Conventions & sécurité

- DTO suffixes `Request`/`Response`.
- Wrappers d’ID en controllers; pas de UUID brut.
- ApiResponse + TchPage + TchPaging pour listes.
- Scope public: rate-limit; `noindex` si requis.
