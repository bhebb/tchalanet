# Variantes de règlement — `BetType` / `BetOption` / `SettlementVariant`

> Référence de calcul des gains pour les jeux loterie Haïti (Bòlèt, Maryaj, Loto 3/4/5).
> Change OpenSpec : `openspec/changes/supported-bet-options-combinations-v1`.
> Code : `SettlementVariant`, `SettlementVariantResolver` (domain pur),
> `TicketWinningCalculator` (application).

Ce document sert de **table de vérité** pour les tests unitaires. Chaque simulation ci-dessous
correspond à un cas à couvrir dans `SettlementVariantResolverTest` et `TicketWinningCalculatorTest`.

---

## 1. Les trois niveaux

| Niveau | Propriétaire | Rôle |
| --- | --- | --- |
| `BetType` + `BetOption` | `catalog.game` | Ce que le vendeur choisit / affichage catalog |
| `SettlementVariant` | `core.sales` (domain) | Ce que le backend calcule pour le règlement |
| Labels d'affichage | web / mobile / admin | Rendu commercial (vendeur/client) vs technique (admin/support) |

Le vendeur choisit **Exact** ou **Permuté** ; le backend **calcule** la variante technique
(`3-way`, `6-way`, `12-way`, `24-way`) à partir du numéro joué. Les variantes techniques ne sont
jamais des choix vendeur en v1.

Résolution : `SettlementVariantResolver.resolve(betType, rawOption, selection)` — **pure**, sans
Spring, sans repo, sans bus. Une entrée non supportée lève `IllegalArgumentException`.

---

## 2. Faits du tirage (`TicketResultFacts`)

Construits depuis `DrawResultProjection` :

| Fait | Dérivation |
| --- | --- |
| `lot1_2d`, `lot2_2d`, `lot3_2d`, `lot4_2d` | 2 derniers chiffres du lot correspondant |
| `lot1_3d` | 3 chiffres du lot1 |
| `pick3` | lot1 (3 chiffres) sinon lot4 (3 chiffres) |
| `pick4` | lot4 (4 chiffres) sinon lot1 (4 chiffres) |
| `orderedTwoDigits` | liste ordonnée des 2D : lot1, lot2, lot3, lot4 (+ paires dérivées) |

> **Correction Bòlèt** : `MATCH_1_2D` ne regarde QUE `lot1_2d`, `MATCH_2_2D` QUE `lot2_2d`,
> `MATCH_3_2D` QUE `lot3_2d`. Une valeur 2D présente dans un autre lot ne fait pas gagner.

---

## 3. Matrice complète + simulations

Tirage de référence utilisé dans les exemples :

```text
lot1 = 736   (lot1_2d = 36, lot1_3d = 736)
lot2 = 17    (lot2_2d = 17)
lot3 = 76    (lot3_2d = 76)
pick3 = 736
pick4 = —    (voir cas Loto 4 avec pick4 dédié)
```

### 3.1 Bòlèt / 2D lots

| BetType | Option | SettlementVariant | Règle | Simulation | Résultat |
| --- | --- | --- | --- | --- | --- |
| `MATCH_1_2D` | — | `MATCH_1_2D` | selection == `lot1_2d` | joue `36`, lot1_2d=`36` | **WON** |
| `MATCH_1_2D` | — | `MATCH_1_2D` | lot-specific | joue `17` (présent lot2, pas lot1) | **LOST** |
| `MATCH_2_2D` | — | `MATCH_2_2D` | selection == `lot2_2d` | joue `17`, lot2_2d=`17` | **WON** |
| `MATCH_2_2D` | — | `MATCH_2_2D` | lot-specific | joue `36` (présent lot1, pas lot2) | **LOST** |
| `MATCH_3_2D` | — | `MATCH_3_2D` | selection == `lot3_2d` | joue `76`, lot3_2d=`76` | **WON** |
| `MATCH_3_2D` | — | `MATCH_3_2D` | lot-specific | joue `36` (présent lot1, pas lot3) | **LOST** |

### 3.2 Maryaj (`MARRIAGE_2D2D`)

`orderedTwoDigits` = `[36, 17, 76]` pour le tirage de référence.

