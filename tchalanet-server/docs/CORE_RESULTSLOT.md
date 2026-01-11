# core.resultslot — Catalogue global des slots

Version: 2026-01-11

Ce document décrit le rôle, l'API et les conventions autour du catalogue global des "slots" (ex: `NY_MID`, `FL_EVE`, ...).

## 1) Rôle

Source de vérité globale des « slots » utilisés par :
- `drawresult` pour l'ingestion (slot-first)
- `features.publicdraw` pour afficher timezone / drawTime / label
- `draw` indirectement via `draw_channel.slotKey` (pas de dépendance directe requise)

Le catalogue expose une vue (read-only) des slots et sert de façade stable aux autres modules.

## 2) Données exposées (View)

Recommandation minimale pour `ResultSlotView` :
- `slotKey` (String, unique) — ex: "NY_MID"
- `provider` (String) — ex: NY / FL / GA / TX
- `timezone` (ZoneId)
- `drawTime` (LocalTime)
- `daysOfWeek` (liste canonique : MON,TUE, ...)
- `active` (boolean)
- `labelKey` (String, optionnel mais recommandé) — ex: `slot.ny_mid.label`

Le view est destiné à être léger et cache-friendly.

## 3) Public API interne (ce que les autres modules doivent utiliser)

### Option A (recommandée) : `ResultSlotCatalog` (façade)
Exposer uniquement une façade publique et stable dans `com.tchalanet.server.core.resultslot.api` :

- `List<ResultSlotView> listActive()`
- `Optional<ResultSlotView> getByKey(String slotKey)`
- `ResultSlotView requireByKey(String slotKey)` (optionnel, jette si non trouvé)

Implémentation : le catalogue encapsule soit :
- l'envoi d'une Query via `QueryBus` (ex: `ListActiveResultSlotsQuery`) ou
- un `ResultSlotReaderPort` local au module, mais **uniquement** utilisé à l'intérieur du module.

**Règle importante** : les autres modules ne doivent JAMAIS dépendre de `ResultSlotReaderPort` (ports internes non exportés).

## 4) Write API (qui peut écrire ?)

Les slots sont un référentiel admin : les écritures doivent passer par le bus de commandes (CommandBus). Exemple de commandes :
- `CreateResultSlotCommand`
- `UpdateResultSlotCommand`
- `ActivateResultSlotCommand` / `DeactivateResultSlotCommand`

Handlers correspondants dans `core.resultslot.application.command.handler`.

Les contrôleurs d'administration (exposition HTTP pour gérer les slots) peuvent être placés dans un module `admin` ou `platform/ops` (selon la convention du projet).

## 5) Ports (internal only)

Ports internes (à garder non exposés aux autres modules) :
- `application.port.out.ResultSlotReaderPort`
- `application.port.out.ResultSlotWriterPort` (si besoin pour operations server-side)

Ces ports sont utilisés par les adaptateurs infra (JPA/JDBC) et par la couche application interne ; **ils ne doivent pas être importés par des modules features/**.

## 6) Comment empêcher l'usage par d'autres modules (sans usine à gaz)

Objectif : protection à la compilation pour éviter les dépendances indésirables.

Choisissez l'une des stratégies ci‑dessous :

### Stratégie 1 — "package-private + façade publique"
- Organisation conseillée :
  - `com.tchalanet.server.core.resultslot.api` : contient `ResultSlotCatalog`, DTO `ResultSlotView` et API publique
  - `com.tchalanet.server.core.resultslot.internal.port.out` : interfaces ports (reader/writer)
  - `com.tchalanet.server.core.resultslot.internal.infra` : adapters (JPA/JDBC)

Les autres modules n'importeront que `com.tchalanet.server.core.resultslot.api`.

> Note : Java ne permet pas des interfaces package-private si on veut les injecter via Spring depuis d'autres packages. L'approche ici est organisationnelle : placer les ports/adapters sous `.internal` et documenter la règle d'usage.

### Stratégie 2 — "ArchUnit guard"
- Garder les ports publics mais ajouter une règle ArchUnit dans le build qui interdit aux packages `features.*` et `core.draw*` d'importer `core.resultslot.internal` ou `core.resultslot.application.port`.
- Avantage : enforcement à la compilation (ou CI) sans refactor massif.

Recommandation : utiliser la Stratégie 2 pour une protection robuste avec peu d'effort, combinée à la Stratégie 1 pour la clarté du code.

## 7) Cache

Clés & TTL recommandés :
- `resultslot.active_list` — TTL ~20h (slots changent rarement)
- `resultslot.by_key::{slotKey}` — TTL ~20h

Éviction :
- Lors d'opérations d'écriture (create/update/activate/deactivate) dans les handlers de commandes :
  - `evict resultslot.active_list`
  - `evict resultslot.by_key::{slotKey}`
  - éventuellement evict des caches downstream dérivés : `drawresult.latest` / `publicdraw.latest` si vous avez des caches.

## 8) Exemple d'implémentation (contrainte et flux)

- Les modules consommateurs (ex: `features.publicdraw`) appellent uniquement `ResultSlotCatalog.listActive()` ou `getByKey()`.
- `drawresult` utilise le catalogue pour la résolution slot-first lors de l'ingestion (le writer `drawresult` ne doit pas dépendre des ports internes de `resultslot`).

## 9) Remarques opérationnelles

- `ResultSlotView.labelKey` est recommandé : côté frontend, on traduit via la clé stable. Fournir aussi un label formaté (ex: `channelLabel`) peut accélérer le rendu mais gardez labelKey pour la stabilité.
- TTL long pour les slots est acceptable (20h) ; invalidation via commandes suffit.

