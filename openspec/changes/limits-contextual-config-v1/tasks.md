# tasks — limits-contextual-config-v1

## Slice 1 — Backend : endpoint exposure-alerts

- [ ] Ajouter `GET /admin/policies/limits/exposure-alerts` dans `LimitPolicyAdminController`
  - params : `drawId` (UUID), `scope` (DRAW_CHANNEL), `targetId` (channelId), `limit` (int, défaut 10)
  - réutilise `GetExposureAlertsOverviewQuery` + handler existant
- [ ] Ajouter le call dans `AdminLimitsApi` côté web (`getExposureAlerts(drawId, channelId, limit)`)

## Slice 2 — Web : composant LimitPolicyBlockComponent

- [ ] Créer `libs/ui/console/src/lib/limit-policy-block/` (ou feature admin)
  - un champ nullable par ruleKey TICKET (mise max par ligne, max par ticket, max lignes)
  - un champ nullable pour BLOCK_BET_TYPE
  - valeur héritée affichée en grisé quand champ vide (`= hérite : 500 HTG`)
  - emits `LimitPolicyOverride` (map ruleKey → valeur | null)
- [ ] Charger les specs depuis `api.listRules()` pour libellés/paramsTemplate

## Slice 3 — Web : intégration dans Setup tenant config

- [ ] Ajouter section "Limites par défaut" dans la page setup/config tenant
  - scope TENANT
  - load : `api.listAssignments('TENANT')`
  - save : upsert/delete par champ modifié

## Slice 4 — Web : intégration dans Draw Channel config

- [ ] Ajouter section "Limites" dans la page draw channel detail/edit
  - scope DRAW_CHANNEL, targetId = channelId
  - afficher valeur héritée du tenant en grisé
  - load : `api.listAssignments('DRAW_CHANNEL', channelId)`
  - save : upsert/delete par champ modifié

## Slice 5 — Web : intégration dans Seller Terminal

- [ ] Ajouter section "Limites" dans la page seller terminal création/édition
  - scope SELLER_TERMINAL, targetId = terminalId
  - ruleKeys : TICKET uniquement (pas EXPOSURE — pas de projection terminal)
  - afficher valeur héritée du tenant en grisé

## Slice 6 — Web : bouton "Bloke nimero" dans le détail tirage

- [ ] Ajouter bouton "Bloke nimero" dans le header d'actions de `AdminDrawDetailPage`
  - visible seulement si le draw est OPEN
  - ouvre `BlockNumberQuickDialogComponent` avec `drawChannelId` pré-sélectionné
  - masquer le picker de tirage dans le dialog quand channelId est fourni en entrée
- [ ] Ajouter `channelId?: string` comme input optionnel dans `BlockNumberQuickDialogComponent`

## Slice 7 — Web : widget "Numéros à risque" dans le détail tirage

- [ ] Ajouter section "Numéros à risque" dans `DrawDetailActivityComponent`
  - appel `getExposureAlerts(drawId, channelId, 10)`
  - masqué si aucune règle `MAX_STAKE_EXPOSURE` n'est configurée pour ce channel (ratio null)
  - afficher chips numéros avec barre de progression ou couleur (vert → orange → rouge)
  - bouton "Bloquer" à côté de chaque numéro à risque (ouvre quick dialog pré-rempli)

## Slice 8 — Web : simplification menu limits

- [ ] Retirer `global` et `draw` du sidenav `TENANT_ADMIN_NAVIGATION` (garder les routes actives)
- [ ] Menu limits : garder `overview` (lecture seule audit) + `number` (bloke nimero)
- [ ] Ajouter lien "Vue avancée →" sur la page overview vers `/limits/draw` pour accès admin

## Slice 9 — i18n

- [ ] Ajouter clés i18n pour `LimitPolicyBlockComponent` (fr/en/ht)
- [ ] Ajouter clés pour "Numéros à risque" widget (fr/en/ht)
- [ ] Ajouter clés pour le bouton "Bloke nimero" dans le détail tirage (fr/en/ht)
