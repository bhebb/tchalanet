# Plan — Bloc ressources services pour Dashboard Ops

## Objectif

Ajouter au **Dashboard Ops** un bloc compact de surveillance des ressources runtime, sans transformer l’écran Ops en outil complet de monitoring infra.

Le dashboard Ops doit rester orienté anomalies :

- jobs failed ;
- jobs non run / stale ;
- gates désactivés ;
- tirages ou résultats bloqués ;
- services proches de tomber.

Le bloc ressources répond à la question :

> Est-ce qu’un service critique est en train de manquer de mémoire, redémarre trop souvent, ou a été tué par OOM ?

---

## Positionnement dans Ops

Le bloc ressources est **P1**, pas P0.

### P0 — Ops métier

- Jobs failed.
- Jobs stale / non exécutés.
- Draw lifecycle bloqué.
- Résultats providers manquants.
- Cache critique.
- Alertes opérationnelles.

### P1 — Infra runtime

- Mémoire Docker / container.
- CPU élevé.
- Restart count.
- OOM killed.
- DB / Redis health.

---

## UX proposée

### KPI compact en haut

Ajouter une carte dans la première rangée Ops :

```text
Ressources critiques
0
Aucun service en seuil critique
```

États possibles :

```text
0  -> OK
1+ -> WARNING / CRITICAL selon gravité
```

### Carte détail plus bas

```text
Ressources services

API server        620 MB / 1 GB      62%   OK
Postgres          740 MB / 2 GB      37%   OK
Redis              95 MB / 512 MB    18%   OK
Worker            910 MB / 1 GB      91%   CRITICAL

[Voir détails infra]
```

Le dashboard affiche uniquement les services importants et les anomalies. Le détail complet reste dans une page dédiée future :

```text
/app/platform/ops/resources
```

---

## Services à surveiller V0

Minimum :

- `tchalanet-server` / API backend ;
- `postgres` ;
- `redis` si activé ;
- worker / scheduler si séparé ;
- frontend/nginx si déployé en container.

Local/dev optionnel :

- Firebase emulator ;
- Mailhog / outils dev ;
- Jaeger / observability stack.

---

## Données à exposer

Pour chaque service :

```text
serviceKey
name
status
memoryUsedMb
memoryLimitMb
memoryPercent
cpuPercent
restartCount
oomKilled
lastRestartAt
severity
message
```

Statuts :

```text
UP
DEGRADED
DOWN
UNKNOWN
```

Sévérités :

```text
OK
WARNING
CRITICAL
```

---

## Règles de seuil

### Mémoire

```text
memoryPercent < 80%       -> OK
memoryPercent >= 80%      -> WARNING
memoryPercent >= 90%      -> CRITICAL
```

### Restarts

```text
restartCount augmenté dans la dernière heure -> WARNING
restart loop détecté                         -> CRITICAL
```

### OOM

```text
oomKilled = true -> CRITICAL
```

### Service health

```text
UP       -> OK
DEGRADED -> WARNING
DOWN     -> CRITICAL
UNKNOWN  -> WARNING
```

---

## Source de données recommandée

### À éviter en prod/stg

Éviter de donner à `tchalanet-server` un accès direct au Docker socket :

```text
/var/run/docker.sock
```

Raison : accès trop puissant, équivalent à un contrôle élevé sur l’hôte.

### Cible propre

```text
Dashboard Ops
  -> Backend BFF ops
    -> Observability provider
      -> Prometheus / cAdvisor / Docker metrics exporter
```

### Local/dev

On peut documenter une source simple basée sur :

```text
docker stats
docker inspect
docker compose ps
```

Mais ce mode doit rester local/dev ou protégé.

---

## Endpoint proposé

```http
GET /api/v1/platform/ops/resources/summary
```

Réponse :

