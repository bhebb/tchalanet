# Plan — Dashboards Platform : Ops par défaut + Commercial

## Contexte

Le dashboard actuel est visuellement propre et cohérent avec l’identité Tchalanet, mais il mélange deux intentions différentes :

1. **Ops / contrôle plateforme** : savoir ce qui tourne, ce qui est bloqué, ce qui doit être corrigé.
2. **Commercial / business overview** : suivre tenants, abonnements, ventes, onboarding et activité commerciale.

Décision proposée :

- L’entrée principale de la plateforme superadmin ouvre le **dashboard Ops**.
- Depuis Ops, l’utilisateur peut basculer vers le **dashboard Commercial**.
- Les deux dashboards peuvent utiliser le **même moteur de page template / PageModel renderer**, mais avec des templates, widgets et data providers différents.

---

## Objectif produit

### Objectif Ops

Répondre rapidement à :

- Est-ce que la plateforme est saine ?
- Quels jobs ont échoué ?
- Quels tirages sont en retard ?
- Quels résultats doivent être récupérés ou appliqués ?
- Quels settlements sont en attente ?
- Quels caches ou providers posent problème ?
- Quelle action Ops dois-je lancer maintenant ?

### Objectif Commercial

Répondre rapidement à :

- Combien de tenants sont actifs ?
- Quelle est l’activité commerciale du jour ?
- Combien d’abonnements sont actifs ou en retard ?
- Quels tenants performent le mieux ?
- Combien de ventes/tickets sur la période ?
- Où en est l’onboarding des tenants ?

---

## Décision UX

### Entrée par défaut

Route par défaut superadmin :

```text
/app/platform/ops
```

ou, si la route existante doit rester stable :

```text
/app/platform/dashboard
```

mais son contenu devient **Ops-first**.

### Accès au Commercial

Depuis le dashboard Ops, ajouter un switch visible :

```text
[ Ops ] [ Commercial ]
```

ou un bouton secondaire :

```text
Voir le dashboard commercial
```

Routes recommandées :

```text
/app/platform/ops
/app/platform/commercial
```

Alternative si on veut garder une seule famille :

```text
/app/platform/dashboard/ops
/app/platform/dashboard/commercial
```

Recommandation : **`/app/platform/ops` + `/app/platform/commercial`**. C’est plus clair dans la navigation et ça évite de faire croire que tout est un seul dashboard générique.

---

## Dashboard 1 — Ops Platform

### Intention

Poste de contrôle plateforme. Le superadmin arrive ici pour voir les incidents, les traitements automatiques, les tirages et les actions correctives.

### Structure visuelle proposée

```text
Header
- Titre : Opérations plateforme
- Sous-titre : Santé des traitements, tirages, résultats et caches
- Switch : Ops / Commercial
- Actions : Rafraîchir, Voir logs, Exporter état

Section 1 — État opérationnel
- Santé globale
- Jobs en erreur
- Tirages en retard
- Résultats à traiter

Section 2 — Pipeline tirages
- SCHEDULED
- OPEN
- CLOSED
- RESULTED
- SETTLED
- BLOCKED / LATE

Section 3 — Batch & schedulers
- draw.generate
- draw.open_today
- draw.close
- drawresult.fetch
- drawresult.apply
- sales.settle

Section 4 — Providers & résultats
- NY
- FL
- GA
- TX
- statut provider
- dernier fetch
- prochaine tentative
- dernière erreur

Section 5 — Cache & services
- public.draw.latest
- catalog.*
- tenant runtime
- theme/runtime
- état Redis/L1 si disponible

Section 6 — Alertes
- critiques
- warning
- infos
- empty state si aucune alerte

Section 7 — Actions Ops rapides
- Relancer fetch résultats
- Appliquer résultats
- Relancer settlement
- Ouvrir draw lifecycle
- Vider cache public
- Voir batch gates
```

### KPIs Ops recommandés

| Widget | But | Variante couleur |
|---|---|---|
| Santé globale | UP / DEGRADED / DOWN | vert / orange / rouge |
| Jobs en erreur | nombre d’échecs récents | rouge si > 0 |
| Tirages en retard | draws hors fenêtre normale | orange/rouge |
| Résultats à traiter | fetch/apply pending | orange |
| Settlements en attente | tickets à régler | orange |
| Providers dégradés | résultats externes instables | orange/rouge |
| Cache stale | caches critiques vieux | orange |

### Widgets Ops détaillés

#### `opsHealthSummary`

