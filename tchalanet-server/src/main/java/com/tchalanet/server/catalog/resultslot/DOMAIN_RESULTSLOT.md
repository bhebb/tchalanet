# Domaine Catalog ResultSlot

> Référentiel global des « slots » de résultats (identifiants de créneaux: jour/heure/clé). Peu de logique métier; sert de lookup stable pour planification et ingestion des résultats.

---

## 1. Rôle du domaine

**Responsabilité principale**

> Maintenir le catalogue des créneaux de résultats (slots) disponibles sur la plateforme (globaux), pour référencer les tirages et leurs publications.

**Ce que le domaine fait**

- Référence des slots (clé, libellé, jour/heure, fuseau).
- Expose un lookup stable pour les autres domaines (draws, ingestion, reporting).
- CRUD admin (platform) éventuel pour gestion du catalogue.

**Ce que le domaine ne fait pas**

- Ne planifie pas les tirages (core.draw).
- Ne publie pas les résultats (core.draw / catalog.drawresult).
- Ne calcule pas d’agrégats financiers.

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `ResultSlot` — slot global (id, key, label, weekday, time, timezone, status).

### Invariants métier

- Les clés de slot doivent être uniques globalement.
- Un slot ne contient pas les résultats; il référence des tirages/resultsets via d’autres domaines.

> Valeur métier clé :
> Servir d’identifiant stable pour associer et retrouver des résultats publiés.

---

## 2bis. Vue exposée (ResultSlotView)

Recommandation minimale pour `ResultSlotView` (façade/DTO):

- `slotKey` (String, unique) — ex: "NY_MID"
- `provider` (String) — ex: NY / FL / GA / TX
- `timezone` (ZoneId)
- `drawTime` (LocalTime)
- `daysOfWeek` (liste canonique : MON,TUE,...)
- `active` (boolean)
- `labelKey` (String, optionnel mais recommandé) — ex: `slot.ny_mid.label`

Objectif: view léger et cache-friendly.

---

## 3. API publique (façade)

Façade recommandée `ResultSlotCatalog` (`com.tchalanet.server.core.resultslot.api`):

- `List<ResultSlotView> listActive()`
- `Optional<ResultSlotView> getByKey(String slotKey)`
- `ResultSlotView requireByKey(String slotKey)` (optionnel: jette si non trouvé)

Implémentation interne:

- via `QueryBus` (`ListActiveResultSlotsQuery`) OU
- via un `ResultSlotReaderPort` local, non exporté hors module.

Règle: les autres modules ne doivent JAMAIS dépendre de `ResultSlotReaderPort`.

---

## 4. Write API (CommandBus)

Slots = référentiel admin; écritures via commandes:

- `CreateResultSlotCommand`
- `UpdateResultSlotCommand`
- `ActivateResultSlotCommand` / `DeactivateResultSlotCommand`

Handlers correspondants: `core.resultslot.application.command.handler`.

Controllers d’administration: sous `platform`/`admin` selon convention.

---

## 5. Ports (internal only)

- `application.port.out.ResultSlotReaderPort`
- `application.port.out.ResultSlotWriterPort`

Utilisés par adaptateurs infra (JPA/JDBC) et application interne; **non importés** par features/ autres domaines.

---

## 6. Stratégies d’isolation (usage contrôlé)

- Stratégie 1: packages `api` vs `.internal` (ports/adapters).
- Stratégie 2: règle ArchUnit qui interdit `features.*`/`core.draw*` d’importer `.internal` ou `application.port`.

Recommandation: combiner 2 (enforcement) + 1 (clarté).

---

## 7. Cache (clés/TTL/éviction)

Clés & TTL recommandés:

- `resultslot.active_list` — TTL ~20h
- `resultslot.by_key::{slotKey}` — TTL ~20h

Éviction lors des writes (handlers commandes):

- evict `resultslot.active_list`
- evict `resultslot.by_key::{slotKey}`
- éviction éventuelle des caches downstream (`drawresult.latest`, `publicdraw.latest`).

---

## 8. Exemple de consommation

- `features.publicdraw`: utilise uniquement `ResultSlotCatalog.listActive()` / `getByKey()`.
- `catalog.drawresult`: slot-first ingestion; writer ne dépend pas des ports internes de `resultslot`.

---

## 9. Notes opérationnelles

- `ResultSlotView.labelKey` recommandé (stabilité i18n côté frontend).
- TTL long acceptable (20h); invalidation via commandes suffit.

---

## 10. Mapping & DTOs (convention)

- MapStruct pour mapper `infra.persistence.entity` ↔ projections/DTO `ResultSlotResponse`.
- IDs: wrappers côté web/domain; UUID en JPA.

---

## 11. Notes techniques

- Table globale sans `tenant_id` — BaseEntity.
- RLS non applicable (global); sécurité via scope Platform/Admin.
- Exposition possible via SDR sous `/_sdr/resultslots` (admin only).
