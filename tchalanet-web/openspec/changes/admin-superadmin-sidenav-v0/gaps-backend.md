# Gaps backend — admin-superadmin-sidenav-v0

Ces endpoints n'existent pas encore côté backend. Hors scope V0 web.
Les pages affichent un état placeholder ou désactivent l'action concernée.

## Dashboard superadmin

Le BFF PageModel dashboard existe déjà :

```http
GET /platform/dashboard
```

Il résout `private.dashboard.superadmin`, passe par `PageModelDynamicResolver`, puis par le provider `platform_admin_dashboard`.

**Gap V0 réel** : le template superadmin doit être aligné avec le provider existant. Les widgets du cockpit doivent utiliser :

```json
{
  "binding": {
    "mode": "dynamic",
    "source": "platform_admin_dashboard"
  }
}
```

**Pas de contournement Angular** : le frontend ne doit pas composer le cockpit avec des appels parallèles indépendants.

**Risque performance** : si certains widgets deviennent trop lents, ajouter une évolution dédiée pour le mode différé (`runtime.loadStrategy = deferred` ou endpoint de résolution partielle de widgets). Hors scope V0 sauf blocage mesuré.

## Stats legacy

`features.stats` est considéré legacy pour les nouveaux dashboards. Ne pas l'utiliser pour alimenter `tenant_admin_dashboard` ou `platform_admin_dashboard`.

**Source cible KPI/charts** : `core.analytics.api`.

**Suivi hors V0** : plan de retrait ou d'archivage de `features.stats` après inventaire des endpoints encore consommés.

## Rapports platform

```http
GET /platform/reports/tenant-sales
GET /platform/reports/ops-health
GET /platform/reports/support-messages
GET /platform/reports/draw-results-quality
```

**Contournement V0** : widgets par domaine avec placeholder "Rapport disponible prochainement".

## Configuration contact

```http
GET /platform/contact-config
PUT /platform/contact-config
```

**Contournement V0** : page lecture seule ou placeholder avec champs en attente.

## Messages de contact — actions lifecycle

```http
PATCH /platform/contact-requests/{id}/status
POST  /platform/contact-requests/{id}/archive
POST  /platform/contact-requests/{id}/reply
```

**Contournement V0** : vue lecture seule, pas d'action de traitement.

## Santé système

Pas d'endpoint dédié `GET /platform/health` structuré visible dans l'extraction.
**Contournement V0** : `PlatformOpsPage` existante fait office de santé système.

## Accès & sécurité — gestion avancée

```http
POST /admin/access-control/permissions   (create)
POST /admin/access-control/roles         (create)
PATCH /admin/access-control/roles/{id}   (update)
PATCH /identity/users/{id}/status        (disable/reactivate)
```

**Contournement V0** : pages en lecture seule uniquement.

## Plans séparés du Pricing

Si `POST /platform/plans` et `POST /platform/pricing` sont distincts côté backend, à valider.
**Contournement V0** : si le même endpoint gère les deux, la page Plans affiche le sous-ensemble plans.
