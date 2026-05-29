# TODO — Finaliser PageModel contract prédictible

## Objectif

Stabiliser le contrat PageModel pour éviter une intégration Angular trop custom, supprimer les anciens fragments JSON incompatibles, et garantir que public/private/cashier/admin/superadmin utilisent des shapes prévisibles.

Règle globale :

```text
PageModel est dynamique par composition, pas par forme.
Les widgets, fragments, liens, actions, images et shells doivent avoir des schemas fixes.
Les maps libres sont interdites dans le contrat principal, sauf meta/ext.
```

---

# P0 — À faire maintenant

## 1. Normaliser les actions

Corriger tous les payloads `quick_actions.actions`.

Remplacer les mini-actions comme :

```json
{
  "id": "CREATE_TENANT",
  "labelKey": "quickaction.platform.create_tenant",
  "icon": "add_business",
  "path": "/app/platform/tenants/new"
}
```

par le schema complet `ActionItem` :

```json
{
  "id": "CREATE_TENANT",
  "type": "action",
  "label_key": "quickaction.platform.create_tenant",
  "label": null,
  "path": "/app/platform/tenants/new",
  "kind": "internal",
  "icon": "add_business",
  "image": null,
  "style": "primary",
  "disabled": false,
  "reason_key": null,
  "confirm": null
}
```

À corriger partout :

```text
dashboard.cashier.quick_actions.actions
dashboard.tenant_admin.quick_actions.actions
dashboard.superadmin.quick_actions.actions
```

## 2. Remplacer `labelKey` / `messageKey`

Standard retenu :

```text
label_key
message_key
```

À corriger :

```text
labelKey -> label_key
messageKey -> message_key
```

Zones connues :

```text
dashboard.cashier.alerts.items
dashboard.cashier.quick_sale
dashboard.cashier.quick_actions.actions
dashboard.tenant_admin.readiness.topIssues
dashboard.tenant_admin.quick_actions.actions
dashboard.superadmin.quick_actions.actions
```

## 3. Remplacer `route` / `href`

Standard retenu :

```text
path
```

Ou, quand c’est mieux typé :

```text
NavigationDestination
```

À corriger :

```text
CheckTicketWidget.props.route -> path
PublicDrawResultsWidget.props.more_route -> moreDestination ou more_path
TchalaPayload.href -> path ou NavigationDestination
readiness.topIssues.route -> path ou destination typée
```

## 4. Corriger `quick_sale.path`

Vérifier et remplacer :

```text
/cashier/sell
```

par probablement :

```text
/app/cashier/sale
```

## 5. Harmoniser les file_key privés

Nom canonique retenu :

```text
private_shell_cashier
private_shell_tenant_admin
private_shell_super_admin
```

À faire :

```text
private_shell_tenantadmin -> private_shell_tenant_admin
private_shell_superadmin -> private_shell_super_admin
```

Garder les anciens noms temporairement seulement comme compatibilité si nécessaire, mais les nouveaux PageModels doivent pointer vers les noms canoniques.

---

# P1 — Contrat widgets dynamiques

## 6. Public HeroWidget

Migrer :

```text
image_url / image_alt_key
```

vers :

```text
ImageRef
```

Migrer :

```text
cta_primary / cta_secondary / cta_tertiary
```

vers :

```text
actions[]
```

## 7. PlansWidget

Ne pas exposer :

```json
"features": {}
```

ou des feature flags internes.

Utiliser un tableau marketing typé :

```json
"features": [
  {
    "id": "mobile_pos",
    "label_key": "plan.feature.mobile_pos",
    "included": true
  }
]
```

## 8. PublicDrawResultsWidget

Appliquer `max_slots` côté backend.

Règle :

```text
Le backend retourne un payload déjà prêt à afficher.
Le frontend ne doit pas deviner combien de slots couper.
```

Donc si :

```json
"max_slots": 6
```

alors `dynamic.widgets.home.draws.slots` doit contenir maximum 6 items.

## 9. PlatformHealthWidget

Remplacer la map :

```json
"components": {
  "db": "UP",
  "redis": "UP"
}
```

par une liste typée :

```json
"components": [
  {
    "id": "db",
    "label_key": "health.db",
    "status": "UP"
  },
  {
    "id": "redis",
    "label_key": "health.redis",
    "status": "UP"
  }
]
```

## 10. KPI payloads

Éviter les champs plats différents par dashboard :

```json
{
  "salesToday": 0,
  "ticketCountToday": 0,
  "activeSessions": 0
}
```

Préférer un tableau typé pour `KpiGridWidget` :

```json
{
  "items": [
    {
      "id": "salesToday",
      "label_key": "kpi.sales_today",
      "value": 0,
      "format": "money",
      "currency": "USD",
      "trend": null
    }
  ]
}
```

## 11. Summary widgets

Pour `CommercialSummaryWidget` et `OperationsSummaryWidget`, éviter :

```json
{
  "gamesPricing": {},
  "drawChannels": {},
  "limits": {}
}
```

Préférer :

```json
{
  "sections": [
    {
      "id": "gamesPricing",
      "label_key": "summary.games_pricing",
      "status": "READY",
      "count": 7,
      "path": "/app/admin/games-pricing"
    }
  ]
}
```

