# Tchalanet — Draw Results Tick Decision Barriers

## Objectif

Ce document sert de guide de décision pour comprendre **pourquoi un fetch/apply de résultats se lance ou ne se lance pas**.

Il couvre :

- scheduler actif ou non ;
- tick courant actif ou bloqué ;
- properties vs settings DB ;
- batch gates ;
- cooldown provider/slot ;
- fenêtres de dates/heures ;
- appel manuel forcé par `slotKey` et `drawTime`.

---

# 1. Résumé mental simple

Le pipeline results ne doit pas être vu comme “un batch qui appelle une API”.

Il faut le voir comme une chaîne de barrières :

```text
Scheduler enabled ?
   ↓
Global results active ?
   ↓
Batch gate enabled ?
   ↓
Tick autorisé maintenant ?
   ↓
Slot actif ?
   ↓
Provider actif ?
   ↓
Fenêtre temporelle valide ?
   ↓
Cooldown OK ?
   ↓
Fetch provider
   ↓
Upsert draw_result global
   ↓
Apply tenant
   ↓
Draw CLOSED → RESULTED
```

Si une seule barrière bloque, il ne faut pas appeler l’API externe.

---

# 2. Les 3 modes d’exécution

## 2.1 Scheduler automatique

Utilisé en staging/prod.

```text
cron → tick → resolve slots due → fetch/apply
```

Le scheduler doit être prudent :

- respecter les gates ;
- respecter cooldown ;
- respecter les fenêtres ;
- limiter max slots ;
- éviter de spammer les providers.

## 2.2 Ops manuel standard

Utilisé pour debug contrôlé.

```text
POST /ops/draw-results/fetch
POST /ops/draw-results/apply
```

`refresh` est désactivé en V0 : il mélange `fetch` global des `draw_result` et `apply`
tenant-scoped vers les draws. Lancer explicitement les deux jobs si nécessaire.

Il doit encore respecter :

- properties globales ;
- gates ;
- max limits ;
- `dryRun`;
- audit avec `reason`.

## 2.3 Ops manuel forcé

Utilisé pour corriger un cas précis.

```text
force slotKey + drawDate/drawTime
```

Il peut bypasser certaines barrières, mais pas toutes.

Exemple :

```json
{
  "slotKey": "NY_MID",
  "drawDate": "2026-05-01",
  "drawTime": "14:30",
  "timezone": "America/New_York",
  "force": true,
  "dryRun": false,
  "reason": "debug provider NY_MID for specific draw"
}
```

---

# 3. Properties recommandées

## 3.1 Global results

```yaml
tch:
  draw:
    results:
      active: true
```

Décision :

| Valeur  | Effet                         |
| ------- | ----------------------------- |
| `true`  | Pipeline results autorisé     |
| `false` | Aucun fetch/apply automatique |

En local, tu peux mettre :

```yaml
tch.draw.results.active=true
```

mais désactiver seulement le scheduler.

---

## 3.2 Scheduler

```yaml
tch:
  draw:
    results:
      scheduler:
        enabled: false
        tick-cron: '0 */5 * * * *'
```

Décision :

| Property            | Rôle                             |
| ------------------- | -------------------------------- |
| `enabled`           | active/désactive le scheduler    |
| `tick-cron`         | fréquence du tick                |
| `lock-at-most-for`  | durée max lock ShedLock          |
| `lock-at-least-for` | évite double tick trop rapproché |

Recommandation locale :

```yaml
enabled: false
```

Recommandation staging/prod :

```yaml
enabled: true
tick-cron: '0 */5 * * * *'
```

---

## 3.3 Limits

```yaml
tch:
  draw:
    results:
      limits:
        hard-max-slots: 40
        hard-days-back: 7
```

Décision :

| Property         | Rôle                           |
| ---------------- | ------------------------------ |
| `hard-max-slots` | plafond absolu, même en manuel |
| `hard-days-back` | empêche un replay trop large   |

Le handler doit toujours faire :

```text
effectiveMaxSlots = min(command.maxSlots, hardMaxSlots)
effectiveDaysBack = min(command.daysBack, hardDaysBack)
```

