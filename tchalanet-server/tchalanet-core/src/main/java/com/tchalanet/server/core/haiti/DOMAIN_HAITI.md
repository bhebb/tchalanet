> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/draw.md` (haiti projection cross-domain)

# Domaine Haïti — projection loterie & Tchala (documentation développeur)

But: fournir aux développeurs une vue claire du bounded context `core.haiti` — responsabilités, contrats, packages, conventions de code, points d'extension et workflows opérationnels (projection de résultats externes → résultats Haïtiens, gestion du catalogue Tchala).

Langue: français.

---

## 1. Vision & responsabilité

Le domaine Haïti couvre deux sous-domaines étroitement liés mais séparés logiquement :

- La projection loto ("lottery projection") : conversion déterministe des résultats externes (pick3/pick4 de NY/FL/GA/TN etc.) en lots haïtiens (lot1..lot4) suivant une configuration par channel (`draw_channel.flags.projection`).
- Le catalogue Tchala (dream-to-number mapping) : collection de rêves/mots associés à des numéros (0..99) géré en lecture publique et modération (suggestion publique → approbation superadmin / import / merge / dedup).

Contraintes de conception importantes

- Domain pur : pas d'import Spring/JPA/Jackson dans `core.haiti.domain.*`.
- Hexagonal / CQRS : commands/queries = records dans `application.*`, handlers = classes implémentant `CommandHandler`/`QueryHandler`.
- Infra (Jackson/IO/DB/Spring) uniquement dans `core.haiti.infra.*`.
- Tests rapides & isolés : domain purement testable sans Spring.

---

## 2. Organisation des packages (convention actuelle)

- core.haiti.domain

  - lottery (projection métier)
    - model: `HaitiLot`, `HaitiProjectionToken`, `ExternalPick`, `HaitiProjectionConfig`, `HaitiResult`
    - service: `HaitiResultProjector` (interface) + `DefaultHaitiResultProjector` (impl. pure)
    - exception: domain exceptions spécifiques (ex: `InvalidExternalPickException`)
  - tchala (catalogue)
    - model: `TchalaEntry`, `TchalaNumber`, `TchalaLang`, `DreamText`, `DedupeKey`, `TchalaEntryId`, enums (status, source)
    - exception: `InvalidTchalaNumberException`, `TchalaEntryNotFoundException` (métier)

- core.haiti.application

  - command.model / command.handler
  - query.model / query.handler
  - port.out: persistance / import / projection adapter ports (ex: `TchalaEntryRepositoryPort`, `HaitiLotteryPort`, `TchalaImportSourcePort`)
  - util: aides applicatives (parsing CSV rows, number parsing)

- core.haiti.infra

  - adapter: implémentations des ports (JPA adapters, in-memory adapters, file readers)
  - web: controllers / dto (API public / admin)
  - converter: Spring converters (String -> TchalaEntryId)
  - persistence: jpa entities & spring repositories (adapter level)

- docs/: documentation et guides (ce fichier)

---

## 3. Concepts clés (résumé rapide)

- Projection engine

  - Input: `ExternalPick` (pick3, pick4) normalisé.
  - Config: `HaitiProjectionConfig` (Map<HaitiLot, HaitiProjectionToken>) — extrait de `draw_channel.flags.projection` en infra (app adapter).
  - Output: `HaitiResult` (Map<HaitiLot,String>) — version immuable stockée dans `draw_result.haiti_result`.
  - Interface: `HaitiResultProjector.project(config, pick)`.

- Tchala lifecycle
  - Suggestion publique → `SubmitTchalaSuggestionCommand` → saved as PENDING.
  - Superadmin approve/merge/reject/import via commands.
  - Canonical: une seule entrée APPROVED par (lang, dedupeKey) — garanti par repo (index unique) et logique métier.
  - Numbers lookup performant via table `tchala_entry_number` (indexed by lang+number).

---

## 4. Contrats et types importants

- ExternalPick

  - factory `ExternalPick.of(pick3, pick4)` : normalise (trim, digits only), valide longueurs (pick3 3 digits, pick4 4 digits) ou lève `InvalidExternalPickException`.

- HaitiProjectionConfig

  - record `HaitiProjectionConfig(Map<HaitiLot,HaitiProjectionToken> tokens)`
  - validation: contient tous les HaitiLot (LOT1..LOT4). Usage : la lecture/applicative parse `draw_channel.flags.projection` en `HaitiProjectionConfig` dans `core.haiti.infra` (adapter qui lit JSON).

- HaitiProjectionToken (enum)

  - tokens supportés : PICK3_FULL_3, PICK3_FIRST2, PICK3_LAST2, PICK4_FULL_4, PICK4_FIRST2, PICK4_LAST2
  - comportement déterministe : implémenté dans `DefaultHaitiResultProjector`.

- HaitiResult

  - record immuable Map<HaitiLot,String> values; garantis non nulls.

- Ports (exemplaires)

  - `HaitiLotteryPort` (application.port.out) : `HaitiResult projectResult(ExternalPick, HaitiProjectionConfig)` — adapter infra (`HaitiLotteryAdapter`) expose le projector.
  - `TchalaEntryRepositoryPort` : CRUD/queries tenant-aware (liste pending, search approved, find by number, find canonical by key, save, delete)
  - `TchalaImportSourcePort` : read rows from CSV/FS/object store — infra-only parsing.

- Exceptions
  - `com.tchalanet.server.common.error.NotFoundException` (générique) extends `EntityNotFoundException`.
  - `TchalaEntryNotFoundException` extends `NotFoundException` pour les flows Tchala.

---

## 5. Règles / conventions à respecter

- Domain purity : aucune dépendance Spring/Jackson/DB dans `core.haiti.domain.*`.
- Ports : toute interaction IO doit transiter par un port (`application.port.out`) et une adapter `infra`.
- Records : commands & queries doivent être `record` (Java 25) pour clarté et immutabilité.
- Exceptions : utiliser exceptions métier spécifiques (ex: `TchalaEntryNotFoundException`) pour améliorer le traçage et mapping en ProblemDetail.
- DTOs web : placer dans `core.haiti.infra.web.model` et nommer `*Request` / `*Response`.
- Conserver les JSON `version` fields pour `haiti_result` afin de permettre des évolutions incrémentales.

---

## 6. Exemple d'intégration (comment utiliser la projection)

1. Un adapter d'ingestion (fetcher) obtient un payload externe et normalise :
   - construit `ExternalPick.of(pick3, pick4)` (ou adapte pour provider si missing fields).
2. L'adapter lit la configuration de projection (depuis `draw_channel.flags`) et la transforme en `HaitiProjectionConfig` via `HaitiProjectionConfigReader` (infra).
3. Appelle le port `HaitiLotteryPort.projectResult(externalPick, config)`.
4. Reçoit `HaitiResult` et sérialise dans `draw_result.haiti_result` (JSON versionné) via `DrawResultWriterPort` (persist via JDBC upsert).

---

## 7. Tchala — guide rapide pour devs

- Pour ajouter une nouvelle suggestion publique : utiliser `SubmitTchalaSuggestionCommand` (application/command.model) — parsing des `numbers` se fait dans handler.
- Pour approuver/merge : utiliser `ApproveTchalaEntryCommand` / `MergeTchalaEntriesCommand`.
- Pour importer CSV : fournir `TchalaImportSourcePort` implementée en infra (ex: `FileSystemCsvTchalaImportSourceAdapter`) injectée dans `ImportTchalaEntriesCommandHandler`.
- Index SQL : `tchala_entry_number(lang, number)` est la clé pour recherche par numéro.

Best practice pour merge

- Utiliser `TchalaMerge.mergeNumbers` (policy-driven) — n’écrase pas le canonical sans décision explicite.
- Marquer l’entrée en MERGED (pending → merged) et enregistrer `canonical_entry_id`.

---

## 8. Tests recommandés

- Domain tests (unit) :

  - `DefaultHaitiResultProjectorTest` → token-by-token validations et erreurs de longueur.
  - `ExternalPick.of` validations.
  - `TchalaNumber` boundaries, `DreamText.normalizeForKey`.

- Application tests :

  - Handler tests : `SubmitTchalaSuggestionCommandHandlerTest`, `ApproveTchalaEntryCommandHandlerTest` (utiliser repo in-memory adapter).

- Infra tests (integration) :
  - Repository adapters (JPA) avec Testcontainers Postgres pour vérifier indexes et requêtes paginées.
  - Upsert draw_result path (via `DrawResultJdbcRepository`) avec scénarios idempotence.

---

## 9. Checklist d'ajout d'une nouvelle règle de projection

1. Ajouter le token dans `HaitiProjectionToken` (enum) — implémenter la logique dans `DefaultHaitiResultProjector`.
2. Mettre à jour la doc `docs/DOMAIN_HAITI.md` (cette page).
3. Si config par channel attend ce token, ajouter parsing dans `HaitiProjectionConfigReader` (infra).
4. Écrire tests unitaires pour le token (domain tests).
5. Déployer en staging puis exécuter fetcher local pour vérifier comportement en situation réelle.

---

## 10. Où regarder en priorité (fichiers utiles)

- `src/main/java/com/tchalanet/server/core/haiti/domain/lottery/*` — model + projector.
- `src/main/java/com/tchalanet/server/core/haiti/application/port/out/*` — ports (HaitiLotteryPort, TchalaEntryRepositoryPort).
- `src/main/java/com/tchalanet/server/core/haiti/infra/adapter/*` — adapters concrètes (JPA, CSV, converters).
- `src/main/java/com/tchalanet/server/core/haiti/infra/web/*` — controllers + DTOs.
- `docs/draw-handlers.md` — relation cross-domain draw ↔ haiti (utile pour projection / draw_result lifecycle).

---

## 11. FAQ rapide

Q: Où parser `draw_channel.flags` en `HaitiProjectionConfig` ?
A: dans une adapter infra dédiée `HaitiProjectionConfigReader` (dans `core.haiti.infra.adapter`) — jamais dans domain.

Q: Le projector peut-il lancer une exception si un champ manquant ?
A: Oui, `DefaultHaitiResultProjector` doit échouer fast-fail (IllegalArgumentException / domain exception) si un token nécessite une donnée absente.

Q: Peut-on override un résultat après settlement ?
A: Non dans le MVP — override après SETTLED nécessite workflow comptable (reversal). Les handlers ops refusent override après settlement.

---

## 12. Contacts et suivi

- Auteur / contact : équipe backend Tchalanet (consulter le repo pour auteurs des fichiers récents).
- Pour une demande de changement de modèle (ajout d'un token, champs JSON) : ouvrir une issue et assigner aux reviewers backend + ops.

---

## 13. Analysis V1 (2026-05-05) — Flow Validation

### Sub-domain Tchala

✅ **Import Tchala Entries flow**:

- Bulk load from CSV/JSON
- Validate dream numbers (range, format)
- Create TchalaEntry per entry with PENDING_APPROVAL status
- Persist via TchalaWriterPort
- Return import summary (created, errors)
- No events published (internal state)

✅ **Approve/Reject flow**:

- Load entry (PENDING_APPROVAL)
- Validate entry
- Transition to APPROVED or REJECTED
- Persist
- No events published

✅ **Query flows**:

- GetTchalaByNumber(number) → TchalaEntryView
- SearchTchala(dreamText) → List<TchalaEntryView>
- Both RLS-scoped per tenant, indexed lookups

### Sub-domain Lottery

✅ **Haiti Projection Service**:

- Pure function: `project(resultSlot, date, externalResult) → HaitiResult`
- Normalize external numbers to haiti space
- Apply dream-to-number mapping
- Validate against schema
- No persistence, no I/O, deterministic

✅ **Integration**:

- Called by FetchExternalResultsWindowCommandHandler (step 4)
- Input: ExternalResult (NY/FL/GA/TX normalized numbers)
- Output: HaitiResult (lot1, lot2, lot3, lot4, derivedPairs)
- Used by draw_result.haiti_result persistence

### Architecture Compliance

- ✅ Tenant-scoped (Tchala): RLS by tenant_id
- ✅ Global (Lottery): Shared normalization logic
- ✅ Typed IDs: TenantId, TchalaEntryId
- ✅ Pure functions: HaitiProjectionService no side-effects
- ✅ No events: Reference data lifecycle (silent)
- ✅ Integration: Via ports (HaitiLotteryPort, TchalaEntryRepositoryPort)

### Configuration

- Projection tokens: PICK3_FULL_3, PICK3_FIRST2, PICK3_LAST2, PICK4_FULL_4, PICK4_FIRST2, PICK4_LAST2
- Stored in: draw_channel.flags.projection (JSON config)
- Per-channel customizable

---

# Domaine core.haiti — Particularités pays / conformité

> Encapsule les règles spécifiques au contexte Haïti (timezone, formats, conformité, exceptions légales) qui impactent d’autres domaines.

---

## 1. Rôle du domaine

- Fournir services/utilitaires pour les spécificités Haïti.
- Centraliser règles de conformité locales.

**Ne fait pas**

- Règles globales non spécifiques.

---

## 2. Modèle & invariants

- `HaitiRules`: paramètres (holidays, cutoffs, timezones, formats).
- Invariants:
  - Ne pas disperser ces règles dans d’autres domaines.

---

## 3. Use Cases

- `ResolveLocalCutoffQuery`
- `NormalizeHaitiPhoneCommand`

---

## 4. Ports

- `HaitiConfigRepoPort` (si en DB)

---

## 5. Intégrations

- draw/sales/time pour ajustements.

---

## 6. Notes techniques

- Package commun ou core dédié; wrappers ID.

---

## 7. Incohérences / TODO

- Lister les règles concrètes HA (timezone, cutoffs, formats numéros).

---

Document last updated: 2026-01-07