Affiche :

```text
status: UP | DEGRADED | DOWN
criticalAlertsCount
warningAlertsCount
lastCheckAt
```

#### `opsDrawPipeline`

Affiche les compteurs par état :

```text
scheduledCount
openCount
closedCount
resultedCount
settledCount
lateCount
blockedCount
```

#### `opsBatchStatus`

Affiche les jobs/gates :

```text
jobKey
active
lastRunAt
lastStatus
lastDurationMs
lastError
nextRunAt
```

#### `opsProviderStatus`

Affiche les providers :

```text
providerCode
status
lastFetchAt
lastSuccessAt
lastErrorAt
lastErrorMessage
pendingSlots
```

#### `opsCacheStatus`

Affiche les caches critiques :

```text
cacheName
status
estimatedSize
lastEvictedAt
ttl
stale
```

#### `opsQuickActions`

Actions disponibles selon permissions :

```text
openDrawLifecycle
runFetchResults
runApplyResults
runSettlement
clearPublicDrawCache
openBatchGates
```

### Empty states Ops

Exemples :

```text
Aucune alerte critique.
Tous les traitements critiques sont dans leur fenêtre normale.
```

```text
Aucun tirage en retard.
Le pipeline de tirages est à jour.
```

```text
Aucun provider dégradé.
Les résultats externes répondent normalement.
```

---

## Dashboard 2 — Commercial Platform

### Intention

Vue business plateforme. Le superadmin l’utilise pour comprendre l’activité commerciale, les tenants, les abonnements et l’onboarding.

### Structure visuelle proposée

```text
Header
- Titre : Performance commerciale
- Sous-titre : Tenants, abonnements, ventes et onboarding
- Switch : Ops / Commercial
- Filtres : Aujourd’hui, 7 jours, 30 jours, période personnalisée
- Actions : Exporter, Imprimer

Section 1 — Tenants
- Total tenants
- Tenants actifs
- Tenants suspendus

Section 2 — Activité du jour
- Tickets vendus
- Ventes brutes
- Net estimé

Section 3 — Abonnements
- Abonnements actifs
- En retard
- Total abonnement / MRR si disponible plus tard

Section 4 — Onboarding
- En attente
- Incomplets
- Prêts à vendre

Section 5 — Graphiques
- Tendance des ventes
- Ventes par jeu

Section 6 — Classements
- Top tenants
- Top terminaux / vendeurs si pertinent

Section 7 — Support & contenu
- Actualités publiques
- demandes support / contact si disponible

Section 8 — Actions rapides commerciales
- Créer un tenant
- Voir onboarding
- Gérer abonnements
- Voir rapports plateforme
```

### KPIs Commercial recommandés

| Widget | But | Variante couleur |
|---|---|---|
| Total tenants | taille plateforme | navy |
| Tenants actifs | tenants opérationnels | vert pâle |
| Tenants suspendus | tenants problématiques | rouge pâle |
| Tickets vendus | activité du jour | navy |
| Ventes brutes | activité argent | gold ou gold pâle |
| Net estimé | marge estimée | vert pâle |
| Abonnements actifs | revenu récurrent | gold |
| En retard | risque commercial | orange pâle |
| Onboarding en attente | setup à finir | orange pâle |

### Règle couleur

Pour éviter un dashboard trop jaune :

```text
Navy plein  = métriques principales structurantes
Gold plein  = une seule métrique business prioritaire
Gold pâle   = métriques argent secondaires
Vert pâle   = état OK / actif
Orange pâle = attention / pending / retard
Rouge pâle  = blocage / suspendu
Neutre pâle = total secondaire / info stable
```

---

## Même moteur de page template ?

### Réponse courte

Oui, on peut utiliser le **même moteur de page template / PageModel renderer** pour les deux dashboards.

Mais il ne faut pas utiliser le même **template métier** ni le même **data provider**.

La bonne séparation est :

```text
Même renderer
Même système de widgets
Même composants visuels de dashboard
Templates différents
Providers différents
Endpoints BFF différents
```

---

## Architecture recommandée

### Frontend

```text
features/platform/ops
  platform-ops.page.ts
  platform-ops.store.ts

features/platform/commercial
  platform-commercial.page.ts
  platform-commercial.store.ts

libs/page-model
  page-model-renderer.ts
  widget-host.ts
  page-model.types.ts

libs/widgets/dashboard
  kpi-card.widget.ts
  chart-card.widget.ts
  status-list.widget.ts
  quick-actions.widget.ts
  alerts.widget.ts
```