---

## 3.4 Defaults

```yaml
tch:
  draw:
    results:
      defaults:
        max-slots: 10
        days-back: 2
```

Utilisé quand le scheduler ou l’Ops request ne précise rien.

---

## 3.5 Cooldown

```yaml
tch:
  draw:
    results:
      cooldown:
        enabled: true
        per-slot-minutes: 10
        bypass-when-force: true
```

Décision :

| Cas                      | Cooldown appliqué ?                                           |
| ------------------------ | ------------------------------------------------------------- |
| scheduler automatique    | oui                                                           |
| Ops manuel `force=false` | oui                                                           |
| Ops manuel `force=true`  | peut être bypassé                                             |
| dryRun                   | non obligatoire, mais recommandé de ne pas écrire le cooldown |

---

# 4. Settings DB / Batch gates

Les settings DB servent à couper rapidement un job sans redéployer.

## 4.1 Settings recommandés

| Namespace | Setting key | Scope | Default | Description |
| --- | --- | --- | ---: | --- |
| `batch` | `jobs.results:external:fetch.enabled` | global | `true` | Autorise fetch global |
| `batch` | `jobs.results:external:apply.enabled` | tenant | `true` | Autorise apply tenant |
| `batch` | `jobs.draw:lifecycle:open.enabled` | tenant | `true` | Autorise open due |
| `batch` | `jobs.draw:lifecycle:close.enabled` | tenant | `true` | Autorise close due |
| `batch` | `jobs.draw:lifecycle:settle.enabled` | tenant | `true` | Autorise settlement |

## 4.2 Ordre de décision gate

```text
property active ?
   ↓
scheduler enabled ?
   ↓
batch key allowlisted ?
   ↓
global setting enabled ?
   ↓
tenant setting enabled ? si tenant-scoped
   ↓
cooldown ?
```

## 4.3 Règle importante

Les settings DB ne doivent pas remplacer les properties.

Ils servent à piloter runtime.

```text
properties = garde-fou de déploiement
settings DB = contrôle opérationnel
command input = demande ponctuelle
```

---

# 5. Current tick : décision détaillée

Un tick ne doit pas appeler l’API directement.

Il doit d’abord produire une liste de `FetchCandidate`.

## 5.1 Exemple de pseudo-code

```java
void tick() {
  if (!props.results.active()) return;
  if (!props.scheduler.enabled()) return;
  if (!batchGate.isEnabled(RESULTS_EXTERNAL_FETCH)) return;

  Instant now = clock.instant();

  List<ResultSlot> slots = resultSlotCatalog.findActiveSlots();

  List<FetchCandidate> candidates = dueSlotResolver.resolve(
      slots,
      now,
      props.defaults.daysBack(),
      props.defaults.maxSlots()
  );

  for (FetchCandidate candidate : candidates) {
    if (cooldown.isActive(candidate.slotKey(), candidate.occurredAt())) {
      continue;
    }

    fetchSlot(candidate);
    cooldown.markFetched(candidate.slotKey(), candidate.occurredAt());
  }
}
```

---

# 6. Résolution des dates/heures

## 6.1 Source de vérité

Pour un slot donné :

```text
result_slot.slot_key
result_slot.timezone
result_slot.draw_time
```

Exemple :

```text
slotKey = NY_MID
timezone = America/New_York
drawTime = 14:30
drawDate = 2026-05-01
```

Alors :

```text
occurredAt = 2026-05-01T14:30:00 America/New_York
```

converti en `Instant`.

## 6.2 Règle

Le provider peut retourner une date/heure, mais pour le matching interne, la source de vérité MVP doit être :

```text
result_slot.draw_time + result_slot.timezone + drawDate
```

Sinon apply risque de ne pas retrouver le draw.

---

# 7. Fenêtre de fetch automatique

Le scheduler ne doit fetcher qu’un slot “probablement disponible”.

Exemple de config :

```yaml
tch:
  draw:
    results:
      window:
        min-after-draw-minutes: 3
        max-after-draw-hours: 24
        include-yesterday: true
```

Décision :

