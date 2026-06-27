# Super admin operations

## Objectif

Cette page décrit les surfaces operateur disponibles pour un super admin dans le portail web.
Elle ne remplace pas les conventions backend : les details techniques restent dans
`tchalanet-server/docs/conventions/batch/` et `tchalanet-server/docs/conventions/context/`.

## Mode support tenant

Le mode support tenant permet a un super admin d'ouvrir l'espace admin d'un tenant sans changer
son identite reelle.

Flux attendu :

1. Le super admin selectionne un tenant depuis `Plateforme > Tenants` ou `Plateforme > Support tenant`.
2. Il saisit une raison de support.
3. Le portail ouvre `/app/admin` avec la navigation tenant admin.
4. Les appels `/admin/**` recoivent les headers de contexte support.

Headers envoyes par le front :

| Header | Role |
|---|---|
| `X-Tch-Tenant-Override` | Tenant cible du support |
| `X-Tch-Act-As` | Surface operationnelle demandee (`TENANT_ADMIN`) |
| `X-Tch-Override-Reason` | Justification/audit de la session support |

Points importants :

- le super admin reste `SUPER_ADMIN` dans le token ;
- le guard frontend accepte `/app/admin` uniquement si une session support locale est active ;
- le `TchBackendClient` sait ajouter des headers explicites via `asTenantAdmin`, et l'intercepteur
  support les ajoute automatiquement aux appels `/admin/**` pendant la session ;
- quitter le mode support vide la session locale et redirige vers l'espace plateforme ;
- les actions sensibles doivent conserver une raison et une trace d'audit fonctionnel.

## Jobs Spring Batch vs endpoints ops

Spring Batch est le moteur unique des traitements operationnels recurrents ou rejouables.
Les endpoints ops metier restent utiles comme UX guidee, mais ils lancent les jobs Spring Batch
quand l'action existe deja comme job/scheduler.

Les commandes directes sont reservees aux actions humaines ciblees : manual result, override,
confirm, clear cache, ou action explicite sur une selection de draws.

Regle pratique :

| Besoin super admin | Surface recommandee |
|---|---|
| Voir les tirages, filtrer par tenant/date/statut/slot | `Operations > Tirages` |
| Annuler/verrouiller/deverrouiller/regler/archiver des tirages selectionnes | `Operations > Tirages` |
| Entrer un resultat manuel ou override un resultat existant | `Operations > Resultats` |
| Fetch ponctuel de resultats externes | `Operations > Resultats` |
| Lancer/rejouer une execution technique avec parametres JSON | `Operations > Jobs` |
| Inspecter historique, statut et contexte d'execution Spring Batch | `Operations > Jobs` |
| Activer/desactiver une gate | A garder pour la page gates/cache dediee, pas l'ecran Jobs V0 |

Donc l'ecran Jobs ne doit pas devenir un formulaire metier pour tout. Il expose le registre Spring
Batch, le demarrage manuel, l'historique et le restart. Les actions metier frequentes restent dans
les pages `Tirages` et `Resultats`, mais retournent des `executionId` lorsqu'elles lancent un job.

`Refresh` est volontairement absent en V0 : `fetch` cree/met a jour les `draw_result` globaux,
`apply` rattache ensuite ces resultats aux draws tenant-scopes.

## Parametres JSON pour les jobs

Le dialogue `Demarrer le job` attend un objet JSON avec des cles `snake_case`.
Le runtime ajoute automatiquement `request_id`, `actor` ou `ts` quand ils sont absents.

### Jobs tenant-scoped

Les jobs de portee `TENANT` exigent `tenant_id`.

Exemple minimal :

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000003"
}
```

Exemple avec dry run :

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000003",
  "dry_run": "true"
}
```

### `draw:lifecycle:generate`

Genere des tirages pour un tenant.

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000003",
  "days_ahead": "7",
  "dry_run": "true"
}
```

Utiliser plutot `Operations > Tirages > Generer` quand l'operateur veut une action guidee par date
ou par liste de tenants.

### `draw:lifecycle:open`

Ouvre les tirages dans une fenetre temporelle pour un tenant.

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000003",
  "date": "2026-06-27",
  "max_items": "100",
  "dry_run": "true"
}
```

### `draw:lifecycle:close`

Ferme les tirages dans une fenetre temporelle pour un tenant.

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000003",
  "max_items": "100",
  "dry_run": "true"
}
```

### `draw:lifecycle:settle`

Regle les tirages ayant des resultats.

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000003",
  "date": "2026-06-27",
  "days_back": "1",
  "max_draws": "100",
  "dry_run": "true"
}
```

`force=true` doit rester exceptionnel et audite.

### `results:external:fetch`

Recupere les resultats externes. Portee globale : pas de `tenant_id`.

```json
{
  "date": "2026-06-27",
  "slot_key": "FL_EVE",
  "max_slots": "20",
  "dry_run": "true"
}
```

Pour une action quotidienne simple, utiliser plutot `Operations > Resultats > Fetch`.

### `results:external:apply`

Applique les resultats fetches aux tirages d'un tenant.

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000003",
  "date": "2026-06-27",
  "slot_key": "FL_EVE",
  "days_back": "1",
  "max_slots": "20",
  "dry_run": "true"
}
```

Pour une action operateur guidee, utiliser plutot `Operations > Resultats > Refresh` ou
`Operations > Tirages > Appliquer`.

### `catalog:search:reindex`

Reconstruit l'index de recherche catalogue. Portee globale.

```json
{
  "full_rebuild": "true",
  "max_items": "1000"
}
```

## Audit attendu

Les points a verifier dans l'audit fonctionnel :

| Action | Trace attendue |
|---|---|
| Ouverture support tenant | acteur reel super admin, tenant cible, raison |
| Action `/admin/**` pendant support | acteur reel super admin, tenant cible, mode support |
| Action lifecycle sur tirage | ids de tirages, raison, tenant cible |
| Override/resultat manuel | slot, date, valeurs, raison |
| Job manuel | job key, params utiles, actor/request id, dry run/force |

Si une action ne peut pas etre expliquee avec ces champs, elle doit etre corrigee avant d'etre
consideree exploitable en production.
