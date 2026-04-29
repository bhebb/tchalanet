# Domaine Core DrawResult

> Ingestion, persistance et exposition des **résultats de tirage externes** (providers US lottery), avec la **projection Haïti** déjà calculée. Domaine **global** (non tenanté), point d'entrée unique du monde extérieur dans le système.

---

## 1. Rôle du domaine

**Responsabilité principale**

> Orchestrer l'ingestion des résultats externes via `core.uslottery`, déclencher la projection Haïti via `core.haiti`, persister le `draw_result` global avec son statut, et exposer ces résultats aux domaines consommateurs (principalement `core.draw`).

**Ce que le domaine fait**

- Orchestre le **fetch tick** (scheduler toutes les 5 min) qui scanne les `result_slot` actifs.
- Appelle `core.uslottery.ExternalResultsFetchPort` pour récupérer le payload provider.
- Appelle `core.haiti.HaitiLotteryPort` pour projeter en `lot1..lot4`.
- Persiste `draw_result` (upsert idempotent par `(slot, occurred_at)`).
- Gère le statut `PROVISIONAL → FINAL` (et `ERROR`).
- Permet l'override admin (`OverrideDrawResultCommand`) et la saisie manuelle (`RecordManualDrawResultCommand`) via ops.
- Publie `DrawResultIngestedEvent` après commit (uniquement à transition vers `FINAL`).
- Expose `DrawResultReaderPort` à `core.draw` (read-only).
- Expose un API admin `/admin/draw-results` (lecture).

**Ce que le domaine ne fait pas**

- ❌ Ne connaît pas les tenants (table globale, pas de RLS).
- ❌ Ne connaît pas les `draw_channel`.
- ❌ Ne crée jamais de `Draw` (rôle de `core.draw`).
- ❌ Ne déclenche jamais le settlement (rôle de `core.draw`).
- ❌ N'implémente pas la logique de projection (rôle de `core.haiti`).
- ❌ N'appelle pas directement les providers HTTP (rôle de `core.uslottery`).

---

## 2. Modèle métier (agrégats / entités)

### Agrégat principal : `DrawResult`

- **Identité** : `DrawResultId` (typed wrapper sur UUID).
- **Identité métier** : `UNIQUE(result_slot_id, occurred_at)`.
- **JPA Entity** : `BaseEntity` (pas de tenant_id, pas de RLS).

**Champs clés** :

| Champ            | Type               | Sémantique                                                     |
| ---------------- | ------------------ | -------------------------------------------------------------- |
| `resultSlotId`   | `ResultSlotId`     | FK vers `result_slot` (catalog)                                |
| `occurredAt`     | `Instant`          | Moment du tirage (via `OccurredAtResolver`)                    |
| `sourceResult`   | `JSON`             | Payload normalisé `{pick3: "425", pick4: "1234"}`              |
| `haitiResult`    | `JSON`             | Projection `{lot1: "425", lot2: "12", lot3: "34", lot4: "42"}` |
| `rawPayload`     | `JSON?`            | Payload provider brut (debug)                                  |
| `status`         | `DrawResultStatus` | `PROVISIONAL` / `FINAL` / `ERROR`                              |
| `quality`        | `String?`          | Qualité provider (COMPLETE/PARTIAL/SUSPECT)                    |
| `source`         | `String?`          | `API` / `MANUAL` / `IMPORT`                                    |
| `sourceHash`     | `String?`          | Hash SHA-256 du payload (idempotence + audit)                  |
| `fetchedAt`      | `Instant`          | Moment du fetch côté serveur                                   |
| `overrideReason` | `String?`          | Audit override                                                 |
| `flags`          | `JSON`             | Métadonnées extensibles                                        |

### État : `DrawResultStatus`

```
        ┌──────────────┐
   ────▶│ PROVISIONAL  ├──────▶ FINAL  (enrichi par fetch suivant ou ops)
        └──────────────┘
              │
              └──────▶ ERROR  (provider down, payload invalide)
```

| État          | Sémantique                                                    |
| ------------- | ------------------------------------------------------------- |
| `PROVISIONAL` | Résultat reçu mais qualité incertaine (`quality != COMPLETE`) |
| `FINAL`       | Résultat consolidé (qualité OK, vérifié)                      |
| `ERROR`       | Erreur provider, à traiter manuellement                       |