| Condition              | Action                       |
| ---------------------- | ---------------------------- |
| now < drawTime + 3 min | trop tôt, skip               |
| now > drawTime + 24h   | trop vieux, skip automatique |
| yesterday enabled      | inclure hier pour rattrapage |
| force=true             | peut bypasser la fenêtre max |

---

# 8. Cooldown par slot

## 8.1 Clé recommandée

```text
drawresult:fetch:cooldown:{provider}:{slotKey}:{occurredAt}
```

Exemple :

```text
drawresult:fetch:cooldown:NY:NY_MID:2026-05-01T18:30:00Z
```

## 8.2 Pourquoi inclure occurredAt ?

Parce que sinon tu bloques `NY_MID` globalement, même pour une autre journée.

## 8.3 Stockage MVP

| Option    | Recommandation           |
| --------- | ------------------------ |
| In-memory | OK local/dev             |
| Redis     | recommandé staging/prod  |
| DB        | possible mais plus lourd |

---

# 9. Fetch forcé par slotKey + drawTime

## 9.1 Pourquoi ce mode est nécessaire

Il permet de tester/corriger :

- un provider précis ;
- un slot précis ;
- une date précise ;
- un mapping pick3/pick4 précis ;
- une correction après bug de timezone.

## 9.2 Commande recommandée

```json
{
  "slotKey": "NY_MID",
  "drawDate": "2026-05-01",
  "drawTime": "14:30",
  "timezone": "America/New_York",
  "force": true,
  "dryRun": true,
  "reason": "debug exact NY_MID result"
}
```

## 9.3 Variante sans drawTime

Si `drawTime` et `timezone` existent dans `result_slot`, la commande peut être plus simple :

```json
{
  "slotKey": "NY_MID",
  "drawDate": "2026-05-01",
  "force": true,
  "dryRun": true,
  "reason": "debug exact NY_MID result"
}
```

Le handler résout alors :

```text
drawTime = result_slot.draw_time
timezone = result_slot.timezone
occurredAt = drawDate + drawTime + timezone
```

## 9.4 Règle force

| Barrière            | Bypass avec force ?                                    |
| ------------------- | ------------------------------------------------------ |
| scheduler.enabled   | non applicable en manuel                               |
| results.active      | non, sauf endpoint SUPER_ADMIN spécial                 |
| batch gate          | non recommandé                                         |
| cooldown            | oui                                                    |
| max date window     | oui                                                    |
| hard-max-slots      | non                                                    |
| provider disabled   | non par défaut                                         |
| slot inactive       | non par défaut, sauf `includeInactive=true` superadmin |
| overwrite CONFIRMED | non sauf `force=true + reason + audit`                 |

---

# 10. Apply forcé par slotKey

## 10.1 Commande recommandée

```json
{
  "tenantId": "TENANT_UUID",
  "slotKey": "NY_MID",
  "drawDate": "2026-05-01",
  "force": true,
  "dryRun": true,
  "reason": "apply exact NY_MID result after manual fetch"
}
```

## 10.2 Matching attendu

Apply doit trouver :

```text
draw tenant:
  status = CLOSED
  draw_channel.result_slot_id = result_slot.id
  draw.draw_date = command.drawDate

draw_result global:
  result_slot_id = result_slot.id
  occurred_at = computedOccurredAt
```

Puis :

```text
draw.draw_result_id = draw_result.id
draw.status = RESULTED
```

---

# 11. Refresh forcé

## 11.1 Commande recommandée

```json
{
  "tenantId": "TENANT_UUID",
  "slotKey": "NY_MID",
  "drawDate": "2026-05-01",
  "force": true,
  "dryRun": true,
  "reason": "manual refresh exact slot/date"
}
```

## 11.2 Décision

`refresh` doit être explicite :

```text
refresh = fetch global exact slot/date + apply tenant exact slot/date
```

Éviter un refresh flou qui refetch trop large.

---

# 12. Requêtes SQL de debug

## 12.1 Vérifier slot

```sql
select
  id,
  slot_key,
  provider,
  timezone,
  draw_time,
  active,
  source_cfg,
  projection_cfg
from result_slot
where slot_key = 'NY_MID';
```

