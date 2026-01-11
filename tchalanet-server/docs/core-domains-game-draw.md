# Core Domains — Game & Draw (Tchalanet)

Ce document décrit les deux domaines cœur : `game` et `draw`. Objectif : que les futurs devs comprennent **quoi mettre où**, comment **ajouter un nouveau jeu**, et comment s’alimentent **les tirages / résultats / payout**.

---

## 0) Vision métier (rappel)

- Vente de jeux haïtiens : Bolet, Maryaj, Numéros, Loto3/4/5.
- NY / FL / GA / TN = sources externes de résultats (pick3/pick4, MID/EVE), **pas** des jeux.
- Le **Draw** est l’unité opérationnelle centrale (ouverture/fermeture, tickets, payout).
- Un Draw référence un `draw_result` quand un résultat externe est disponible/confirmé.
- Le client voit des résultats haïtiens (lots/projections), même si la source est externe.

---

## 1) Domaine `game`

### 1.1 Rôle

Définit ce qui est vendable et comment l’interpréter :
- validation des numéros (digits)
- type de combinaison (SINGLE, EXACT, PAIR_UNORDERED, …)
- description / tri / activation
- catalogue global, indépendant du tenant

### 1.2 Tables principales

- `game` (global) : catalogue (ex: `HT_BOLET`, `HT_LOTO3`)
- `tenant_game` (tenant-scoped) : activation, pricing/odds/multipliers, limites

### 1.3 Pourquoi `tenant_game`

Chaque tenant peut :
- activer/désactiver un jeu
- définir `min_stake` / `max_stake`
- définir pricing via `flags` (odds / multipliers)

> Le pricing vit dans `tenant_game`, pas dans les channels.

### 1.4 Exemples `tenant_game.flags`

- Bolet :

```json
{ "odds": { "lot2": 50, "lot3": 20, "lot4": 10 } }
```

- Loto4 :

```json
{ "multiplier": 5000 }
```

---

## 2) Domaine `draw`

### 2.1 Concepts clefs

- `draw_channel` (tenant-scoped) : un slot planifié (timezone, draw_time, cutoff, days_of_week), `external_provider` (NY|FL|GA|TN), `flags` pour mapping/projection.
  - `flags.source` : comment lire la source externe (pick3/pick4 + MID/EVE).
  - `flags.projection` : comment projeter en lots haïtiens (lot1..lot4).
  - Important : un channel = horaire + source, **pas** un jeu.
- `draw_channel_game` (tenant-scoped) : jointure qui associe jeux × channels (enabled, flags pour overrides).
  - Evite duplication de channels identiques.
  - Permet override par jeu×channel via `flags`.
- `draw` (tenant-scoped, aggregate root) : instance quotidienne d’un channel.
  - Unique sur `(tenant_id, draw_channel_id, draw_date)`.
  - États : SCHEDULED / OPEN / CLOSED / RESULTED / SETTLED / CANCELED.
  - Référence `draw_result_id` quand résultat attaché.
- `draw_result` (global) : résultat externe (vérité indépendante).
  - Unique sur `(provider, provider_slot, occurred_at)`.
  - Contenu : `source_result` (JSON normalisé), `haiti_result` (projection), `status` (PROVISIONAL/FINAL/ERROR), `source` (API/MANUAL/IMPORT), `raw_payload` (audit).

---

## 3) Pourquoi `draw_channel_game` est important (résumé concret)

Sans `draw_channel_game` il faudrait créer un channel par jeu (NY_MID_BOLET, NY_MID_LOTO3, …) — duplication et risques.
Avec `draw_channel_game` : 1 channel = 1 slot (NY_MID), 1 draw / jour / slot, plusieurs games s’appuient sur le même draw.

---

## 4) Cycle de vie (workflow)

### 4.1 États de `draw`

- SCHEDULED : créé à l’avance
- OPEN : ventes ouvertes
- CLOSED : ventes fermées (cutoff)
- RESULTED : `draw_result_id` attaché
- SETTLED : payouts/ledger effectués (idempotent)
- CANCELED : annulé (ops)

### 4.2 Jobs principaux

