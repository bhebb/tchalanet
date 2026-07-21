# Validation résultats providers — DrawResult / Draw

Ce guide sert de checklist opérationnelle pour valider le pipeline résultat:

`provider confirmé → résultat provisoire si politique de confiance → confirmation Ops → notification tenant/reminder → apply → settle`

## Règles de base

- Pour les nouveaux tenants Haïti, seuls les channels New York et Florida sont actifs par défaut: `HT_NY_MID`, `HT_NY_EVE`, `HT_FL_MID`, `HT_FL_EVE`.
- Les autres providers supportés sont provisionnés comme channels disponibles mais inactifs. Le client ou l'admin plateforme doit les activer explicitement.
- Un provider automatique ne veut pas dire qu'on peut payer tout de suite. Si la trust policy crée un résultat `PROVISIONAL`, Ops doit le confirmer avant l'apply.
- L'apply lie seulement les résultats `CONFIRMED` ou `OVERRIDDEN` au draw tenant. Le settle utilise ensuite ce résultat confirmé pour traiter les tickets.
- Les anciens tenants ne sont pas changés automatiquement par cette règle. La politique NY/FL par défaut concerne le provisioning des nouveaux tenants.

## Providers supportés

| Provider | Slots Haïti | Fetch auto | Actif par défaut nouveau tenant | Activation client/admin |
|---|---|---:|---:|---|
| NY | `NY_MID`, `NY_EVE` | Oui | Oui | Rien à activer côté tenant. Vérifier `TCH_US_NY_LOTTERY_ENABLED`, base URL, token app et headers. |
| FL | `FL_MID`, `FL_EVE` | Oui | Oui | Rien à activer côté tenant. Vérifier `TCH_US_FL_LOTTERY_ENABLED`, base URL, `x-partner`, origin/referer. |
| GA | `GA_MID`, `GA_EVE`, `GA_LATE` | Oui | Non | Activer les channels tenant voulus et vérifier `TCH_US_GA_LOTTERY_ENABLED`. |
| TX | `TX_1000`, `TX_1227`, `TX_1800`, `TX_2212` | Oui | Non | Activer les channels tenant voulus et vérifier `TCH_US_TX_LOTTERY_ENABLED`. |
| PA | `PA_MID`, `PA_EVE` | Oui | Non | Activer les channels tenant voulus et vérifier `TCH_US_PA_LOTTERY_ENABLED`. |
| NJ | `NJ_MID`, `NJ_EVE` | Oui | Non | Activer les channels tenant voulus et vérifier `TCH_US_NJ_LOTTERY_ENABLED`. |
| CA | `CA_MID`, `CA_EVE` | Oui | Non | Activer les channels tenant voulus et vérifier `TCH_US_CA_LOTTERY_ENABLED`. |
| OH | `OH_MID`, `OH_EVE` | Oui, avec auth | Non | Activer les channels tenant voulus. Vérifier auth Ohio ou `TCH_US_OH_BEARER_TOKEN`. |
| MI | `MI_MID`, `MI_EVE` | Oui | Non | Activer les channels tenant voulus et vérifier headers GraphQL Michigan. |
| MN | `MN_EVE` | Non, manuel | Non | Activer seulement si Ops accepte un résultat manuel. Pick 4 absent, donc projection limitée. |
| TN | `TN_MID`, `TN_EVE` | Non aujourd'hui | Non | Skeleton enum/config seulement: pas de client fetch enregistré; validation manuelle ou développement provider requis. |
| IL | `IL_MID`, `IL_EVE` | Non aujourd'hui | Non | Enum-only: résultat manuel en V0 ou développement provider requis. |
| MO | `MO_MID`, `MO_EVE` | Non aujourd'hui | Non | Enum-only: résultat manuel en V0 ou développement provider requis. |

## Délais automatiques attendus

Le job de processing tourne toutes les 5 minutes. Les délais ci-dessous sont donc exprimés en minutes après l'heure officielle du tirage, avec un décalage possible jusqu'au prochain tick.

À communiquer aux clients: un provider automatique ne publie pas un résultat exactement à l'heure du tirage. Le résultat arrive généralement quelques minutes après; Tchalanet commence à le chercher à partir de +5 minutes.

