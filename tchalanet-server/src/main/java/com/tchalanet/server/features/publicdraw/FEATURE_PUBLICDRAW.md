# Feature PublicDraw (BFF)

> BFF public pour afficher informations de tirages par slot (timezone/drawTime/labels) et résultats publiés. Ce module expose uniquement des modèles read-only pour l'UI publique.
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
  - `com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog` (listActive(), findByKey(...), ResultSlotView)
  - `com.tchalanet.server.core.drawresult.api.DrawResultReaderPort` / public query contracts exposés par `core.drawresult`
  - `NextDrawCalculator` (service utilitaire — idéalement dans core.common.time ou core.resultslot.service)
- Publicdraw contient uniquement du code read-only / projection + transformations UI contract.

### Écart documenté : projection SQL publique

`features.publicdraw.persistence.PublicDrawResultJdbcRepository` lit directement `draw_result` et `result_slot` avec `JdbcTemplate` pour produire une projection publique performante (`latest per slot`, historique paginé, tri whiteliste).

Cet écart est borné :

- lecture seule uniquement;
- aucune entité JPA core ou repository interne core/catalog n'est injecté;
- aucune écriture, aucun invariant provider/projection, aucune décision de validité résultat;
- le SQL filtre uniquement les résultats publiables (`dr.status = 'VALID'`) et les slots actifs;
- migration cible : remplacer cette projection par un contrat public `core.drawresult.api` quand ce contrat couvrira `latest per slot` et l'historique public avec métadonnées slot.

---

## 4) UI contracts recommandés

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
- PublicDrawResultController
- PublicDrawResultMapper
- app/
  - NextDrawCalculator
  - PublicDrawResultDetailsService
  - PublicDrawResultReader
  - PublicDrawResultSearchService
  - PublicLatestDrawResultsService
- model/
  - PublicDrawResultSearchCriteria
  - GetLatestPublicDrawResultsRequest
  - ListPublicDrawResultsRequest
  - PublicLatestDrawResultsResponse
  - PublicDrawResultItemResponse
- persistence/
  - PublicDrawResultJdbcRepository
  - PublicDrawResultJdbcReader
  - PublicDrawResultRepositoryReader
  - PublicDrawResultRow
```

---

## 7) Rôles & responsabilités

- Controller : parse params, créer un criteria/request si nécessaire, renvoyer `ApiResponse`
- Services : orchestrer appels à `ResultSlotCatalog` + reader publicdraw + `NextDrawCalculator`, construire UI contracts
- Mapper : transformer domain -> `PublicDrawResultItemResponse`
- Projection reader (optionnel) : SQL optimisé pour latest-per-channel ou historique paginé

---

## 8) Stratégies d'implémentation pratiques

- Latest per channel (impl simple) :
  - `slots = resultSlotCatalog.listActive()`
  - pour chaque slot : calculer la date/occurredAt locale, appeler `drawResultCatalog.findRefBySlotKeyAndDate(slotKey, drawDate)` ou `findByResultSlotIdAndOccurredAt` si disponible
  - récupérer le `DrawResult` complet si besoin et mapper
- SQL projection (optimisé) : utiliser `ROW_NUMBER() OVER (PARTITION BY slot_key ORDER BY occurred_at DESC)` et limiter par `rn <= :limitPerChannel`.

---

## 9) Exemples de signatures (Java)

- Controller (exemple)

```java
@GetMapping("/public/results/latest")
public ApiResponse<List<PublicLatestDrawResultsResponse>> latest(@RequestParam(defaultValue="1") int limitPerChannel) {
  return ApiResponse.success(latestService.latest(limitPerChannel));
}
```

---

## 10) Conventions & sécurité

- UI contract suffixes `Request`/`Response`.
- Wrappers d’ID en controllers; pas de UUID brut.
- ApiResponse + TchPage + TchPaging pour listes.
- Scope public: rate-limit; `noindex` si requis.

---

## 11) Consumed by

- **`features.pagemodel`** : utilise `PublicLatestDrawResultsService` via `DrawsProvider` pour l'affichage des derniers résultats dans le widget Home.

## 12) Pipeline source

Les résultats publics proviennent du pipeline backend `fetch -> apply` :

- `core.drawresult` fetch les providers via `result_slot.source_cfg`, projette Haïti via `result_slot.projection_cfg`, puis persiste `draw_result`.
- `core.draw` applique le résultat aux draws tenant-scoped.
- `features.publicdraw` expose uniquement des modèles read-only; aucune logique provider ou projection ne vit ici.