```ts
export interface OpsResourceSummaryResponse {
  generatedAt: string;
  criticalCount: number;
  warningCount: number;
  services: OpsServiceResourceItem[];# Plan — Bloc ressources services pour Dashboard Ops

## Objectif

Ajouter au **Dashboard Ops** un bloc compact de surveillance des ressources runtime, sans transformer l’écran Ops en outil complet de monitoring infra.

Le dashboard Ops doit rester orienté anomalies :

- jobs failed ;
- jobs non run / stale ;
- gates désactivés ;
- tirages ou résultats bloqués ;
- services proches de tomber.

Le bloc ressources répond à la question :

> Est-ce qu’un service critique est en train de manquer de mémoire, redémarre trop souvent, ou a été tué par OOM ?

---

## Positionnement dans Ops

Le bloc ressources est **P1**, pas P0.

### P0 — Ops métier

- Jobs failed.
- Jobs stale / non exécutés.
- Draw lifecycle bloqué.
- Résultats providers manquants.
- Cache critique.
- Alertes opérationnelles.

### P1 — Infra runtime

- Mémoire Docker / container.
- CPU élevé.
- Restart count.
- OOM killed.
- DB / Redis health.

---

## UX proposée

### KPI compact en haut

Ajouter une carte dans la première rangée Ops :

```text
Ressources critiques
0
Aucun service en seuil critique
```

États possibles :

```text
0  -> OK
1+ -> WARNING / CRITICAL selon gravité
```

### Carte détail plus bas

```text
Ressources services

API server        620 MB / 1 GB      62%   OK
Postgres          740 MB / 2 GB      37%   OK
Redis              95 MB / 512 MB    18%   OK
Worker            910 MB / 1 GB      91%   CRITICAL

[Voir détails infra]
```

Le dashboard affiche uniquement les services importants et les anomalies. Le détail complet reste dans une page dédiée future :

```text
/app/platform/ops/resources
```

---

## Services à surveiller V0

Minimum :

- `tchalanet-server` / API backend ;
- `postgres` ;
- `redis` si activé ;
- worker / scheduler si séparé ;
- frontend/nginx si déployé en container.

Local/dev optionnel :

- Firebase emulator ;
- Mailhog / outils dev ;
- Jaeger / observability stack.

---

## Données à exposer

Pour chaque service :

```text
serviceKey
name
status
memoryUsedMb
memoryLimitMb
memoryPercent
cpuPercent
restartCount
oomKilled
lastRestartAt
severity
message
```

Statuts :

```text
UP
DEGRADED
DOWN
UNKNOWN
```

Sévérités :

```text
OK
WARNING
CRITICAL
```

---

## Règles de seuil

### Mémoire

```text
memoryPercent < 80%       -> OK
memoryPercent >= 80%      -> WARNING
memoryPercent >= 90%      -> CRITICAL
```

### Restarts

```text
restartCount augmenté dans la dernière heure -> WARNING
restart loop détecté                         -> CRITICAL
```

### OOM

```text
oomKilled = true -> CRITICAL
```

### Service health

```text
UP       -> OK
DEGRADED -> WARNING
DOWN     -> CRITICAL
UNKNOWN  -> WARNING
```

---

## Source de données recommandée

### À éviter en prod/stg

Éviter de donner à `tchalanet-server` un accès direct au Docker socket :

```text
/var/run/docker.sock
```

Raison : accès trop puissant, équivalent à un contrôle élevé sur l’hôte.

### Cible propre

```text
Dashboard Ops
  -> Backend BFF ops
    -> Observability provider
      -> Prometheus / cAdvisor / Docker metrics exporter
```

### Local/dev

On peut documenter une source simple basée sur :

```text
docker stats
docker inspect
docker compose ps
```

Mais ce mode doit rester local/dev ou protégé.

---

## Endpoint proposé

```http
GET /api/v1/platform/ops/resources/summary
```

Réponse :

```ts
export interface OpsResourceSummaryResponse {
  generatedAt: string;
  criticalCount: number;
  warningCount: number;
  services: OpsServiceResourceItem[];
}

export interface OpsServiceResourceItem {
  serviceKey: string;
  displayName: string;
  status: 'UP' | 'DEGRADED' | 'DOWN' | 'UNKNOWN';

  memoryUsedMb?: number;
  memoryLimitMb?: number;
  memoryPercent?: number;

