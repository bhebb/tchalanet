# Draw — Inventaire des handlers, doublons critiques et plan de consolidation

Ce document centralise l'inventaire des handlers (commands & queries) du domaine `draw`, identifie les doublons critiques (notamment JPA vs JDBC pour `draw_result`) et propose un plan d'actions priorisé, découplé en petits PRs sûrs.

Langue: français.

---

## Objectif
- Inventorier les handlers nécessaires et leur responsabilité canonique.
- Consolider les doublons critiques (écriture `draw_result`, lookups, variants fetch joins). 
- Proposer et prioriser des modifications sûres (petites PRs, tests, rollback) afin d'aboutir à:
  - une seule voie d'écriture pour `draw_result` (upsert atomique),
  - des repos/ports canoniques pour la lecture publique (feed) basés sur `draw` + `draw_channel` + `draw_result`,
  - une surface de handlers plus claire (canoniques vs wrappers/ops).

Contrainte principales:
- Respecter le schéma/colonnes récents (voir sections plus bas).
- Ne pas propager inutilement `tenantId` si la table est protégée par RLS (préserver ports cross-tenant quand nécessaire).
- Java 25: favoriser `record` pour modèles de commandes/queries/projections.

---

## 0) Rappel schéma (champs récents à conserver)
- `draw`: `scheduled_at`, `cutoff_at`, timestamps d'état (opened/closed/resulted/settled), `status`, `locked`, `system_generated`, `draw_result_id`.
- `draw_channel`: `timezone`, `draw_time`, `cutoff_sec`, `days_of_week`, `active`, `sort_order`, `flags` (json), `external_provider`.
- `draw_result` (global): `provider`, `provider_slot`, `occurred_at`, `source_result` jsonb, `haiti_result` jsonb, `status`, `quality`, `source`, `source_hash`, `raw_payload`, `override_reason`, `fetched_at`.
- `draw_channel_game`: association (draw_channel_id, game_id) + `enabled`, `flags`.

---

## 1) Inventaire canonique (handlers essentiels)