## 12.2 Vérifier draws tenant pour ce slot

```sql
select
  d.id,
  d.tenant_id,
  d.status,
  d.draw_date,
  d.scheduled_at,
  d.cutoff_at,
  d.draw_result_id,
  dc.code as draw_channel_code,
  dc.result_slot_id,
  rs.slot_key
from draw d
join draw_channel dc on dc.id = d.draw_channel_id
join result_slot rs on rs.id = dc.result_slot_id
where rs.slot_key = 'NY_MID'
  and d.draw_date = date '2026-05-01'
order by d.scheduled_at;
```

## 12.3 Vérifier draw_result global

```sql
select
  dr.id,
  rs.slot_key,
  dr.result_slot_id,
  dr.occurred_at,
  dr.status,
  dr.source_hash,
  dr.source_result,
  dr.created_at,
  dr.updated_at
from draw_result dr
join result_slot rs on rs.id = dr.result_slot_id
where rs.slot_key = 'NY_MID'
order by dr.occurred_at desc;
```

## 12.4 Vérifier mismatch apply

```sql
select
  d.id as draw_id,
  d.draw_date,
  d.status as draw_status,
  d.draw_result_id,
  rs.slot_key,
  rs.draw_time,
  rs.timezone,
  dr.id as result_id,
  dr.occurred_at,
  dr.status as result_status
from draw d
join draw_channel dc on dc.id = d.draw_channel_id
join result_slot rs on rs.id = dc.result_slot_id
left join draw_result dr
  on dr.result_slot_id = rs.id
where rs.slot_key = 'NY_MID'
  and d.draw_date = date '2026-05-01';
```

---

# 13. Logs indispensables

## 13.1 Tick

```text
drawresult.tick started now={} schedulerEnabled={} active={} maxSlots={} daysBack={}
drawresult.tick blocked reason={}
drawresult.tick candidates count={} sample={}
```

## 13.2 Candidate resolver

```text
drawresult.candidate slot={} date={} drawTime={} timezone={} occurredAt={} due={} reason={}
```

## 13.3 Cooldown

```text
drawresult.cooldown check provider={} slot={} occurredAt={} active={}
drawresult.cooldown bypass provider={} slot={} occurredAt={} reason=force
drawresult.cooldown mark provider={} slot={} occurredAt={} ttl={}
```

## 13.4 Fetch exact

```text
drawresult.fetch.exact slot={} provider={} date={} occurredAt={} force={} dryRun={}
drawresult.fetch.provider_response slot={} found={} rawHash={}
drawresult.fetch.upsert resultId={} status={} sourceHash={}
```

## 13.5 Apply exact

```text
drawresult.apply.exact tenant={} slot={} date={} occurredAt={} dryRun={}
drawresult.apply.match drawId={} resultId={}
drawresult.apply.updated drawId={} status=RESULTED
```

---

# 14. Décisions recommandées pour Tchalanet

## 14.1 Local

```yaml
tch:
  draw:
    results:
      active: true
      scheduler:
        enabled: false
      cooldown:
        enabled: false
```

Utiliser Ops manuel.

## 14.2 Staging

```yaml
tch:
  draw:
    results:
      active: true
      scheduler:
        enabled: true
        tick-cron: '0 */10 * * * *'
      cooldown:
        enabled: true
        per-slot-minutes: 10
```

## 14.3 Prod

```yaml
tch:
  draw:
    results:
      active: true
      scheduler:
        enabled: true
        tick-cron: '0 */5 * * * *'
      cooldown:
        enabled: true
        per-slot-minutes: 10
```

---

# 15. Règle finale

Pour éviter la confusion, toujours distinguer :

```text
scheduler = quand le système essaie automatiquement
gate = est-ce que le job a le droit de tourner
window = est-ce que le slot est temporellement éligible
cooldown = est-ce qu’on a appelé trop récemment
force = demande manuelle exceptionnelle
dryRun = simulation sans écriture
```

Un appel manuel précis devrait toujours permettre :

```text
slotKey + drawDate → occurredAt déterministe → fetch → draw_result → apply
```

Sans dépendre du tick automatique.