> **Transition** : `PROVISIONAL → FINAL` peut se faire automatiquement (fetch suivant qui confirme) ou manuellement (ops `manual` ou `override`).

### Invariants métier

- `(result_slot_id, occurred_at)` unique.
- `haiti_result` doit contenir `lot1`, `lot2`, `lot3`, `lot4` (contrainte CHECK SQL).
- `occurred_at` est calculé via `OccurredAtResolver` avec la timezone du **slot**, jamais celle du tenant.
- Pas de `tenant_id` (table globale, `BaseEntity`).

> Valeur métier clé :
> `DrawResult` est la **source de vérité externe partagée** entre tous les tenants. Un seul fetch HTTP par slot, un seul résultat persisté, consommé par N tenants via leurs `Draw`.

---

## 3. Architecture (75-core-rules.md)

### Package structure

```
core/drawresult/
├── api/                                # Contrat public exposé
│   ├── DrawResultReaderPort.java      # consumed by core.draw
│   └── DrawResultView.java
├── domain/                             # Modèle pur
│   ├── model/
│   │   ├── DrawResult.java
│   │   └── DrawResultStatus.java
│   └── event/
│       └── DrawResultIngestedEvent.java
├── application/
│   ├── command/
│   │   ├── model/                     # Commands (records)
│   │   │   ├── FetchExternalResultsWindowCommand.java
│   │   │   ├── OverrideDrawResultCommand.java
│   │   │   └── RecordManualDrawResultCommand.java
│   │   └── handler/                   # @UseCase + @TchTx
│   ├── query/
│   │   ├── model/
│   │   │   ├── ListDrawResultsQuery.java
│   │   │   ├── GetDrawResultByIdQuery.java
│   │   │   └── GetDrawResultBySlotAndDateQuery.java
│   │   └── handler/
│   └── port/
│       └── out/
│           ├── ExternalResultsFetchPort.java     # implemented by core.uslottery
│           ├── DrawResultWriterPort.java
│           └── DrawResultReaderPort.java
└── infra/
    ├── persistence/
    │   ├── DrawResultJpaEntity.java
    │   └── DrawResultJdbcRepository.java
    ├── scheduler/
    │   └── ExternalResultsFetchTickScheduler.java
    ├── config/
    │   └── DrawResultsProperties.java
    └── web/
        ├── DrawResultsController.java            # /admin/draw-results
        ├── mapper/DrawResultWebMapper.java
        └── model/DrawResultResponse.java
```

### Règles d'isolation