### Backend

```text
features/platformadmin/ops
  PlatformOpsDashboardController
  PlatformOpsDashboardService
  PlatformOpsDashboardResponse

features/platformadmin/commercial
  PlatformCommercialDashboardController
  PlatformCommercialDashboardService
  PlatformCommercialDashboardResponse
```

ou, si on veut rester strictement PageModel :

```text
features/platformadmin/dashboard
  PlatformDashboardPageModelController
  PlatformDashboardDataProviderRegistry
  OpsDashboardDataProvider
  CommercialDashboardDataProvider
```

### Routes API possibles

Option A — endpoints BFF simples :

```http
GET /api/v1/platform/ops/dashboard
GET /api/v1/platform/commercial/dashboard
```

Option B — PageModel runtime :

```http
GET /api/v1/platform/page-models/runtime/platform.ops.dashboard
GET /api/v1/platform/page-models/runtime/platform.commercial.dashboard
```

Option C — mix recommandé :

```http
GET /api/v1/platform/dashboard/ops
GET /api/v1/platform/dashboard/commercial
```

Chaque endpoint retourne un modèle déjà prêt pour le renderer :

```json
{
  "page": { ... },
  "widgets": [ ... ],
  "data": { ... },
  "notices": [ ... ],
  "services": [ ... ]
}
```

---

## Recommandation technique

### Utiliser le PageModel renderer pour le layout

Le PageModel peut très bien porter :

```text
title
subtitle
actions
sections
rows
columns
widgets
empty states
```

Donc oui, il est pertinent pour :

- composer la grille ;
- rendre les cartes KPI ;
- rendre les charts ;
- rendre les listes d’alertes ;
- rendre les actions rapides ;
- permettre d’ajuster le layout sans recoder la page.

### Ne pas mettre toute la logique dans le PageModel

Le PageModel ne doit pas devenir un gros objet opaque qui calcule le métier.

À éviter :

```text
PageModel qui connaît les règles de batch
PageModel qui calcule les statuts de draw
PageModel qui agrège directement les ventes
PageModel qui décide des permissions Ops
```

À faire :

```text
Backend BFF calcule les données
Providers construisent les payloads widgets
Renderer affiche seulement
Widgets restent présentationnels
```

---

## Séparation données / présentation

### Template PageModel

Le template décrit la structure :

```text
row 1: ops health KPIs
row 2: draw pipeline + alerts
row 3: batch jobs + providers
row 4: cache + quick actions
```

### Data provider

Le provider remplit les données :

```text
ops.health.status
ops.batch.jobs
ops.draw.pipeline
ops.providers.statuses
ops.cache.entries
```

### Widget

Le widget rend une structure stable :

```text
KpiCardWidget
StatusListWidget
PipelineWidget
QuickActionsWidget
```

---

## Exemple de définition logique — Ops

```json
{
  "logicalId": "platform.ops.dashboard",
  "titleKey": "platform.ops.dashboard.title",
  "layout": {
    "type": "dashboard-grid",
    "sections": [
      {
        "titleKey": "platform.ops.section.health",
        "widgets": [
          { "type": "kpi-card", "dataKey": "health.global" },
          { "type": "kpi-card", "dataKey": "jobs.failed" },
          { "type": "kpi-card", "dataKey": "draws.late" },
          { "type": "kpi-card", "dataKey": "results.pending" }
        ]
      },
      {
        "titleKey": "platform.ops.section.pipeline",
        "widgets": [
          { "type": "draw-pipeline", "dataKey": "drawPipeline" },
          { "type": "alerts", "dataKey": "alerts" }
        ]
      }
    ]
  }
}
```

## Exemple de définition logique — Commercial

```json
{
  "logicalId": "platform.commercial.dashboard",
  "titleKey": "platform.commercial.dashboard.title",
  "layout": {
    "type": "dashboard-grid",
    "sections": [
      {
        "titleKey": "platform.commercial.section.tenants",
        "widgets": [
          { "type": "kpi-card", "dataKey": "tenants.total" },
          { "type": "kpi-card", "dataKey": "tenants.active" },
          { "type": "kpi-card", "dataKey": "tenants.suspended" }
        ]
      },
      {
        "titleKey": "platform.commercial.section.sales",
        "widgets": [
          { "type": "line-chart", "dataKey": "sales.trend" },
          { "type": "donut-chart", "dataKey": "sales.byGame" }
        ]
      }
    ]
  }
}
```

