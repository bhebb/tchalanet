# core.draw — Agrégat Draw (tenant-scoped)

Version: 2026-01-11

Ce document décrit le rôle, l'API interne recommandée, les relations avec `draw_result`, la séparation des responsabilités Fetch/Apply/Refresh, la recommandation pour le `NextDrawCalculator` et la stratégie de cache pour l'agrégat `draw`.

1) Purpose

- Agrégat tenant-scoped responsable de :
  - la planification des tirages (scheduledAt / cutoffAt),
  - l'ouverture/fermeture automatique (open/close) selon la schedule et les règles métier,
  - l'attache d'un résultat global (`draw_result`) (référence via FK),
  - le settlement (règlement, transition d'état, actions post-draw).

- Contrainte : la logique tenant-scoped (états, transitions) vit dans `core.draw` ; elle référence les résultats globaux mais ne doit pas posséder la source canonique des résultats.

2) Public API interne (pour autres modules)

- Les autres modules n'ont généralement pas besoin d'interagir avec `draw` (sauf ops/admin). Exposer uniquement via bus :

  - QueryBus (lecture) :
    - `GetNextDrawQuery`  -> retourne le prochain `Draw` (ou résumé) pour un tenant/channel
    - `ListDrawsQuery`    -> recherche/pagination des draws tenant-scoped
    - `GetDrawQuery`      -> détails d'un draw (incluant référence au `draw_result` si présent)

  - CommandBus (écriture / opérations de transition) :
    - `OpenDueDrawsCommand`   -> ouvre les draws qui deviennent éligibles (cron/scheduler)
    - `CloseDueDrawsCommand`  -> ferme les draws arrivés à cutoff
    - `SettleDrawsCommand`    -> opère le settlement post-draw
    - `ApplyFetchedResultToDrawCommand` (ou `ApplyExternalResultsWindowCommand` délégué) -> attache un `draw_result` (id) à un `draw` tenant, provoque transitions d'état nécessaires

- Règle pratique : `RefreshExternalResultsWindowCommand` (orchestrateur de fetch) peut rester dans `core.drawresult` mais il DOIT déclencher `ApplyFetchedResultToDrawCommand` via `CommandBus` pour lier les résultats aux draws (découplage clair).

3) FK & relation aux résultats

- Schéma conceptuel : `draw.draw_result_id` (FK -> `draw_result.id` global).
- `draw` ne possède pas les données du résultat — il référence le résultat canonique stocké globalement (dans core.drawresult).
- Conséquences :
  - Lors de l'attachement d'un résultat, on met à jour `draw.draw_result_id` et on déclenche les transitions d'état (ex: OPEN -> CLOSED -> SETTLED selon règles).
  - Les readers de `draw` peuvent joindre les métadonnées du `draw_result` via `DrawResultCatalog` si besoin pour affichage, mais l'écriture de `draw_result` reste dans `core.drawresult`.

4) Où placer Apply / Fetch / Refresh ? (séparation des responsabilités)

- Règles :
  - Fetch/Override/Manual/Refresh(fetch) → appartiennent à `core.drawresult` (ils récupèrent/normalisent des résultats externes et écrivent le canon global `draw_result`).
  - Apply…ToDraw (attacher FK + transitions métier) → appartient à `core.draw` (c'est le comportement tenant-scoped qui applique un résultat global au draw local).

- Pattern recommandé :
  - `core.drawresult` expose un orchestrateur `RefreshExternalResultsWindowCommand` qui :
    1. fetch les résultats externes, upsert en `draw_result`,
    2. publie des commandes/events (ex: `ApplyFetchedResultToDrawCommand` ou émet `DrawResultCreated` events) ; pour lier au draw tenant-scoped, il envoie une commande sur `CommandBus` vers `core.draw`.
  - `core.draw` exécute `ApplyFetchedResultToDrawCommand` en :
    - validant que le draw existe et est eligible,
    - attachant `draw_result_id` au `draw` (mise à jour de l'entité),
    - effectuant les transitions d'état (open/close/settle) et les side-effects associées (events, notifications).

5) NextDrawCalculator

- Contexte : le calcul du prochain draw est basé principalement sur la configuration du slot (timezone, drawTime, daysOfWeek).
- Recommandation : déplacer le service hors de `core.draw` (qui est tenant-scoped) vers un module plus générique dépendant de `resultslot` :
  - `core.resultslot.application.service.NextDrawCalculator`  ou
  - `core.common.time.NextOccurrenceCalculator`

- Bénéfices :
  - `publicdraw` et `drawresult` peuvent utiliser le calcul de prochaine occurrence sans importer `core.draw`.
  - Réduction des dépendances circulaires et meilleure séparation de responsabilités.

6) Cache `draw` (si présent)

- Objets typiquement cachés : `draw.summary`, `draw.next` (prochain draw par tenant/channel), `draw.list` (si lecture lourde)
- Eviction recommandée :
  - Quand on `apply result` (attachement d'un `draw_result` changeant le statuts + FK) : evict les caches `draw.summary::{tenant,channel}`, `draw.next::{tenant,channel}`
  - Sur opérations planification : `OpenDueDrawsCommand`, `CloseDueDrawsCommand`, `SettleDrawsCommand`, `RescheduleDrawCommand` : evict les caches affectés

- Pattern d'implémentation :
  - utilise `@Cacheable` sur les queries (ex: `GetNextDrawQuery` handler) et `@CacheEvict` sur les handlers de commandes qui modifient l'état des draws.
  - pour orchestration cross-module : si `core.drawresult` déclenche `ApplyFetchedResultToDrawCommand`, c'est le handler dans `core.draw` qui doit réaliser l'`@CacheEvict`.