- `schedule_draws` : crée draws futurs (ex: 7 jours)
- `open_due_draws` : SCHEDULED → OPEN
- `close_due_draws` : OPEN → CLOSED
- `fetch_results` (global) : upsert `draw_result` depuis providers
- `apply_results_to_draws` : lie `draw.draw_result_id` aux draws CLOSED (matching)
- `settle_draws` : RESULTED → SETTLED (payout)

### 4.3 Matching draw ↔ draw_result

- Provider = `draw_channel.external_provider`
- Slot = déduit de `draw_channel.code` (MID/EVE) ou champ explicite
- Date = `draw.draw_date` (date locale du channel)
- Matching par `(provider, slot, date/occurred_at)`

---

## 5) Contrats JSON recommandés

### 5.1 `draw_channel.flags` (source + projection) — exemple

```json
{
  "source": {
    "provider": "NY",
    "pick3": { "external_game_key": "NUMBERS", "external_channel_code": "MID" },
    "pick4": { "external_game_key": "WIN4", "external_channel_code": "MID" }
  },
  "projection": {
    "lot1": "PICK3_FULL_3",
    "lot2": "PICK4_FIRST2",
    "lot3": "PICK4_LAST2",
    "lot4": "PICK3_FIRST2"
  }
}
```

### 5.2 `draw_result.source_result` (normalisé)

```json
{
  "version": 1,
  "pick3": { "value": "123" },
  "pick4": { "value": "4567" },
  "meta": { "provider": "NY", "slot": "MID" }
}
```

### 5.3 `draw_result.haiti_result` (projection)

```json
{
  "version": 1,
  "lot1": { "type": "PICK3_FULL_3", "value": "123" },
  "lot2": { "type": "PICK4_FIRST2", "value": "45" },
  "lot3": { "type": "PICK4_LAST2", "value": "67" },
  "lot4": { "type": "PICK3_FIRST2", "value": "12" }
}
```

Remarque : versionner les JSON (`version`) pour permettre l’évolution sans casser les handlers.

---

## 6) Ajouter un nouveau jeu (ex: `HT_LOTO2`)

1. `game` (catalogue global) : migration seed idempotente — `code=HT_LOTO2`, `name=\"Loto 2\"`, `combination=EXACT`, `min_digits=2`, `max_digits=2`, `active=true`.
2. `tenant_game` : upsert pour le tenant cible — `enabled=true`, `display_name=\"Loto 2\"`, `flags={ \"multiplier\": 50 }`.
3. `draw_channel_game` : relier aux channels choisis (tous ou subset). Upsert `(tenant_id, draw_channel_id, game_id, enabled=true)`.
4. Sales / Ticket / Payout : accepter le nouveau `game.code` et ajouter la règle de calcul de payout soit via un GameRuleEngine soit via handler dédié.
- Important : pas de changement nécessaire sur `draw_channel` ou `draw`.

---

## 7) Ajouter un nouveau provider externe (ex: `NJ`)

1. Créer `draw_channel` : `HT_NJ_MID`, `HT_NJ_EVE` (timezone, draw_time, cutoff_sec, `external_provider=NJ`, `flags.source`, `flags.projection`).
2. Relier jeux via `draw_channel_game`.
3. Implémenter fetcher `NJ` pour produire `draw_result` (ou marquer MANUAL si pas d’API).

---

## 8) Règles Ops / Override (MVP)

- Avant `SETTLED` : override autorisé (création de `draw_result` MANUAL si nécessaire, ex TN).
- Après `SETTLED` : override bloqué (pour MVP). Toute modification après settlement nécessite workflow de reversal comptable.

---

## Annexes rapides

- Tables clefs : `game`, `tenant_game`, `draw_channel`, `draw_channel_game`, `draw`, `draw_result`.
- Schéma recommandé :
  - `draw_channel.flags` = { source, projection }
  - `draw_result` = { source_result, haiti_result, status, source, raw_payload, version }
- Idempotence : toutes les seeds doivent être upsert safe (ON CONFLICT … DO UPDATE).

---

Notes pratiques :
- Exemple concret de seed (games, tenant_game, draw_channel, draw_channel_game) se trouve dans :
  `src/main/resources/db/migration/V46__seed_core_game_draw_ht_default_tenant.sql` (seed Haiti pour tenant `00000000-0000-0000-0000-000000000003`).

Fin.

