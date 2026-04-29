## Why

Le flow `draw` est un pipeline cross-domaines critique pour le produit :

- `catalog.game`
- `catalog.resultslot`
- `catalog.drawchannel`
- `core.uslottery`
- `core.haiti`
- `core.drawresult`
- `core.draw`
- `features.publicdraw`
- `features.ops`

Ce pipeline couvre la génération des draws, l'ouverture/fermeture des ventes, la récupération des résultats externes, la projection Haïti, l'application tenant-scoped, le settlement batch, puis l'exposition publique.

L'analyse initiale a révélé plusieurs écarts qui rendent le flow difficile à fiabiliser :

- **Configuration éclatée et potentiellement contradictoire** entre `application.yaml`, `application-draw.yaml` et `application-uslottery.yaml`
- **Import Spring incohérent** : `application.yaml` référence `application-draw.yml` / `application-uslottery.yml` alors que les fichiers présents sont en `.yaml`
- **Écart doc ↔ code sur la projection Haïti** :
  - la doc `core.drawresult` indique que la projection lit `result_slot.projection_cfg`
  - le code utilise aujourd'hui un `HaitiProjectionConfigPort.getDefault()` global
- **Gates batch incomplètement alignées** :
  - `override` et `manual` passent actuellement par le gate `RESULTS_EXTERNAL_REFRESH`
  - la doc et le schéma de config laissent entendre des gates distinctes
- **Schedulers et propriétés non homogènes** :
  - le cron d'apply lit un chemin de propriété distinct du reste
  - `DrawSettleScheduler` reste partiellement hardcodé par provider
- **Documentation incomplète ou obsolète** :
  - pas de `DOMAIN_DRAWCHANNEL.md` proche du code
  - références obsolètes dans `FEATURE_PUBLICDRAW.md`
  - `DrawResultIngestedEvent` documenté comme global, alors que l'event actuel transporte aussi des identifiants tenant/draw/channel

Sans une change dédiée, on risque de corriger localement des handlers ou des entités sans stabiliser la chaîne d'exécution réelle.

## What Changes

- **[Pipeline audit as source of truth]** Créer une cartographie technique du flow `draw` / `drawresult` couvrant :
  - controllers admin / ops / public
  - schedulers
  - commands
  - ports inter-domaines
  - events downstream
- **[Config hardening]** Unifier et documenter les sources de configuration runtime pour :
  - `tch.draw.results.*`
  - `tch.batch.gates.results.*`
  - `tch.us-lottery.providers.*`
  - imports Spring des YAML spécialisés
- **[Projection alignment]** Aligner le code et la doc sur une règle explicite pour `projection_cfg` :
  - support par slot si présent
  - fallback global documenté sinon
- **[Batch hardening]** Clarifier et durcir les schedulers `fetch`, `apply`, `open`, `close`, `settle` :
  - gates correctes
  - cron correctes
  - providers pilotés par configuration plutôt que par liste codée en dur quand pertinent
- **[Provider/client verification]** Vérifier la cohérence entre :
  - `result_slot.source_cfg`
  - registry `uslottery`
  - clients NY/FL/GA/TN/TX
  - matching provider/channel/external key
- **[Documentation refresh]** Réécrire la doc du flow réel côté backend et doc fonctionnelle

## Capabilities

### New Capabilities

- `draw-execution-pipeline-hardening`: définit les invariants runtime du pipeline draw/drawresult, les sources de configuration autorisées, la règle de projection Haïti, et les responsabilités des schedulers/batchs

### Modified Capabilities

- `draw-execution`: clarifie le pipeline réel generate/open/close/fetch/apply/settle
- `draw-result-ingestion`: aligne fetch, projection, upsert, statut et publication d'event
- `public-draw-results`: réaligne les dépendances feature/publicdraw sur les contrats réellement exposés

## Impact

- **Code modifié** :
  - configuration Spring / properties liées à `draw`, `drawresult`, `uslottery`
  - schedulers `ExternalResultsFetchTickScheduler`, `ExternalResultsApplyTickScheduler`, `DrawLifeCycleTickScheduler`, `DrawSettleScheduler`
  - handlers `FetchExternalResultsWindowCommandHandler`, `ApplyExternalResultsWindowCommandHandler`, `RefreshExternalResultsWindowCommandHandler`
  - ports/adapters de projection Haïti si `projection_cfg` par slot devient effectif
  - contrôleurs ops draw / draw-results si les gates sont corrigées
- **Code créé** :
  - doc proche du code pour `catalog.drawchannel` si absente
  - tests de binding de propriétés
  - tests ciblés de pipeline runtime si absents
- **Configuration** :
  - nettoyage des imports YAML
  - suppression des doublons contradictoires
  - chemins de propriétés batch/results rendus cohérents
- **Docs** :
  - `DOMAIN_DRAW.md`
  - `DOMAIN_DRAWRESULT.md`
  - `DOMAIN_USLOTTERY.md`
  - `DOMAIN_DRAWCHANNEL.md` si manquante
  - `FEATURE_PUBLICDRAW.md`
  - `tchalanet-docs/docs/02-functional/flows/draw-execution.md`
- **Non scope** :
  - refonte générale du modèle métier `Draw`
  - redesign des APIs publiques web/mobile hors corrections nécessaires
  - migration SQL structurelle hors besoin directement lié au pipeline d'exécution

## Risks / Trade-offs

- **[Risque] Nettoyer la config révèle des hypothèses implicites** : certains environnements peuvent dépendre d'un doublon ou d'une valeur par défaut non documentée
  - Mitigation : tests de binding, fallback explicite, inventaire des valeurs runtime attendues
- **[Risque] Support `projection_cfg` par slot change le comportement métier** si les seeds actuels ne correspondent pas au défaut global
  - Mitigation : fallback documenté + tests par slot avant activation stricte
- **[Risque] Dé-hardcoder le settle scheduler peut modifier le volume batch**
  - Mitigation : garder les limites par provider configurables et tracer les exécutions
- **[Trade-off] Change large mais cohérente** : plusieurs domaines sont touchés
  - Accepté car le problème est pipeline-first, pas slice locale

## Goals / Non-Goals

**Goals**

- Sécuriser le runtime du flow draw/drawresult de bout en bout
- Réduire les ambiguïtés de configuration
- Rendre les batchs et schedulers lisibles, cohérents et configurables
- Aligner doc et code sur le flow réellement supporté

**Non-Goals**

- Refonte complète des agrégats `Draw` ou `DrawResult`
- Changement fonctionnel large des écrans publicdraw
- Rebaselining Flyway ou restructuration SQL globale