| Provider | Slots auto | Arrivée résultat après l'heure du tirage | Statut côté client |
|---|---|---:|---|
| NY | `NY_MID`, `NY_EVE` | observé autour de +20 à +30 min | Auto actif par défaut pour nouveau tenant. Peut rester `PROVISIONAL` si revue Ops requise. |
| FL | `FL_MID`, `FL_EVE` | observé autour de +5 à +20 min | Auto actif par défaut pour nouveau tenant. Peut rester `PROVISIONAL` si revue Ops requise. |
| GA | `GA_MID`, `GA_EVE`, `GA_LATE` | observé autour de +6 à +21 min | Auto disponible après activation tenant. |
| TX | `TX_1000`, `TX_1227`, `TX_1800`, `TX_2212` | observé autour de +8 à +23 min | Auto disponible après activation tenant. |
| PA | `PA_MID`, `PA_EVE` | observé autour de +5 à +25 min | Auto disponible après activation tenant. |
| NJ | `NJ_MID`, `NJ_EVE` | observé autour de +6 à +18 min | Auto disponible après activation tenant. |
| CA | `CA_MID`, `CA_EVE` | observé autour de +5 à +20 min | Auto disponible après activation tenant. |
| MI | `MI_MID`, `MI_EVE` | observé autour de +16 à +21 min | Auto disponible après activation tenant. |
| OH | `OH_MID`, `OH_EVE` | à confirmer, aucune arrivée observée localement le 2026-07-20 | Ne pas promettre au client avant validation auth/feed. |
| MN/TN/IL/MO | slots manuels | N/A | Pas de résultat automatique aujourd'hui; saisie Ops/manuelle requise. |

Ces valeurs viennent de l'observation BD locale du `2026-07-20`. Avant de publier un délai contractuel, valider le provider sur plusieurs jours et noter le délai médian par slot.

| Étape | Début | Retry | Stop | Attendu en pratique |
|---|---:|---:|---:|---|
| Fetch provider | +5 min après le draw | 10 min | +240 min | Première tentative vers +5 à +10 min. |
| Reminder résultat manuel | +5 min | tick 5 min | N/A | Notification Ops/tenant quand il faut saisir manuellement. |
| Reminder provider overdue | +60 min | tick 5 min | N/A | Alerte si provider automatique ne donne rien. |
| Reminder provisional stuck | +30 min | tick 5 min | N/A | Alerte si un résultat reste `PROVISIONAL`. |
| Apply résultat | +10 min | 5 min | +720 min | Applique seulement `CONFIRMED` ou `OVERRIDDEN`. |
| Settle draw | +10 min | 5 min | +1440 min | Requiert draw `RESULTED`, résultat confirmé et tickets prêts. |

Ces délais ne sont pas des SLA providers. Ils décrivent quand Tchalanet commence à chercher, appliquer et régler, et les temps d'arrivée observés servent à informer le client du délai normal après tirage.

## Qualité et statut du résultat

La qualité (`quality`) décrit si les données provider/projection sont complètes. Le statut (`status`) décrit si le résultat peut être utilisé par le pipeline draw. Un résultat incomplet doit rester visible pour Ops, mais ne doit pas être appliqué automatiquement.

| Situation | Quality | Status attendu | Apply auto | Action Ops |
|---|---|---|---:|---|
| Pick 3 et Pick 4 présents, projection Haïti complète, provider trusted | `COMPLETE` | `CONFIRMED` | Oui | Surveiller seulement. |
| Pick 3 et Pick 4 présents, projection complète, trust policy `REQUIRE_PLATFORM_REVIEW` | `COMPLETE` | `PROVISIONAL` | Non | Confirmer après revue. |
| Pick 4 seul reçu: `lot2`/`lot3` remplis, `lot1`/`lot4` vides | `SUSPECT` | `PROVISIONAL` | Non | Attendre correction provider ou override manuel complet. |
| Pick 3 seul reçu: `lot1`/`lot4` remplis, `lot2`/`lot3` vides | `SUSPECT` | `PROVISIONAL` | Non | Attendre correction provider ou override manuel complet. |
| Pick attendu absent, longueur invalide, projection KO ou qualité provider douteuse | `SUSPECT` ou `INVALID` | `PROVISIONAL` | Non | Corriger/override; ne pas confirmer sans vérification. |
| Override Ops complet | `COMPLETE` | `OVERRIDDEN` | Oui | Audit obligatoire avec raison. |

