LimitPolicy — Limit Definitions (Catalogue)

Concepts communs

Période (Period)
- per_ticket : s’applique à un ticket/transaction
- per_draw : s’applique à un tirage (drawId / drawKey)
- per_day : s’applique à une journée (selon timezone tenant)

Portée d’agrégation (AggregationScope)
Où on cumule les totaux (selon la limite) :
- AGENT (vendeur/caissier)
- OUTLET (point de vente)
- ZONE (zone géographique)
- RANGE (groupe arbitraire d’outlets défini par tenant)
- (optionnel plus tard) TENANT

Dimension (Dimension)
- line : une ligne du ticket
- ticket : tout le ticket
- selection : une sélection précise (ex “12”, “12-34”, “000”)
- total : total toutes sélections confondues

Canonisation selection_key (indispensable)
- 2D : "00".."99" (toujours 2 chars)
- 3D : "000".."999" (toujours 3 chars)
- Marriage : "12-34" (recommandé : trié min-max)
- Lotto4/5 pattern : "<pattern>:<digits>" (ex: "x0123xx:0123", "012xx34:01234")

Bet Types (noms internes recommandés)
Borlette “Match” (2D)
- MATCH_1_2D : “match 1” (selon règles)
- MATCH_2_2D
- MATCH_3_2D
Note : ces trois modes ont la même sélection 2D (selection_key = "00".."99") mais des multiplicateurs/payouts différents.

Marriage (2D + 2D)
- MARRIAGE_2D2D : deux numéros 2D (ex: 12 et 34) → selection_key = "12-34"

Lotto (3D / patterns)
- LOTTO3_3D : sélection 3D → selection_key = "000".."999"
- LOTTO4_PATTERN : pattern de type x0123xx, x01xx23, xxx0123 → selection_key = "<pattern>:<digits>"
- LOTTO5_PATTERN : pattern de type 01234xx, 012xx34, xx01234 → selection_key = "<pattern>:<digits>"

Limit Definitions — Détails

Pour chaque limite :

But
S’applique à
Données du contexte (ce que l’évaluateur utilise)
Facts (ce que l’on doit cumuler depuis la DB / cache)
Params (structure suggérée)

1) MAX_STAKE_PER_SELECTION_PER_TICKET
### But

empêcher qu’un ticket mette trop d’argent sur la même sélection (numéro/combinaison).
Période : per_ticket
Dimension : selection
S’applique à : un ou plusieurs bet_types.

Contexte utilisé :
- lignes du ticket : (betType, selection_key, stake)
- total stake par sélection dans ce ticket

Facts : aucun (tout est dans le ticket)

Params :
{
  "max": "50.00",
  "currency": "HTG",
  "applies_to": { "bet_types": ["MATCH_1_2D","MATCH_2_2D","MATCH_3_2D"], "selection_pattern": "*" }
}

2) MAX_SALES_COUNT_PER_SELECTION_PER_DRAW
### But

bloquer une sélection si elle a été vendue trop de fois sur un tirage (anonyme).
Période : per_draw
Dimension : selection
Agrégation : AGENT|OUTLET|ZONE|RANGE

Contexte utilisé :
- drawId
- pour chaque line : selection_key, betType
- deltaCount = nombre de lignes vendues pour cette sélection dans la transaction (souvent 1)

Facts requis :
- sold_count_so_far(drawId, scopeKey, betType, selection_key)

Params :
{
  "period": "per_draw",
  "aggregation_scope": "OUTLET",
  "max_count": 100,
  "applies_to": { "bet_types": ["MATCH_1_2D"], "selection_pattern": "*" }
}

3) MAX_EXPOSURE_PER_SELECTION_PER_DRAW
### But

bloquer une sélection si la somme des mises sur ce numéro/combinaison atteint un plafond, par tirage.
Période : per_draw
Dimension : selection
Agrégation : AGENT|OUTLET|ZONE|RANGE

Contexte utilisé :
- stake de la ligne (ou somme des stakes des lignes identiques)
- drawId, selection_key, betType

Facts requis :
- sold_stake_total_so_far(drawId, scopeKey, betType, selection_key)

Params :
{
  "period": "per_draw",
  "aggregation_scope": "ZONE",
  "max": "500.00",
  "currency": "HTG",
  "applies_to": { "bet_types": ["MATCH_1_2D","MARRIAGE_2D2D"], "selection_pattern": "*" }
}

4) MAX_TOTAL_STAKE_PER_DRAW
### But

limiter la vente totale (toutes sélections confondues) pour un tirage.
Période : per_draw
Dimension : total
Agrégation : AGENT|OUTLET|ZONE|RANGE

Contexte utilisé :
- ticketStakeTotal (somme des lignes)
- drawId

Facts requis :
- sold_total_stake_so_far(drawId, scopeKey)

Params :
{
  "period": "per_draw",
  "aggregation_scope": "AGENT",
  "max": "2000.00",
  "currency": "HTG"
}

5) MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW
### But

limiter le risque payout potentiel cumulé sur une sélection, par tirage (très utile pour Marriage/Lotto).
Période : per_draw
Dimension : selection
Agrégation : OUTLET|ZONE|RANGE|TENANT (souvent pas AGENT)

Contexte utilisé :
- stake + payoutMultiplier (dépend du betType)
- potentialPayout = stake * multiplier
- drawId, selection_key, betType

Facts requis :
- potential_payout_exposure_so_far(drawId, scopeKey, betType, selection_key)

Params :
{
  "period": "per_draw",
  "aggregation_scope": "OUTLET",
  "max": "250000.00",
  "currency": "HTG",
  "applies_to": { "bet_types": ["MARRIAGE_2D2D","LOTTO3_3D","LOTTO4_PATTERN","LOTTO5_PATTERN"] }
}

6) MAX_STAKE_PER_LINE
### But

mise maximum par ligne (peu importe la sélection).
Période : per_ticket
Dimension : line

Contexte :
- stake de chaque ligne

Facts : aucun

Params :
{ "max": "50.00", "currency": "HTG", "applies_to": { "bet_types": ["MATCH_1_2D","LOTTO3_3D"] } }

7) MIN_STAKE_PER_LINE
### But

mise minimum par ligne (anti-spam / règles commerciales).
Période : per_ticket
Dimension : line

Contexte :
- stake de chaque ligne

Params :
{ "min": "1.00", "currency": "HTG", "applies_to": { "bet_types": ["MATCH_1_2D","MATCH_2_2D","MATCH_3_2D"] } }

8) MAX_LINES_PER_TICKET
### But

limiter le nombre de lignes sur un ticket.
Période : per_ticket
Dimension : ticket

Contexte :
- linesCount

Params :
{ "max_count": 20, "applies_to": { "bet_types": ["MATCH_1_2D","MARRIAGE_2D2D","LOTTO3_3D"] } }

9) MAX_STAKE_PER_TICKET
### But

plafond de mise par ticket (total des lignes).
Période : per_ticket
Dimension : ticket

Contexte :
- ticketStakeTotal

Params :
{ "max": "200.00", "currency": "HTG" }

10) DAILY_STAKE_CAP
### But

plafond de vente total sur une journée (utile pour gestion de caisse).
Période : per_day
Agrégation : AGENT|OUTLET|ZONE|RANGE

Contexte :
- ticketStakeTotal
- date (timezone tenant)

Facts :
- daily_stake_total_so_far(day, scopeKey)

Params :
{
  "period": "per_day",
  "aggregation_scope": "OUTLET",
  "max": "5000.00",
  "currency": "HTG",
  "window_tz": "America/Toronto"
}

11) MAX_PAYOUT_PER_LINE
### But

plafond payout par ligne (réel ou potentiel selon ton flow payout).
Période : per_ticket / per_payout_tx
Dimension : line

Contexte :
- payoutPerLine (calculé ou connu au moment payout)

Params :
{ "max": "1000.00", "currency": "HTG", "applies_to": { "bet_types": ["MATCH_1_2D","LOTTO3_3D"] } }

12) MAX_PAYOUT_PER_TICKET
### But

plafond payout total par ticket.
Période : per_ticket / per_payout_tx
Dimension : ticket

Contexte :
- payoutTotal

Params :
{ "max": "2500.00", "currency": "HTG" }

13) DAILY_PAYOUT_CAP
### But

plafond de payouts sur une journée (cash management / risque).
Période : per_day
Agrégation : AGENT|OUTLET|ZONE|RANGE

Facts :
- daily_payout_total_so_far(day, scopeKey)

Params :
{
  "period": "per_day",
  "aggregation_scope": "OUTLET",
  "max": "15000.00",
  "currency": "HTG",
  "window_tz": "America/Toronto"
}

14) MAX_CANCELS_PER_DAY
### But

limiter le nombre d’annulations par jour (anti-fraude).
Période : per_day
Agrégation : AGENT|OUTLET

Facts :
- daily_cancel_count_so_far(day, scopeKey)

Params :
{
  "period": "per_day",
  "aggregation_scope": "AGENT",
  "max_count": 10,
  "window_tz": "America/Toronto"
}

Mapping rapide “quelle limite pour quel besoin ?”

“Empêcher qu’un vendeur vende trop sur un même numéro”
- MAX_STAKE_PER_SELECTION_PER_TICKET (par ticket)
- MAX_EXPOSURE_PER_SELECTION_PER_DRAW (cumul montant, par draw, scope AGENT/OUTLET/ZONE/RANGE)
- MAX_SALES_COUNT_PER_SELECTION_PER_DRAW (cumul nb ventes, par draw)

“Bloquer si trop de risque payout”
- MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW

“Gestion caisse / volume”
- MAX_STAKE_PER_TICKET, MAX_LINES_PER_TICKET
- DAILY_STAKE_CAP
- DAILY_PAYOUT_CAP