---

## Données et fraîcheur

### Ops

Ops doit être plus frais :

```text
refresh manuel visible
auto-refresh optionnel 30–60 sec plus tard
cache court pour summary
aucun cache sur actions critiques
```

### Commercial

Commercial peut être moins frais :

```text
refresh manuel
cache 1–5 min selon widget
période filtrable
charts calculés côté BFF/read model
```

---

## Permissions

### Ops

Accès :

```text
SUPER_ADMIN
permission: platform.ops.read
```

Actions forcées :

```text
platform.ops.execute
platform.ops.cache.clear
platform.ops.draw.force
platform.ops.result.force
```

Les actions forcées doivent demander :

```text
force=true
reason non vide
audit obligatoire
```

### Commercial

Accès :

```text
SUPER_ADMIN
permission: platform.commercial.read
```

Actions :

```text
platform.tenant.create
platform.subscription.manage
platform.report.export
```

---

## Design system dashboard

### Composants à créer/réutiliser

```text
tch-dashboard-shell
tch-dashboard-switch
tch-kpi-card
tch-status-kpi-card
tch-dashboard-section
tch-dashboard-grid
tch-empty-state
tch-alert-list
tch-quick-action-grid
```

### Variantes KPI

```text
primary
accent
success
warning
danger
neutral
```

Mapping :

```text
primary -> navy
accent -> gold
success -> vert pâle
warning -> orange pâle
danger -> rouge pâle
neutral -> surface neutre
```

---

## Plan d’implémentation

### Étape 1 — Clarifier navigation

- [ ] Route par défaut superadmin vers Ops.
- [ ] Ajouter entrée Commercial dans la page ou la sidebar.
- [ ] Renommer le dashboard actuel en Commercial si besoin.

### Étape 2 — Extraire composants dashboard

- [ ] `tch-kpi-card`
- [ ] `tch-dashboard-section`
- [ ] `tch-quick-action-grid`
- [ ] variantes couleur via tokens `--tch-*`

### Étape 3 — Commercial dashboard

- [ ] Garder les widgets actuels.
- [ ] Appliquer la règle couleur.
- [ ] Ajouter période et export plus tard.

### Étape 4 — Ops dashboard

- [ ] Créer template Ops.
- [ ] Ajouter endpoint BFF Ops summary.
- [ ] Ajouter widgets pipeline draws, batch jobs, providers, cache, alertes.
- [ ] Brancher actions rapides vers les pages Ops existantes.

### Étape 5 — PageModel engine

- [ ] Vérifier que les widgets dashboard peuvent être rendus par le renderer.
- [ ] Créer deux logical IDs :
  - `platform.ops.dashboard`
  - `platform.commercial.dashboard`
- [ ] Brancher providers différents.
- [ ] Garder les widgets présentationnels.

---

## Critères d’acceptation

### UX

- [ ] Le superadmin arrive sur une page Ops claire.
- [ ] Il peut accéder au dashboard Commercial en un clic.
- [ ] Les deux dashboards ne mélangent pas leurs intentions.
- [ ] Les empty states sont explicites.
- [ ] Les couleurs respectent la hiérarchie : navy structure, gold accent, vert/orange/rouge statuts.

### Technique frontend

- [ ] Pas de couleurs hardcodées dans les composants.
- [ ] Utilisation de tokens `--tch-*` et variables locales `--comp-*`.
- [ ] Widgets stateless autant que possible.
- [ ] Stores séparés Ops / Commercial.
- [ ] Le renderer PageModel peut afficher les deux templates.

### Technique backend

- [ ] Les endpoints dashboard vivent dans `features/platformadmin`.
- [ ] Les controllers restent thin.
- [ ] Les données viennent de queries/read models, pas de repositories cross-domain.
- [ ] Les actions Ops forcées sont permissionnées et auditées.
- [ ] Les caches sont utilisés seulement comme optimisation, jamais comme source de vérité.

---

## Décision finale proposée

```text
On garde deux dashboards :

1. Ops Dashboard — entrée par défaut de la plateforme.
2. Commercial Dashboard — accessible depuis Ops et/ou sidebar.

Les deux utilisent le même moteur PageModel/rendering et les mêmes composants dashboard.
Ils n’utilisent pas le même template métier ni les mêmes providers.

Ops = contrôle, incidents, jobs, tirages, résultats, cache.
Commercial = tenants, abonnements, ventes, onboarding, performance.
```