Règle forte: seul `COMPLETE + CONFIRMED` ou `COMPLETE + OVERRIDDEN` peut être appliqué au draw. `SUSPECT`, `INVALID` et `PROVISIONAL` bloquent l'apply et le settle.

## Contrôle BD des arrivées réelles

La table globale `draw_result` n'est pas RLS tenant. Pour vérifier les arrivées réelles d'une journée:

```sql
select
  rs.provider,
  rs.slot_key,
  rs.timezone,
  to_char(dr.occurred_at at time zone rs.timezone, 'YYYY-MM-DD HH24:MI') as occurred_local,
  to_char(dr.fetched_at at time zone rs.timezone, 'YYYY-MM-DD HH24:MI') as fetched_local,
  round(extract(epoch from (dr.fetched_at - dr.occurred_at)) / 60.0, 1) as minutes_after_draw,
  dr.status,
  dr.source,
  dr.quality,
  dr.haiti_result->>'lot1' as lot1,
  dr.haiti_result->>'lot2' as lot2,
  dr.haiti_result->>'lot3' as lot3,
  dr.haiti_result->>'lot4' as lot4,
  dr.flags->'haiti'->>'projectionReason' as projection_reason,
  to_char(dr.created_at at time zone rs.timezone, 'YYYY-MM-DD HH24:MI') as created_local
from draw_result dr
join result_slot rs on rs.id = dr.result_slot_id
where dr.result_date = date 'YYYY-MM-DD'
order by dr.fetched_at, rs.provider, rs.slot_key;
```

Synthèse utile:

```sql
select
  rs.provider,
  count(dr.id) as results_count,
  min(round(extract(epoch from (dr.fetched_at - dr.occurred_at)) / 60.0, 1)) as min_minutes,
  max(round(extract(epoch from (dr.fetched_at - dr.occurred_at)) / 60.0, 1)) as max_minutes,
  avg(round(extract(epoch from (dr.fetched_at - dr.occurred_at)) / 60.0, 1))::numeric(10, 1) as avg_minutes,
  count(*) filter (where dr.status = 'PROVISIONAL') as provisional_count
from draw_result dr
join result_slot rs on rs.id = dr.result_slot_id
where dr.result_date = date 'YYYY-MM-DD'
group by rs.provider
order by rs.provider;
```

Observation locale du `2026-07-20`: 19 résultats externes sont entrés. Les arrivées constatées étaient entre +5 et +30 minutes après le draw selon les slots. NY evening et FL evening sont restés `PROVISIONAL` sans update ultérieur; Ops doit donc les confirmer avant apply. OH n'avait aucune ligne malgré les slots actifs, donc l'auth/feed Ohio doit être validé avant activation client. MN n'avait aucune ligne, ce qui est attendu car le client est manuel.

## Conditions d'activation d'un provider

Avant de dire qu'un provider est actif pour un tenant, vérifier tous les points:

- Global: `tch.us-lottery.enabled=true`.
- Provider: `tch.us-lottery.providers.<code>.enabled=true` et configuration réseau/auth correcte.
- Slot: `result_slot.active=true`, avec `source_cfg.provider`, `provider_slot_code`, mapping Pick 3 / Pick 4 et trust policy.
- Tenant: `draw_channel.active=true` pour le channel choisi.
- Jeux: `draw_channel_game.enabled=true` pour les jeux tenant vendus sur ce channel.
- Draws: les draws existent pour la période à valider, ou le scheduler `draw:lifecycle:generate` a tourné après activation.
- Gates: `draw:lifecycle:generate`, `draw:lifecycle:open`, `draw:lifecycle:close`, `results:external:fetch`, `results:external:apply`, `draw:lifecycle:settle`, et les gates Ops manual/confirm si nécessaires.

## Validation A — nouveau tenant NY/FL par défaut