- ✅ `domain/` = pur (pas de Spring, pas de Jackson).
- ✅ `application/` = handlers + ports out.
- ✅ `infra/` = adapters (JDBC, scheduling, web).
- ✅ Aucune dépendance vers `core.draw` (interdit — `core.draw` consomme via le port).
- ✅ `DrawResultReaderPort` exposé en `api/` (lecture seule, point d'entrée pour `core.draw`).

---

## 4. API Publique (Commands & Queries via Bus)

### Commands (write — `CommandBus.send(...)`)

| Command                             | Trigger                      | Effet                                     | Gate                                    |
| ----------------------------------- | ---------------------------- | ----------------------------------------- | --------------------------------------- |
| `FetchExternalResultsWindowCommand` | scheduler `fetchTick` ou ops | Fetch + projection + upsert `draw_result` | `RESULTS_EXTERNAL_FETCH`                |
| `OverrideDrawResultCommand`         | ops uniquement               | Modifie un `draw_result` existant         | `RESULTS_EXTERNAL_OVERRIDE` _(à créer)_ |
| `RecordManualDrawResultCommand`     | ops uniquement               | Saisie manuelle (provider down)           | `RESULTS_EXTERNAL_MANUAL`               |

**Spec compliance** : DR1 (write commands).

`RESULTS_EXTERNAL_REFRESH` est une orchestration ops : le BFF `features.ops` vérifie le gate puis envoie successivement `FetchExternalResultsWindowCommand` et `ApplyExternalResultsWindowCommand`. Il n'existe pas de command handler de refresh dans `features.ops`.

### Queries (read — `QueryBus.send(...)`)

| Query                             | Usage                                   |
| --------------------------------- | --------------------------------------- |
| `ListDrawResultsQuery`            | Admin (liste paginée)                   |
| `GetDrawResultBySlotAndDateQuery` | Apply tick (via `DrawResultReaderPort`) |
| `GetDrawResultByIdQuery`          | Lookup direct                           |

**Spec compliance** : DR2 (read queries).

### Port exposé : `DrawResultReaderPort`

```java
public interface DrawResultReaderPort {
  Optional<DrawResultView> findBySlotAndDate(String slotKey, LocalDate date);
  Optional<DrawResultView> findById(DrawResultId id);
}
```

Consommé par `core.draw.application.port.out.DrawResultReaderPort` (port read-only, pattern "Core → Core lecture simple" cf. `inter_domain_calls.md` § 5.2).

### REST Controllers

#### `DrawResultsController` — `/admin/draw-results` (lecture uniquement)

| Method | Endpoint                        | HTTP | Returns                                    |
| ------ | ------------------------------- | ---- | ------------------------------------------ |
| GET    | `/admin/draw-results`           | 200  | `ApiResponse<TchPage<DrawResultResponse>>` |
| GET    | `/admin/draw-results/today`     | 200  | `ApiResponse<TchPage<DrawResultResponse>>` |
| GET    | `/admin/draw-results/last-days` | 200  | `ApiResponse<TchPage<DrawResultResponse>>` |

- ✅ `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`
- ✅ Retourne `ApiResponse<T>`.

> Les endpoints **write** (`fetch`, `refresh`, `override`, `manual`) vivent dans `features.ops.DrawResultsOpsController` et routent vers les commands de ce domaine.

**Spec compliance** : DR3 (admin endpoints).

---

## 5. Events publiés

| Event                     | Quand                                 | Tenanté     | Consumers                                                                |
| ------------------------- | ------------------------------------- | ----------- | ------------------------------------------------------------------------ |
| `DrawResultIngestedEvent` | `draw_result` créé OU passe à `FINAL` | ❌ (global) | `core.draw` (accélère apply, optionnel — polling reste source de vérité) |

> ⚠️ Pas d'event publié pour `PROVISIONAL` (limite le bruit, tous les consommateurs attendent `FINAL`).
> ✅ Renommé de `DrawResultedAppliedEvent` → `DrawResultIngestedEvent`.

### Events consommés

> Aucun. Le domaine ne réagit qu'aux schedulers et commandes ops.

**Spec compliance** : DR4 (events) + `event_model.md`.

---

## 6. Règles métier non négociables

### 6.1 Idempotence du fetch

- Contrainte SQL `UNIQUE(result_slot_id, occurred_at) WHERE deleted_at IS NULL`.
- Cooldown applicatif par slot (par défaut 10 min) : évite de marteler le provider.
- Cache provider raw (TTL 5-15 min, géré par `core.uslottery`) : évite les appels HTTP redondants.

### 6.2 Calcul de `occurred_at`

- Toujours via `OccurredAtResolver` (cf. `timezone.md`).
- Utilise la **timezone du slot**, jamais celle du tenant.
- Composé : `(date + slot.draw_time + slot.timezone).toInstant()`.
- ✅ Suppression des `LocalTime.of(...)` hardcodés.

### 6.3 Statut `PROVISIONAL → FINAL`

- Un fetch upsert `draw_result` :
  - Si `quality = COMPLETE` → `status = FINAL`.
  - Sinon → `status = PROVISIONAL`.
- Un fetch suivant peut promouvoir un `PROVISIONAL` en `FINAL` (si nouvelle qualité OK).
- Un override ops peut forcer `FINAL`.

### 6.4 Override

- Modifie `source_result`, `haiti_result`, `status`, `override_reason`.
- ✅ **L'override est refusé** si **au moins un** `Draw` lié est `SETTLED` (vérification via `DrawReaderPort` de `core.draw`).
- ✅ Audité systématiquement via `@AuditedForceCommand`.

### 6.5 Saisie manuelle

- Crée un `draw_result` avec `source = MANUAL`, `quality = COMPLETE` (donc directement `FINAL`).
- `recordedBy` et `notes` obligatoires.
- Audité.

**Spec compliance** : DR5 (business rules).

---

## 7. Schedulers

| Scheduler                           | Cron                                              | Action                                                          |
| ----------------------------------- | ------------------------------------------------- | --------------------------------------------------------------- |
| `ExternalResultsFetchTickScheduler` | `${tch.draw.results.shared.scheduler.tick_cron}`  | Pour chaque slot dû, envoie `FetchExternalResultsWindowCommand` |
| `ExternalResultsApplyTickScheduler` | `${tch.draw.results.shared.scheduler.apply_cron}` | Applique les résultats aux draws tenant-scoped                  |

**Filtres du fetch tick** :

- Slot actif (`active = true`).
- Slot a `timezone` et `draw_time` non null.
- `now ∈ [drawTime + min_minutes_after_draw, drawTime + max_minutes_after_draw]` (timezone du slot).
- Cooldown respecté (par défaut 10 min).
- TODO P2 : au moins un `draw_channel` actif chez un tenant pointe sur ce slot (économie HTTP).

**Spec compliance** : DR6 (scheduling).

---

## 8. Cache

| Cache name                                                    | TTL      | Éviction                  |
| ------------------------------------------------------------- | -------- | ------------------------- |
| `infra.uslottery.provider_raw::{provider}_{date}_{queryHash}` | 5-15 min | géré par `core.uslottery` |

> ⚠️ `draw_result` lui-même n'est pas caché (lecture rare, pas de hot path). Le cache vit côté `core.uslottery` au niveau payload provider raw.

**Spec compliance** : DR7 + `cache.md`.

---

## 9. Persistence

- Table `draw_result` — `BaseEntity` (global, pas de tenant_id).
- Pas de RLS (sécurité par scope HTTP : `Platform` / `Ops`).
- Indexes :
  - `uq_draw_result_slot_time` (UNIQUE)
  - `ix_draw_result_slot_time` (recherche par slot+date)
  - `ix_draw_result_status` (pour batch FINAL/ERROR)
- Contrainte CHECK : `haiti_result` contient `lot1..lot4`.
- Audit : Envers (TODO P3).

Voir `jpa_entities.md`, `persistence.md`.

---

## 10. Dépendances

**Dépend de** :

- `common.types.id` (`DrawResultId`, `ResultSlotId`)
- `common.types.BaseEntity`
- `common.bus` (`CommandBus`, `QueryBus`)
- `common.tx.AfterCommit`
- `common.event.DomainEventPublisher`
- `common.contracts.haiti.HaitiProjectionOutput`
- `common.contracts.results.*` (ExternalResultOutput, SourceFlags, etc.)
- `catalog.resultslot.ResultSlotCatalog` (lecture des slots)
- `core.uslottery.ExternalResultsFetchPort` (port consommé)
- `core.haiti.HaitiLotteryPort` (port consommé)
- `core.draw.api.DrawReaderPort` (port de lecture simple vers draw)

**Utilisé par** :

- `core.draw` (consomme `DrawResultReaderPort`)
- `features.publicdraw` (exposition publique des résultats)
- `features.ops` (orchestration manuelle)

---

## 11. Notes techniques

### Pourquoi `core.drawresult` est en `core/` et pas en `catalog/`

Question légitime puisque `draw_result` est read-mostly et global.

**Raisons** :

1. Il a un **scheduler actif** (`ExternalResultsFetchTickScheduler`) qui ingère du monde extérieur — un catalog est passif.
2. Il a un **lifecycle** (`PROVISIONAL → FINAL → ERROR`) — un catalog n'a pas d'états.
3. Il **publie des events** (`DrawResultIngestedEvent`) — un catalog n'en publie jamais (cf. `event_model.md` § 8).
4. Il a une **logique métier d'ingestion** (orchestration fetch + projection + upsert + transition statut) — un catalog n'a que du CRUD admin.

**Conclusion** : c'est borderline mais le verdict est **`core/`**. Si à terme le scheduler disparaît et que le domaine devient passif, il pourra migrer en `catalog/`.

### Typed IDs (`typed_ids.md`)

- ✅ `DrawResultId`, `ResultSlotId` wrappers.
- ✅ Aucune fuite UUID brut hors persistence.

### Mapping (`api_response.md`)

- ✅ `DrawResultView` (port) et `DrawResultResponse` (web) sont des records immuables.
- ✅ Controllers migrés vers `ApiResponse<T>`.

### Transactions

- ✅ Tous les command handlers `@TchTx`.
- ✅ Events publiés via `AfterCommit.run(...)`.

---

## 12. Séparation des responsabilités

### `core.drawresult` (ingestion globale)

✅ Ingère, projette, persiste.
✅ Expose une API stable de lecture.
✅ Publie un seul event.
✅ Aucun tenant.

### `core.uslottery` (clients providers)

✅ Parle aux APIs HTTP (NY/FL/GA/TX/TN).
✅ Cache provider raw.
✅ Fournit `ExternalResultsFetchPort`.
✅ Aucune persistance.

### `core.haiti` (projection)

✅ Projecteur pur (lit `slot.projection_cfg`, applique sur `ExternalPick`).
✅ Fournit `HaitiLotteryPort`.

### `core.draw` (lifecycle tenant)

✅ Consomme `DrawResultReaderPort` (read-only).
✅ Réagit éventuellement à `DrawResultIngestedEvent`.

---

## 13. Patterns appliqués

- ✅ **CQRS** (`command_query_handlers.md`)
- ✅ **Typed IDs** (`typed_ids.md`)
- ✅ **API Response** (`api_response.md`)
- ✅ **Cache Strategy** (`cache.md`) — cache au niveau payload provider raw
- ✅ **Event Model after-commit** (`event_model.md`)
- ✅ **Idempotence par contrainte SQL** (`idempotency.md`)

---

## 14. Conformité aux specs

| Spec         | Requirement      | Status                              |
| ------------ | ---------------- | ----------------------------------- |
| DR1          | Write commands   | ✅ Fetch, Refresh, Override, Manual |
| DR2          | Read queries     | ✅ List + à compléter (P1)          |
| DR3          | Admin endpoints  | ✅ Migré vers `ApiResponse<T>`      |
| DR4          | Events           | ✅ Renommé et conforme              |
| DR5          | Business rules   | ✅ Override post-SETTLED implémenté |
| DR6          | Schedulers       | ✅                                  |
| DR7          | Cache            | ✅                                  |
| event_model  | After-commit     | ✅                                  |
| api_response | `ApiResponse<T>` | ✅                                  |

---

## 15. TODOs spécifiques au domaine

### P1 — Conformité conventions

- [x] Renommer `DrawResultedAppliedEvent` → `DrawResultIngestedEvent`.
- [x] Migrer `DrawResultsController` vers `ApiResponse<T>`.
- [ ] `OccurredAtResolver` partout (supprimer `LocalTime.of(14,30)` hardcoded dans `core.uslottery` — couplage à corriger).
- [x] Créer `BatchJobKey.RESULTS_EXTERNAL_OVERRIDE` et `RESULTS_EXTERNAL_MANUAL` (au lieu de réutiliser `_REFRESH`).
- [ ] Migrer toutes les exceptions vers `gate.assertEnabledOrThrow(...)` (au lieu de `IllegalStateException`).

### P2 — Architecture

- [x] `OverrideDrawResultCommand` refuse si Draw lié SETTLED.
- [ ] Filtrer fetch tick sur slots ayant au moins un channel tenant actif.
- [ ] Cleanup `core.uslottery` : virer `games[]` de la YAML (DB seule source de vérité), provider clients ne calculent plus `occurredAt`, ne connaissent plus les `channelCode`.

### P3 — Observabilité

- [ ] Activer Envers pour audit complet.
- [ ] Convention sur `draw_result` archivage (>2 ans ?).

---

> **Source of truth** : ce document est la référence backend pour le domaine `core.drawresult`.
> Pour la vue cross-apps (Web/Mobile/API), voir `tchalanet-docs/docs/02-functional/domains/drawresult.md`.
> Pour le pipeline complet inter-domaines, voir `tchalanet-docs/docs/02-functional/flows/draw-execution.md`.

## 16. Projection par slot

La projection Haïti est résolue par `HaitiProjectionConfigPort.resolve(slot.projectionCfg())`.
Ordre de priorité :

1. `result_slot.projection_cfg.rules` si complet et valide (`lot1..lot4`).
2. Configuration globale par défaut si la config slot est absente ou invalide.

Cette règle s'applique au fetch provider et à la saisie manuelle ops.
