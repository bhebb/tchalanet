## Context

Cette change traite le flow `draw` comme un **pipeline d'exécution cross-domaines**. Le problème principal n'est pas un champ ou une entité isolée, mais la cohérence d'ensemble entre :

- les catalogues (`game`, `resultslot`, `drawchannel`)
- les clients providers (`uslottery`)
- la projection métier (`haiti`)
- l'ingestion globale (`drawresult`)
- l'application tenant-scoped et le settlement (`draw`)
- l'orchestration ops et l'exposition publique

Le choix retenu est **runtime-first** :

1. sécuriser ce qui tourne réellement
2. clarifier les responsabilités
3. remettre la documentation à niveau

## Findings

### F1 — Imports YAML incohérents

`application.yaml` importe :

- `application-draw.yml`
- `application-uslottery.yml`

alors que les fichiers présents sont :

- `application-draw.yaml`
- `application-uslottery.yaml`

Décision : corriger la configuration importée et vérifier le chargement effectif via test de contexte.

### F2 — Duplication de configuration provider/results

Des clés `tch.us-lottery.*` et `tch.draw.results.*` existent à plusieurs endroits, avec du contenu non strictement identique.

Décision : définir une source de vérité unique par famille de propriétés :

- `application.yaml` pour les valeurs transverses stables
- `application-draw.yaml` pour la mécanique draw/results
- `application-uslottery.yaml` pour les providers externes

Les doublons restants sont supprimés, pas simplement commentés.

### F3 — Projection Haïti non alignée avec `result_slot.projection_cfg`

`ResultSlotView` expose bien `projectionCfg`, et la doc drawresult dit que la projection lit `slot.projection_cfg`. Pourtant, le fetch handler charge aujourd'hui un `HaitiProjectionConfigPort.getDefault()`.

Décision :

- introduire une résolution explicite de projection par slot
- ordre de priorité :
  1. `result_slot.projection_cfg` si valide
  2. fallback global documenté
- mettre à jour `RecordManualDrawResultCommandHandler` sur la même règle

### F4 — Gates batch incohérentes

La config expose des gates distinctes pour :

- `fetch`
- `apply`
- `refresh`
- `manual`
- `override`

mais `DrawResultsOpsController` utilise aujourd'hui le gate `RESULTS_EXTERNAL_REFRESH` pour `override` et `manual`.

Décision : chaque endpoint ops doit utiliser son gate dédié. Les noms de gates deviennent partie intégrante du contrat d'exploitation.

### F5 — Apply scheduler et propriétés non homogènes

`ExternalResultsApplyTickScheduler` lit un cron distinct (`tch.draw.results.scheduler.apply_cron`) qui n'est pas aligné sur le schéma principal `tch.draw.results.shared.scheduler.*`.

Décision :

- introduire une arborescence unique et cohérente pour les propriétés scheduler results
- migrer fetch/apply vers cette arborescence
- conserver des alias transitoires seulement si nécessaire pour compatibilité runtime

### F6 — Settle scheduler encore trop codé en dur

`DrawSettleScheduler` lance explicitement NY/FL/GA/TX/TN.

Décision :

- conserver la logique de filtering par provider
- mais dériver la liste des providers actifs depuis la configuration/runtime registry
- permettre des switches provider-level sans dépendre d'une liste codée dans la classe

### F7 — Documentation pipeline incomplète

Écarts identifiés :

- `DOMAIN_DRAWCHANNEL.md` manquant près du code
- `FEATURE_PUBLICDRAW.md` référence des packages/contrats plus à jour
- `DrawResultIngestedEvent` documenté comme global pur alors que son payload actuel transporte des identifiants tenant/draw/channel

Décision : la doc doit refléter le flow réel et les contrats réellement exposés.

## Design

### D1 — Sources de configuration

Le design cible est :

- `application.yaml`
  - imports Spring corrects
  - fenêtres batch globales
  - conventions runtime communes
- `application-draw.yaml`
  - propriétés `tch.draw.*`
  - results scheduler/apply/fetch shared props
  - gates draw-results si elles ne vivent pas déjà au niveau racine de l'app
- `application-uslottery.yaml`
  - providers externes
  - base URLs, headers, paths, timezone, games

Règle : une propriété ne doit pas être définie dans deux fichiers différents sauf alias de transition documenté.

### D2 — Pipeline fetch/apply

Pipeline cible :

1. `ResultSlotCatalog.findByKey(slotKey)`
2. lecture `source_cfg` pour déterminer les channel codes providers actifs
3. `UsLotteryProviderClient.fetchDraws(...)`
4. normalisation en `ExternalResultOutput`
5. résolution de `occurredAt` via `OccurredAtResolver`
6. résolution de la config de projection :
   - `projection_cfg` du slot
   - fallback global documenté
7. projection Haïti
8. upsert `draw_result`
9. apply par tenant via `DrawApplyPort`

### D3 — Batch model

Schedulers conservés :

- `DrawLifeCycleTickScheduler`
  - generate
  - open
  - close
- `ExternalResultsFetchTickScheduler`
- `ExternalResultsApplyTickScheduler`
- `DrawSettleScheduler`

Règles :

- chaque scheduler doit avoir :
  - une gate explicite
  - des propriétés bindées et testées
  - une logique de scope claire (global vs tenant-scoped)
- `fetch` reste global
- `apply` reste tenant-scoped
- `settle` reste tenant-scoped et provider-filtered

### D4 — Documentation model

Artifacts à réaligner :

- flow fonctionnel MkDocs
- `DOMAIN_*` backend
- `FEATURE_PUBLICDRAW.md`

La doc backend près du code reste la source de vérité technique.

## Implementation Notes

- Ajouter un test Spring Boot de binding dédié pour empêcher les régressions de config
- Ajouter des tests unitaires/adapters pour la résolution provider/game/channel à partir de `source_cfg`
- Ajouter des tests pour la résolution `projection_cfg` par slot
- Ne pas profiter de la change pour refondre l'agrégat `Draw`

## Open Questions Closed

- Priorité retenue : **correctness prod**
- Arbitrage doc/code : **code/runtime d'abord**
- La change se concentre sur le **pipeline réel**, pas sur une refonte de modèle locale