Objectif: prouver que le provisioning ne démarre pas tous les providers.

1. Créer ou reprovisionner un tenant standard avec les jeux Haïti.
2. Lire ses `draw_channel` Haïti.
3. Vérifier que les seuls channels actifs sont `HT_NY_MID`, `HT_NY_EVE`, `HT_FL_MID`, `HT_FL_EVE`.
4. Vérifier que GA/TX/PA/NJ/CA/OH/MI/MN/TN/IL/MO existent si provisionnés, mais sont `active=false`.
5. Vérifier que les jeux sont liés, mais que les draws ne seront générés que pour les channels actifs.

## Validation B — provider automatique happy path

À faire avec `NY_MID` ou `FL_MID` en premier.

1. Choisir un draw fermé ou générer un draw test puis le faire passer `OPEN → CLOSED`.
2. Lancer ou attendre `results:external:fetch`.
3. Vérifier qu'un `draw_result` existe pour le `result_slot`.
4. Vérifier la qualité et les lots Haïti. Si le provider donne seulement Pick 4, `lot1`/`lot4` restent vides; si le provider donne seulement Pick 3, `lot2`/`lot3` restent vides. Ces cas sont `SUSPECT` et doivent rester `PROVISIONAL`.
5. Si le status est `PROVISIONAL`, confirmer par Ops ou compléter par override manuel, puis conserver l'audit de confirmation/correction.
6. Attendre ou lancer `results:external:apply`.
7. Vérifier que le draw tenant passe `CLOSED → RESULTED` avec le bon `drawResultId`.
8. Attendre ou lancer `draw:lifecycle:settle`.
9. Vérifier que le draw passe `RESULTED → SETTLED` et que les tickets sont `WON` ou `LOST`.

## Validation C — provider opt-in

À faire avec un provider automatique non actif par défaut, par exemple GA ou TX.

1. Activer un seul channel tenant, par exemple `HT_GA_MID`.
2. Vérifier que le provider global et le `result_slot` correspondant sont actifs.
3. Générer le prochain draw ou vérifier qu'il existe déjà.
4. Rejouer la validation B.
5. Désactiver le channel si le client ne veut pas garder ce provider.

## Validation D — provider manuel ou sans client automatique

À faire avec MN, TN, IL ou MO seulement si le client veut vraiment ce provider.

1. Activer le channel tenant choisi.
2. Fermer un draw test.
3. Vérifier qu'aucun fetch automatique exploitable ne produit de résultat.
4. Saisir le résultat via Ops.
5. Confirmer le résultat si la policy le laisse `PROVISIONAL`.
6. Lancer apply puis settle.
7. Vérifier les statuts finaux et l'audit manuel.

## Validation E — contrôles négatifs

- Un résultat `PROVISIONAL` ne doit pas être appliqué au draw.
- Un channel tenant inactif ne doit pas générer de nouveaux draws.
- Un provider automatique qui ne répond pas doit produire un reminder overdue vers +60 min.
- Un provider manuel doit produire un reminder de saisie vers +5 min.
- Un draw déjà `SETTLED` ne doit pas accepter de correction simple du résultat appliqué.

## Template d'évidence

| Étape | Attendu | Observé | Pass/Fail | Notes |
|---|---|---|---|---|
| Nouveau tenant | Actifs = NY/FL seulement |  |  |  |
| Fetch provider | `draw_result` créé |  |  |  |
| Ops confirm | `PROVISIONAL → CONFIRMED` si requis |  |  |  |
| Notification/reminder | Notification visible/auditée |  |  |  |
| Apply | Draw `CLOSED → RESULTED` |  |  |  |
| Settle | Draw `RESULTED → SETTLED`; tickets traités |  |  |  |

## Contraintes connues

- Les timings providers externes varient. Tchalanet retry, mais ne garantit pas l'heure de publication officielle.
- TN, IL et MO sont supportés comme codes/slots, pas comme fetch automatique actuel.
- MN est volontairement manuel dans le client actuel.
- La seed historique du tenant demo peut avoir plus de channels actifs; ne pas l'utiliser comme preuve de la politique nouveau tenant.