---

# P1 — Fragments et validation

## 12. Ajouter `fragment_type` + `schema_version`

Tous les fragments critiques doivent avoir :

```json
{
  "fragment_type": "PrivateShell",
  "schema_version": 2
}
```

Types attendus :

```text
PublicHeader
PublicFooter
PrivateShell
HeroPayload
FeatureGridPayload
TchalaPayload
QuickActionsPayload
```

## 13. Créer une registry de fragments typés

But : éviter le chargement brut `Map<String, Object>`.

À prévoir :

```text
JsonFragmentRegistry.load(fileKey, PrivateShell.class)
JsonFragmentRegistry.load(fileKey, PublicHeader.class)
JsonFragmentRegistry.load(fileKey, HeroPayload.class)
```

Le provider `json_file` ne doit pas retourner directement une Map brute pour les fragments shell.

## 14. Validation au démarrage

Ajouter un startup validator qui lit tous les fragments embarqués.

Règle :

```text
Si private_shell_tenant_admin ne matche pas PrivateShell -> fail fast.
Si public_header_links ne matche pas PublicHeader -> fail fast.
Si un file_key référencé n’existe pas -> fail fast.
```

À valider :

```text
fragment_type
schema_version
shape attendu
champs obligatoires
collections présentes avec []
null conservés sur champs optionnels
```

## 15. Snapshot tests

Ajouter des snapshot tests sur :

```text
public_shell_home
private_shell_cashier
private_shell_tenant_admin
private_shell_super_admin
```

Ou selon les noms de fichiers :

```text
public.home
private.dashboard.cashier.web
private.dashboard.tenant_admin
private.dashboard.superadmin
```

## 16. Tests de non-régression navigation privée

Ajouter un test :

```text
Aucun fragment privé ne contient primary/secondary comme navigation principale de header.
```

Les surfaces privées doivent utiliser :

```text
topAppBar
navigationDrawer
footer:null
```

La navigation principale doit être uniquement dans :

```text
navigationDrawer.topDestinations
navigationDrawer.sections
navigationDrawer.footerDestinations
```

---

# P2 — Nettoyage / généralisation

## 17. Décider officiellement `footer.social`

Décider si `footer.social` fait partie du contrat `ShellFooter`.

Si oui, ajouter au schema :

```json
"social": []
```

Si non, déplacer dans :

```text
footer.secondary
```

ou dans un widget/footer block dédié.

## 18. navigationDrawer.brand.label

Pour tenant admin, éviter un brand sans nom affichable.

Actuellement possible :

```json
"label_key": null,
"label": null,
"subtitle_key": "surface.tenant_admin"
```

À corriger selon contexte :

```text
Si tenantName connu -> label = "Tchalanet"
Sinon -> label_key = "app.name"
```

## 19. Notifications top-level

Le top-level :

```json
"notifications": {}
```

est OK pour les pages privées, mais doit rester protégé par :

```text
surface
tenant
role
user
```

À vérifier :

```text
cashier -> notifications opérationnelles cashier uniquement
tenant admin -> notifications tenant admin uniquement
superadmin -> notifications plateforme uniquement
public -> aucune notification privée
```

## 20. Tenants / subscriptions dashboard

Optionnel mais recommandé si on veut généraliser les widgets :

```text
dashboard.superadmin.tenants
dashboard.superadmin.subscriptions
```

peuvent migrer vers des arrays KPI typés.

Exemple :

```json
{
  "items": [
    {
      "id": "active",
      "label_key": "dashboard.superadmin.tenants.active",
      "value": 1,
      "format": "number",
      "trend": null
    }
  ]
}
```

---

# P2 — Nettoyage legacy

## 21. Supprimer les anciens fallbacks runtime classpath

À supprimer/déprécier côté `core.pagemodel` :

```text
PageModelTemplateLoaderPort
ClasspathPageModelTemplateLoader
```

Règle :

```text
Classpath JSON = seed seulement.
Runtime PageModel = DB seulement.
```

Résolution runtime :

```text
tenant courant
-> tenant default
-> PAGE_MODEL_NOT_FOUND
```

## 22. Nettoyer les anciens fichiers de compatibilité

Fichiers de compatibilité possibles à garder temporairement :

```text
private_shell_tenantadmin
private_shell_superadmin
private_header_tenantadmin
private_sidebar_tenantadmin
private_header_tenant_superadmin
private_sidebar_superadmin
```

Mais les nouveaux PageModels doivent utiliser :

```text
private_shell_tenant_admin
private_shell_super_admin
```

---

# Done / déjà aligné

```text
- PrivateShell contient topAppBar + navigationDrawer + footer:null
- Top app bar privée ne porte pas la navigation principale
- Navigation principale privée vit dans navigationDrawer
- PublicHeader/PublicFooter utilisent brand/primary/secondary/actions
- ImageRef existe dans les fragments récents
- NavigationDestination est utilisé dans les nouveaux fragments shell
- level=GLOBAL remplace is_system pour les templates
- scope + slug doivent être persistés dans page_model_template
```