| Option | SettlementVariant | Règle | Simulation | Résultat |
| --- | --- | --- | --- | --- |
| `1` Ordre exact | `MARRIAGE_EXACT_ORDER` | les 2 numéros sortent dans l'ordre joué | joue `36-17` (36 avant 17) | **WON** |
| `1` Ordre exact | `MARRIAGE_EXACT_ORDER` | ordre respecté | joue `17-36` (17 avant 36 ? non) | **LOST** |
| `2` Revers/double | `MARRIAGE_REVERSE_ALLOWED` | les 2 présents, ordre libre | joue `17-36` | **WON** |
| `2` Revers/double | `MARRIAGE_REVERSE_ALLOWED` | un des deux absent | joue `36-99` | **LOST** |
| `2` Revers/double | `MARRIAGE_REVERSE_ALLOWED` | doublon `aa` → il faut 2 occurrences de `a` | joue `36-36`, un seul `36` | **LOST** |

### 3.3 Loto 3 (`LOTTO3_3D`) — pick3 = `736`

| Option | SettlementVariant | Pattern | Simulation | Résultat |
| --- | --- | --- | --- | --- |
| `1` Exact | `LOTTO3_STRAIGHT` | ordre exact | joue `736` | **WON** |
| `1` Exact | `LOTTO3_STRAIGHT` | ordre exact | joue `763` | **LOST** |
| `2` Permuté | `LOTTO3_BOX_6_WAY` | 3 chiffres différents (pick3=`736`) | joue `367` (mêmes chiffres) | **WON** |
| `2` Permuté | `LOTTO3_BOX_3_WAY` | 2 chiffres identiques (ex pick3=`112`) | joue `211` | **WON** |
| `2` Permuté | `LOTTO3_BOX_*` | chiffres différents du tirage | joue `789` | **LOST** |

> La variante 3-way vs 6-way est **calculée depuis le numéro joué** (2 chiffres identiques → 3-way,
> 3 différents → 6-way). Le gain lui-même compare les chiffres triés (`sortedDigits`).

### 3.4 Loto 4 (`LOTTO4_PATTERN`)

Résolution du box selon la forme du numéro joué :

| Forme | Exemple | SettlementVariant |
| --- | --- | --- |
| 3 chiffres identiques | `1112` | `LOTTO4_BOX_4_WAY` |
| 2 paires | `1122` | `LOTTO4_BOX_6_WAY` |
| 1 paire + 2 différents | `1123` | `LOTTO4_BOX_12_WAY` |
| 4 chiffres différents | `1234` | `LOTTO4_BOX_24_WAY` |
| 4 identiques | `1111` | **rejeté** (`IllegalArgumentException`) |

Simulations (pick4 indiqué par cas) :

| Option | SettlementVariant | Simulation (joué → pick4) | Résultat |
| --- | --- | --- | --- |
| `1` Exact | `LOTTO4_STRAIGHT` | `1234` → `1234` | **WON** |
| `1` Exact | `LOTTO4_STRAIGHT` | `1234` → `1243` | **LOST** |
| `2` Permuté | `LOTTO4_BOX_24_WAY` | `1234` → `4321` | **WON** |
| `2` Permuté | `LOTTO4_BOX_12_WAY` | `1123` → `3211` | **WON** |
| `2` Permuté | `LOTTO4_BOX_6_WAY` | `1122` → `2211` | **WON** |
| `2` Permuté | `LOTTO4_BOX_4_WAY` | `1112` → `2111` | **WON** |
| `2` Permuté | `LOTTO4_BOX_*` | `1234` → `1235` | **LOST** |
| `3` Deux premiers | `LOTTO4_FRONT_PAIR` | `12` → pick4 `1234` (startsWith) | **WON** |
| `3` Deux premiers | `LOTTO4_FRONT_PAIR` | `34` → pick4 `1234` | **LOST** |
| `4` Deux derniers | `LOTTO4_BACK_PAIR` | `34` → pick4 `1234` (endsWith) | **WON** |
| `4` Deux derniers | `LOTTO4_BACK_PAIR` | `12` → pick4 `1234` | **LOST** |

### 3.5 Loto 5 (`LOTTO5_PATTERN`)

Faits : `lot1_3d`, `lot2_2d`, `lot3_2d`. Exemple tirage : lot1=`361`, lot2=`75`, lot3=`76`.

| Option | SettlementVariant | Concat gagnante | Simulation | Résultat |
| --- | --- | --- | --- | --- |
| `1` 1er+2e lot | `LOTTO5_LOT1_LOT2` | `lot1_3d + lot2_2d` = `36175` | joue `36175` | **WON** |
| `1` 1er+2e lot | `LOTTO5_LOT1_LOT2` | | joue `36176` | **LOST** |
| `2` 1er+3e lot | `LOTTO5_LOT1_LOT3` | `lot1_3d + lot3_2d` = `36176` | joue `36176` | **WON** |
| `3` Mixte | `LOTTO5_MIXED_1_2_3` | `dernier chiffre lot1 + lot2_2d + lot3_2d` = `1`+`75`+`76` = `17576` | joue `17576` | **WON** |