  cpuPercent?: number;

  restartCount?: number;
  oomKilled?: boolean;
  lastRestartAt?: string;

  severity: 'OK' | 'WARNING' | 'CRITICAL';
  message?: string;
  detailsPath?: string;
}
```

---

## Intégration dans l’endpoint Ops overview

L’endpoint principal Ops peut agréger un résumé minimal :

```http
GET /api/v1/platform/ops/overview
```

Fragment :

```ts
export interface OpsOverviewResponse {
  generatedAt: string;

  schedulerSummary: {
    failedCount: number;
    staleCount: number;
    neverRunCount: number;
    disabledGateCount: number;
  };

  resourceSummary: {
    criticalCount: number;
    warningCount: number;
    topItems: OpsServiceResourceItem[];
  };

  alerts: OpsAlertItem[];
}
```

Le dashboard affiche `resourceSummary`. Le détail complet charge `/resources/summary` seulement si besoin.

---

## Backend — placement recommandé

Comme il s’agit d’un écran Ops agrégé, le placement naturel est :

```text
features/platformadmin/ops
```

Structure possible :

```text
features/platformadmin/ops/
  web/
    PlatformOpsOverviewController
    PlatformOpsResourcesController
  app/
    PlatformOpsOverviewService
    OpsResourceSummaryService
  model/
    OpsOverviewResponse
    OpsResourceSummaryResponse
    OpsServiceResourceItem
  infra/
    ObservabilityMetricsClient
    DockerLocalMetricsClient   # dev/local seulement si nécessaire
```

Règle : la feature orchestre et agrège. Elle ne devient pas source de vérité infra.

---

## Frontend — placement recommandé

```text
features/platform/ops-dashboard/
  platform-ops-dashboard.page.ts
  platform-ops-dashboard.store.ts
  platform-ops-dashboard.scss
```

Composants réutilisables :

```text
libs/ui/components/dashboard-kpi-card
libs/ui/components/status-badge
libs/ui/components/resource-status-list
```

Le style doit consommer les tokens Tchalanet :

```text
--tch-color-primary
--tch-color-accent
--tch-color-surface
--tch-color-error
--tch-color-outline-variant
--tch-radius-xl
```

---

## Ce qu’on affiche vs ce qu’on n’affiche pas

### Afficher dans le dashboard

- Nombre de services critiques.
- Nombre de warnings.
- Top 3 services problématiques.
- Mémoire utilisée / limite.
- Restart / OOM si présent.
- Lien vers détails.

### Ne pas afficher dans le dashboard

- Liste complète de tous les containers.
- Logs détaillés.
- Historique long CPU/mémoire.
- Graphiques infra avancés.
- Paramètres Docker.

Ces éléments appartiennent à une page dédiée ou à l’outil d’observabilité.

---

## Critères d’acceptation V0

- [ ] Le dashboard Ops affiche `Ressources critiques`.
- [ ] Le dashboard affiche au moins API, Postgres, Redis si disponible.
- [ ] Un service à `memoryPercent >= 80%` remonte en WARNING.
- [ ] Un service à `memoryPercent >= 90%` remonte en CRITICAL.
- [ ] `oomKilled=true` remonte en CRITICAL.
- [ ] Les services DOWN remontent dans les alertes Ops.
- [ ] Le dashboard ne duplique pas une page complète de monitoring.
- [ ] Le détail est accessible via un lien `Voir détails infra`.
- [ ] Aucun accès Docker socket direct n’est requis en staging/prod.
- [ ] Les erreurs de collecte métriques dégradent le widget, mais ne cassent pas tout le dashboard.

---

## Décision finale

Ajouter un bloc **Ressources services** au Dashboard Ops.

Le bloc reste compact, alert-driven, et orienté décision :

```text
Est-ce qu’un service critique est proche de tomber ?
```

Le détail infra reste dans une page dédiée ou dans l’outil d’observabilité.

}

export interface OpsServiceResourceItem {
  serviceKey: string;
  displayName: string;
  status: 'UP' | 'DEGRADED' | 'DOWN' | 'UNKNOWN';

  memoryUsedMb?: number;
  memoryLimitMb?: number;
  memoryPercent?: number;

  cpuPercent?: number;

  restartCount?: number;
  oomKilled?: boolean;
  lastRestartAt?: string;

  severity: 'OK' | 'WARNING' | 'CRITICAL';
  message?: string;
  detailsPath?: string;
}
```

