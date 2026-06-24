# Gaps backend — admin-superadmin-sidenav-v0

Ces endpoints n'existent pas encore côté backend. Hors scope V0 web.
Les pages affichent un état placeholder ou désactivent l'action concernée.

## Dashboard superadmin

Aucun BFF dashboard platform. Contournement V0 : appels parallèles indépendants.

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
