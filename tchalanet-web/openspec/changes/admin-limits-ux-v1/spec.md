# Spec — admin-limits-ux-v1

Redesign de `Kontwòl lavant` pour simplifier l'UX autour des deux règles produit réelles,
ajouter les vues contextuelles par scope, et nettoyer le backend du catalog inutilisé.

Prérequis : `spec/limits-contextual-config-v1` mergée (backend limitpolicy contextuel, draw_exposure,
admin BFF, `AdminLimitsSectionComponent`, `BlockNumberQuickDialog`).

---

## Règles conservées dans le produit admin

| RuleKey | Label métier HT | Wording explication |
|---|---|---|
| `BLOCK_SELECTION_PER_DRAW` | Bloke nimewo | Bloke yon nimewo sou yon tiraj. |
| `MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` | Plafon pa nimewo | Limite kantite total ki ka vann sou yon nimewo pou yon tiraj. |

`MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW` est cumulatif (table `draw_exposure`).
Ne pas le présenter comme un simple plafond par ligne.
Ne pas le présenter comme équivalent à `TenantGame.maxStake`.

## Règles retirées du catalog admin UX (évaluateurs conservés)

- `MAX_STAKE_PER_TICKET`
- `MAX_LINES_PER_TICKET`
- `BLOCK_BET_TYPE`
- `MAX_SALES_COUNT_PER_SELECTION_PER_DRAW`
- `MAX_STAKE_PER_LINE` (à confirmer avec product — `maxStake` jeu + override scope peut suffire)

## RuleKey à supprimer du code

- `MAX_SALES_COUNT_PER_TICKET` — aucun évaluateur, aucune entrée catalog, doublon conceptuel de `MAX_LINES_PER_TICKET`.

---

## Mental model cible

```
Kontwòl lavant
→ toutes les LimitAssignments configurées
→ ajouter / éditer / désactiver / supprimer

Tenant / Draw Channel / Seller Terminal
→ limits configurées pour ce scope
→ limits héritées/effectives
→ lien contextuel vers gestion centrale

Draw
→ limits effectives qui s'appliquent à ce tiraj maintenant
→ raccourcis opérationnels (Bloke nimewo)
→ lien contextuel vers gestion centrale
```

---

## Page centrale `/limits`

### Stats inline (remplace le panneau summary)

```
Kontwòl lavant
3 limit aktif · 1 blokaj nimewo · 2 plafon
```

### Quick actions

```
[ Bloke nimewo ]   [ Plafon pa nimewo ]
```

Même niveau hiérarchique. `Bloke nimewo` = BLOCK_SELECTION_PER_DRAW.
`Plafon pa nimewo` = MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW.

### Liste des assignments (desktop)

```
RÈG             | APLIKE SOU   | VALÈ       | PORTÉE       | ETA   | AKSYON
Blokaj nimewo   | NY · Midi    | 23, 33, 44 | Draw Channel | Aktif | [Modifye] [Dezaktive] […]
Plafon pa nimewo| Tout santral | 100 000 HTG| Santral      | Aktif | [Modifye] [Dezaktive] […]
```

### Compact card (mobile ~360 px)

```
Blokaj nimewo
23, 33, 44
NY · Midi       Aktif
[Modifye]  […]
```

---

## Vue contextuelle — Tenant

```
Limit santral la
Blokaj nimewo      23, 33, 44
Plafon pa nimewo   100 000 HTG
[Ajoute yon limit]   [Gade tout limit yo →]
```

---

## Vue contextuelle — Draw Channel

```
Limit kanal sa a
Blokaj nimewo      23, 33, 44        (local)
Limit eritye
Plafon pa nimewo   100 000 HTG   Soti nan: Santral
[Ajoute yon limit]   [Jere limit kanal la →]
```

---

## Vue contextuelle — Seller Terminal

```
Limit machin sa a
Plafon pa nimewo   50 000 HTG         (local)
Limit eritye
Blokaj nimewo      23, 33, 44   Soti nan: Santral
[Ajoute yon limit]   [Jere limit machin sa a →]
```

---

## Draw Detail — effective limits only

```
Limit ki aplike sou tiraj sa a
Blokaj nimewo      23, 33, 44   Soti nan: Santral
Plafon pa nimewo   100 000 HTG  Soti nan: NY · Midi
[Bloke nimewo]
[Jere limit kanal la →]
```

---

## Préselection contextuelle

| Surface d'entrée | Paramètres préselectionés |
|---|---|
| Draw → Bloke nimewo | rule=BLOCK_SELECTION_PER_DRAW, scope=DRAW_CHANNEL, scopeId=channelId |
| Draw Channel → Ajoute | scope=DRAW_CHANNEL, scopeId=channelId |
| Seller Terminal → Ajoute | scope=SELLER_TERMINAL, scopeId=terminalId |
| Tenant → Ajoute | scope=TENANT |

L'éditeur central est l'unique implémentation du formulaire.

---

## Effective limits API

La résolution reste backend-owned. Les vues contextuelles ont besoin au minimum de :
- display name de la règle gagnante
- scope gagnant + display name du scope
- valeur configurée
- outcome
- hérité vs local (inferred from scope match)

Ne pas reproduire `LimitResolver` / `ScopeScoreTable` en frontend.

---

## Responsive

Desktop : stats inline compactes, table assignments filtrée, sections contextuelles, provenance visible en secondaire.

Mobile ~360 px : pas de panneau navy, pas de table large, assignments en compact cards, quick actions empilées ou 2 colonnes si touch targets ≥ 48 dp.