---

## Intégration dans l’endpoint Ops overview

L’endpoint principal Ops peut agréger un résumé minimal :

```http
GET /api/v1/platform/ops/overview
```

Fragment :

```ts
export interface OpsOverviewResponse {
  generatedAt: string;

  schedulerSummary: {
    failedCount: number;
    staleCount: number;
    neverRunCount: number;
    disabledGateCount: number;
  };

  resourceSummary: {
    criticalCount: number;
    warningCount: number;
    topItems: OpsServiceResourceItem[];
  };

  alerts: OpsAlertItem[];
}
```

Le dashboard affiche `resourceSummary`. Le détail complet charge `/resources/summary` seulement si besoin.

---

## Backend — placement recommandé

Comme il s’agit d’un écran Ops agrégé, le placement naturel est :

```text
features/platformadmin/ops
```

Structure possible :

```text
features/platformadmin/ops/
  web/
    PlatformOpsOverviewController
    PlatformOpsResourcesController
  app/
    PlatformOpsOverviewService
    OpsResourceSummaryService
  model/
    OpsOverviewResponse
    OpsResourceSummaryResponse
    OpsServiceResourceItem
  infra/
    ObservabilityMetricsClient
    DockerLocalMetricsClient   # dev/local seulement si nécessaire
```

Règle : la feature orchestre et agrège. Elle ne devient pas source de vérité infra.

---

## Frontend — placement recommandé

```text
features/platform/ops-dashboard/
  platform-ops-dashboard.page.ts
  platform-ops-dashboard.store.ts
  platform-ops-dashboard.scss
```

Composants réutilisables :

```text
libs/ui/components/dashboard-kpi-card
libs/ui/components/status-badge
libs/ui/components/resource-status-list
```

Le style doit consommer les tokens Tchalanet :

```text
--tch-color-primary
--tch-color-accent
--tch-color-surface
--tch-color-error
--tch-color-outline-variant
--tch-radius-xl
```

---

## Ce qu’on affiche vs ce qu’on n’affiche pas

### Afficher dans le dashboard

- Nombre de services critiques.
- Nombre de warnings.
- Top 3 services problématiques.
- Mémoire utilisée / limite.
- Restart / OOM si présent.
- Lien vers détails.

### Ne pas afficher dans le dashboard

- Liste complète de tous les containers.
- Logs détaillés.
- Historique long CPU/mémoire.
- Graphiques infra avancés.
- Paramètres Docker.

Ces éléments appartiennent à une page dédiée ou à l’outil d’observabilité.

---

## Critères d’acceptation V0

- [ ] Le dashboard Ops affiche `Ressources critiques`.
- [ ] Le dashboard affiche au moins API, Postgres, Redis si disponible.
- [ ] Un service à `memoryPercent >= 80%` remonte en WARNING.
- [ ] Un service à `memoryPercent >= 90%` remonte en CRITICAL.
- [ ] `oomKilled=true` remonte en CRITICAL.
- [ ] Les services DOWN remontent dans les alertes Ops.
- [ ] Le dashboard ne duplique pas une page complète de monitoring.
- [ ] Le détail est accessible via un lien `Voir détails infra`.
- [ ] Aucun accès Docker socket direct n’est requis en staging/prod.
- [ ] Les erreurs de collecte métriques dégradent le widget, mais ne cassent pas tout le dashboard.

---

## Décision finale

Ajouter un bloc **Ressources services** au Dashboard Ops.

Le bloc reste compact, alert-driven, et orienté décision :

```text
Est-ce qu’un service critique est proche de tomber ?
```

Le détail infra reste dans une page dédiée ou dans l’outil d’observabilité.