A - CommandHandlers canoniques (doivent exister, seuls points d'écriture possibles pour ces responsabilités)
- `GenerateDrawsForRangeCommandHandler` — génération idempotente J..J+n (tenant-scoped, unique (tenant,channel,date)).
- `OpenDueDrawsCommandHandler` — SCHEDULED → OPEN (bulk, lock-safe).
- `CloseDueDrawsCommandHandler` — OPEN → CLOSED (cutoff), lock-safe.
- `UpsertDrawResultCommandHandler` — upsert canonical `draw_result` (provider+slot+occurred_at or mapping via channel/draw_date), produit `INSERT ON CONFLICT ... DO UPDATE` atomique.
- `ApplyExternalResultsForDateCommandHandler` — lie `draw_result` globaux aux `draw` CLOSED (matching via channel/slot/date).
- `AttachResultToDrawCommandHandler` — ops manual attach/override d'un résultat sur un draw donné.
- `SettleDrawsCommandHandler` — RESULTED → SETTLED, orchestration des payouts (idempotent).

B - CommandHandlers wrappers / ingestion / ops (appellent les canoniques)
- `FetchExternalResultsForDateCommandHandler` — fetcher provider → appelle `UpsertDrawResult`.
- `FetchAndApplyExternalResultCommandHandler` — fetch + apply.
- `RecordManualDrawResultCommandHandler` — ops manual → `UpsertDrawResult`.
- `OverrideDrawResultCommandHandler` — ops override (source=ADMIN) et ré-apply.
- `InvalidateDrawResultCommandHandler` — ops invalider (provisoire), relancer apply.
- `RetrySettleDrawCommandHandler`, `ResetSettlementForDrawCommandHandler`, `CancelDrawsCommandHandler` — ops.

C - Admin / corrective (optionnel)
- `CreateDrawCommandHandler`, `UpdateDrawCommandHandler`, `CancelDrawCommandHandler`, `ArchiveDraw...`, `CreateDrawChannelCommandHandler`, `UpdateDrawChannelCommandHandler`, etc.

---

## 2) Inventaire canonique des QueryHandlers (public & internal)

Public feed (surface publique à normaliser)
- `GetLatestPublicDrawResultsQueryHandler` — renvoyer latest per channel (limit param).
- `ListPublicDrawResultsQueryHandler` — page/filtre pour la page publique.
- `GetPublicDrawResultQueryHandler` — détail par channel+draw_date.

Internal / Backoffice
- `GetNextDrawHandler` / `GetNextDrawsHandler` — prochain(s) draw(s) (per channel(s)).
- `ListTodayDrawsHandler`, `ListLastDaysDrawsHandler`, `ListDrawsHandler` — date-range aggregator.
- `GetDrawHandler` — draw detail (incl. enlace draw_result si présent).
- `GetDrawResultHandler`, `ListDrawResultsHandler` — vieux reads, mais reading should use draw->draw_result relationship.
- `ListActiveDrawChannelsHandler`, `GetDrawChannelHandler` — channels + draw_channel_game joins.

---

## 3) Doublons critiques identifiés & recommandations (RAPIDE)
Le rapport initial a identifié plusieurs doublons importants. Ici les décisions recommandées et pourquoi.

### 3.1 Upsert `draw_result` (CRITIQUE)
- Doublon détecté: `DrawResultJpaRepository.upsertResult(...)` (JPA native) vs `DrawResultJdbcRepository.upsertReturnId(...)` (JdbcTemplate upsert + returning id).
- Contexte: action critique (ingestion/results), comportement d'idempotence et règles de quality (COMPLETE / SUSPECT / OVERRIDDEN) implémentées dans l'adapter `DrawResultJpaRepositoryAdapter`.
- Recommandation (priorité HIGH, recommandée):
  - Standardiser sur la voie JDBC atomique (option retenue pour fiabilité / retour d'ID / contrôle fin de WHERE pour éviter overwrites). Rendre la méthode JDBC la source unique d'écriture (single upsert). 
  - Adapter `DrawResultJpaRepositoryAdapter` pour déléguer l'upsert à `DrawResultJdbcRepository.upsertReturnId(...)` (map params), puis charger l'entité via JPA `findByChannelCodeIgnoreCaseAndDrawDate(...)` pour construire l'objet domaine.
  - Mettre `@Deprecated` sur `DrawResultJpaRepository.upsertResult(...)` (commentaire) et ne plus l'appeler.
- Tests: unit + intégration (testcontainer Postgres idéal pour vérifier ON CONFLICT semantics).

### 3.2 Lookup existant léger
- Doublon: `DrawResultJpaRepository.findByChannelCodeIgnoreCaseAndDrawDate` vs `DrawResultJdbcRepository.findExisting`.
- Recommandation: faire `findExisting` wrapper vers `DrawResultJpaRepository.findByChannelCodeIgnoreCaseAndDrawDate` ou inverser selon perf target; l'important est d'avoir un seul SQL/contrat. Idéalement utiliser JPA for read and JDBC for write.

### 3.3 Fetch join variants sur `DrawJpaRepository`
- Doublon: `findByTenantIdAndScheduledAtBetweenFetchChannelOrderByScheduledAt` vs `findByTenantIdAndScheduledAtBetween`.
- Recommandation: garder la variante *fetchChannel* pour la lecture métier (éviter LazyInitializationException dans adapters). Marquer l'autre comme @Deprecated et aliaser si besoin.

### 3.4 `findByStatus...` String vs Enum
- Recommandation: standardiser à l'API d'adapter / port: utiliser `DrawStatus` (enum) dans couche application. Converter à la frontière persistence si le repo a besoin de String.

---

## 4) Plan d'exécution (lots, PRs, tests) — Priorité & étapes concrètes

LOT 0 — Baseline
- Exécuter une compilation complète `mvn -DskipTests package` et lister les warnings. (Baseline avant les changements.)
- Générer rapport d'usages exhaustif (déjà produit partiellement dans `reports/draw_repos_adapters_methods_usage.csv`).

LOT 1 — Upsert consolidation (CRITICAL)
Objectif: une voie d'écriture atomique unique pour `draw_result`.
Étapes détaillées (petit PR):
1. Créer PR `chore/draw/upsert-to-jdbc`.
2. Modifier `DrawResultJpaRepositoryAdapter.save(TenantId, DrawId, DrawResult)` et `upsertFromExternal(...)` pour appeler `DrawResultJdbcRepository.upsertReturnId(...)`. Map params : compute channelCode, drawDate, numbersJson, rawPayloadJson, occurredAt, quality, status, source, sourceHash, force flag.
3. Après `upsertReturnId`, charger l'entité via `DrawResultJpaRepository.findByChannelCodeIgnoreCaseAndDrawDate(...)` et retourner `mapper.toDomain(entity)`.
4. Ajouter tests unitaires pour `DrawResultJpaRepositoryAdapter` (mocks pour jdbc repo + jpa repo) and integration tests using testcontainer Postgres for real upsert semantics.
5. Ajouter `@Deprecated` sur `DrawResultJpaRepository.upsertResult(...)` et ajouter note de migration.
6. CI: run `mvn -DskipTests=false verify` with integration tests target (or run locally with testcontainer).
7. Deploy to staging + smoke tests (fetchers + apply + settle).
8. Monitor logs for NOOP/UPDATED semantics.

Rollback: revert PR.

Documents & tests: include test vectors for COMPLETE vs SUSPECT vs forceUpdateComplete cases.

LOT 2 — Consolidate `findExisting` (HIGH)
- Implementer `DrawResultJdbcRepository.findExisting(...)` as a small wrapper over `DrawResultJpaRepository.findByChannelCodeIgnoreCaseAndDrawDate(...)` (or vice versa). PR petit.
- Tests : unit.

LOT 3 — Deprecate duplicate repo method variants (MEDIUM)
- Grep the usages (script) for each variant; si usages <=1 ou uniquement interne, marquer `@Deprecated` et rediriger callers vers la méthode canonique. PR petits, one-by-one.

LOT 4 — Public feed queries optimisation (MEDIUM)
- Remplacer queries monolithiques qui utilisent `(:param IS NULL OR col = :param)` par 2–3 repo methods orthogonales (ex: `searchAll`, `searchByChannel`, `searchByDateRange`) ou builder dynamic query via CriteriaBuilder.
- Rationale: préserver indexes ; éviter full scan with OR.
- PR: modify `PublicDrawResultRepository` / adapter to expose methods matching patterns required by public handlers.

LOT 5 — Indexes & migrations (LOW)
- Proposer migration SQL `Vxx__draw_query_indexes.sql` avec indexes partiels recommandés.

---

## 5) Ports nouveaux / à ajouter
- `DrawResultWriterPort` (port out) — méthode: `record DrawResultWriteCommand upsert(...) -> DrawResultId`.
  - Implémentation conseillée: `DrawResultJdbcRepositoryAdapter` (JDBC upsert).
  - Keep `DrawResultReaderPort` for reads (JPA adapter ok).
- `PublicDrawFeedPort` (port out) — méthodes: `Page<PublicDrawResultRow> search(...)`, `Optional<PublicDrawResultRow> findOne(channel, date)`, `List<PublicDrawResultRow> latest(limitPerChannel)`.
  - Implémentation: repo adapter qui utilise `draw` join `draw_channel` join `draw_result` for correctness (tenant-scoped draw anchored design).

---

## 6) Tests recommandés (essentiels)
- Unit tests: mapper + adapter (mock repo/jdbc) for `DrawResultJpaRepositoryAdapter` behavior.
- Integration tests: Postgres testcontainer verifying upsert semantics & idempotence:
  - Case 1: first insert SUSPECT -> COMPLETE (force=false) behaviour
  - Case 2: COMPLETE + same hash -> NOOP
  - Case 3: COMPLETE + different hash -> UPDATED
  - Case 4: forceUpdateComplete=true -> overwrite.
- End-to-end smoke in staging for batch `fetch_results` + `apply_results_to_draws` + `settle_draws`.

---

## 7) Migrations SQL recommandées (extraits)
- Index pour `draw_result (provider, provider_slot, occurred_at)` (unique constraint if appropriate)
- Partial indexes for `draw` on `(status, scheduled_at)` and `(status, cutoff_at)` with `deleted_at IS NULL AND locked=false` (if Postgres partial indexes allowed).

(Fournir fichier `db/migration/Vxxx__draw_query_indexes.sql` idempotent `CREATE INDEX IF NOT EXISTS ...`)

---

## 8) Entrées PR concrètes que je peux produire maintenant
- PR A (small): `chore/draw/deprecate-jpa-upsert` — ajoute `@Deprecated` javadoc sur `DrawResultJpaRepository.upsertResult` (safe, compiles). Tests none.
- PR B (medium): `feat/draw/use-jdbc-upsert` — adapter `DrawResultJpaRepositoryAdapter` to call `DrawResultJdbcRepository.upsertReturnId`, plus unit tests; integration test skeleton.
- PR C (small): `chore/draw/replace-findExisting` — change `DrawResultJdbcRepository.findExisting` to delegate to `DrawResultJpaRepository.findByChannelCodeIgnoreCaseAndDrawDate` (or vice versa).
- PR D (medium): `feat/draw/public-feed-repo` — implement `PublicDrawFeedPort` + adapter that reads from `draw` anchor, update handlers to use it.

---

## 9) Script utile à exécuter localement (grep usages)
```bash
# Lister méthodes candidates non-utilisées (ex: handlers or repo methods)
for f in $(ls src/main/java/com/tchalanet/server/core/draw/infra/persistence/repo/*.java); do
  echo "-- $f --"
  sed -n '1,200p' "$f" | grep -E "(public|Optional|List)\s+[A-Za-z0-9_<>]+\s+[a-zA-Z0-9_]+\(" || true
done

# Rapporter usages d'une méthode
# Ex: rechercher usages de upsertReturnId
git grep -n "upsertReturnId(" || true
```

---

## 10) Prochaine action proposée (pick one)
- [ ] Je génère la PR A (déprécier la méthode JPA `upsertResult`) — rapide, safe.
- [ ] Je génère la PR B (implémentation JDBC canonical upsert + tests) — plus lourd, inclut PR A as precursor.
- [ ] Je génère la PR C (consolider findExisting) — petit PR.
- [ ] Je fais l'inventaire complet automatique des méthodes de repo/adapters et génère le rapport exhaustif CSV (pour revue avant modifs).

Dis‑moi quelle action tu veux que je lance maintenant. Si tu choisis une PR, je la créerai localement (fichiers modifiés), j'exécuterai une compilation et les tests rapides, et je fournirai le diff/PR prêt à review.

---

Annexes
- Report initial des méthodes & occurrences: `reports/draw_repos_adapters_methods_usage.csv` (fichier généré automatiquement).
- Notes: tout changement sur le path d'upsert doit être testé en staging avec des échantillons réels (channel mapping) et surveillé (idempotence / quality rules).