---

## 4. Cas limites & résultats incomplets

| Cas | Comportement attendu |
| --- | --- |
| lot2 manquant (null) et option en dépend | `LOST` (fait null → pas de match), jamais d'exception au règlement |
| lot3 manquant pour Loto5 mixte | `LOST` |
| selection mal formée (mauvais nb de chiffres) | `normalizeDigits` → null → `LOST` |
| Loto 4 box `1111` (4 identiques) | resolver **rejette** → traité en amont (validation vente) |
| option non supportée (code inconnu) | resolver lève `IllegalArgumentException` |

### Décision arrêtée — option non supportée (réserve #2)

Deux niveaux distincts, **pas** de conversion en `LOST` :

```
Option non supportée      = ticket invalide à créer  → rejet à la vente
Option supportée non gagnante = LOST
```

**Vente / preview / confirm** (niveau primaire, task 6.18) :

- Option non supportée → **rejet bloquant**, aucune ligne créée, aucun payout potentiel calculé.
- Déjà implémenté : `SaleCommandValidator.validateBetOption` → `BetOption.from(...)` lève →
  `ProblemRest.badRequest("sales.bet_option_out_of_range")`.
- Cas testés :
  - `LOTTO4_PATTERN` + betOption `99` → `sales.bet_option_out_of_range`, pas de ligne.
  - betType à option requise + betOption `null` → `sales.bet_option_required`.
  - betType sans option + betOption non null → `sales.bet_option_not_allowed`.

**Règlement / settlement** (ligne existante corrompue/legacy) :

- Le calculateur **ne catch pas** : `SettlementVariantResolver.resolve(...)` / `BetOption.from(...)`
  laissent remonter l'exception → échec fort du règlement du ticket (stratégie draw actuelle).
- Ne **jamais** convertir en `LOST` : ça masquerait une erreur produit/config.
- V1 : pas de nouveau statut. Une option non supportée au règlement = anomalie, pas un pari perdu.

**Tasks futures** (hors scope v1, quand un ticket legacy le justifie) :

- [ ] Ajouter `TicketLineResultStatus.ERROR` / `SETTLEMENT_FAILED` pour lignes legacy/corrompues.
- [ ] Ajouter reason code `UNSUPPORTED_BET_OPTION`.

> Conséquence pour le code : le `TicketWinningCalculator` reste **sans `try/catch` qui renverrait
> `false`**. L'exception du resolver doit remonter.

---

## 5. Persistance & exposition API

**Décision (task 3.3) : la variante calculée n'est PAS persistée.** Le gain réalisé est calculé
au settlement depuis le snapshot de vente ; la variante est purement informative
(audit/support). Elle est donc **recalculée à la demande**, sans colonne `ticket_line`, sans
migration Flyway.

Primitive backend (core.sales, api) :

| Élément | Rôle |
| --- | --- |
| `SettlementExplanationApi.explain(betType, betOption, selection)` | interface publique |
| `ComputedSettlementVariantView(variant, commercialLabel, adminLabel)` | résultat |
| `SettlementExplanationService` | impl `@Component`, délègue au resolver + `BetOption.label()` |

- `variant` : code technique (`LOTTO4_BOX_24_WAY`).
- `commercialLabel` : label vendeur/client (`BetOption.label()`, ou label lot pour Bòlèt).
- `adminLabel` : `SettlementVariant.adminLabel()` (`Permuté · 24-way`, `Maryaj · revers/double`…).

> La preview vendeur (`TicketSalePreviewResult`) **n'est pas modifiée** : injecter la variante par
> ligne polluerait le contrat consommé par le terminal (règle produit). Les consommateurs
> admin/support/debug (features slice) appellent `SettlementExplanationApi` directement.

## 6. Labels d'affichage

| Contexte | Ce qui est montré |
| --- | --- |
| Terminal vendeur | Exact, Permuté, Deux premiers, Deux derniers, Ordre exact, Revers/double, 1er+2e lot, 1er+3e lot, Mixte |
| Reçu client | label commercial uniquement (jamais la variante technique) |
| Admin / support | peut afficher `Permuté · 24-way`, `Variante calculée: 24-way` |
