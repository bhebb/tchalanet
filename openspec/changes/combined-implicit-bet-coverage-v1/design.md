# Design: combined-implicit-bet-coverage-v1

## Décisions figées

- **D3 — Modèle** : `TicketLineCoverage` (Option B). `TicketLine` = ce que le client achète ;
  `ticket_line_coverage` = comment le système peut payer cette ligne.
- **D4 — Gain potentiel** : calculé **par couverture**. Affichage : 1 couverture → « Gain potentiel » ;
  `BEST_OF` → « Gain possible min–max » ; `CUMULATIVE` → « Gain total possible ».
  Décision produit : Maryaj/Loto = `BEST_OF`, Bòlèt multi-lots = `CUMULATIVE`.
- **Resolver** : renvoie un `CoverageResolution` (liste de couvertures), plus un seul variant.

## 1. Pourquoi B et pas A

`TicketLineCoverage` garde une sémantique claire : `TicketLine` = achat client, `Coverage` =
possibilités de règlement. Option A (lignes internes groupées) mélange « ligne commerciale visible »
et « ligne technique de règlement » → ambigu partout (liste tickets, reçu, settlement, total,
annulation, audit, reprint, reporting). On prend B.

## 2. Modèle cible

```text
ticket_line
- id, ticket_id, game_code, bet_type
- commercial_option_code            (BetOption, y compris options combinées)
- selection
- stake_total
- display_label
- potential_gain_mode               (SINGLE | RANGE_ALTERNATIVE | RANGE_CUMULATIVE)
- min_potential_gain / max_potential_gain

ticket_line_coverage
- id, ticket_line_id
- pricing_variant_code              (PricingVariantCode — clé d'odds, cf. change 1)
- stake_amount
- odds_snapshot
- potential_gain_snapshot
- win_mode                          (ALTERNATIVE | CUMULATIVE)
```

### Audit du modèle actuel

Le modèle actuel ne peut pas porter correctement `IMPLICIT_BEST_MATCH` :

- `TicketLine` contient une seule `stakeAmount`, un seul `payoutBaseAmount`, une seule
  `oddsSnapshot`, un seul `potentialPayoutAmount`, un seul `betOption`.
- `sales_ticket_line` persiste les mêmes champs uniques (`stake_amount`, `payout_base_amount`,
  `odds_snapshot`, `potential_payout_amount`, `bet_option`) et ne possède pas de table enfant de
  variantes/couvertures.
- `TicketLinePreparationService` résout une seule `PricingVariantCode`, puis demande une seule cote
  effective à `core.pricing`.
- `TicketWinningCalculator` évalue une seule variante et paie `line.potentialPayoutAmount()` si elle
  gagne.
- `Ticket.money().potentialPayoutAmount()` est la somme des gains potentiels de lignes, pas une somme
  ou fourchette de couvertures.
- Les surfaces preview/reçu/print (`SalePreparationViewAssembler`, `TicketReceiptAssembler`,
  `TicketPrintViewMapper`) lisent un seul `oddsSnapshot` et un seul `potentialPayoutAmount` par ligne.
- Les snapshots promotion/audit et events (`AppliedPromotionSnapshotJpaAdapter`,
  `TicketLinePlacedItem`) dupliquent aussi ces valeurs uniques.

Conclusion : la “meilleure possibilité” ne peut pas être représentée par un `betOption=null` mappé
vers une variante unique. Pour Loto 3 `123`, par exemple, la meilleure possibilité dépend du résultat
et des couvertures gagnantes (`LOTTO3_STRAIGHT` si exact, sinon `LOTTO3_BOX_6_WAY` si permutation).
Snapshotter une seule cote au moment de la vente surpayerait ou sous-payerait certains résultats.
`IMPLICIT_BEST_MATCH` doit donc attendre `TicketLineCoverage`, avec snapshot de chaque possibilité.

### Exemple futur — Bòlèt `12` tous les lots (30 HTG)

Cet exemple montre pourquoi le modèle coverage supporte Bòlèt cumulatif, mais il n'est pas livré
comme `BetOption` dans ce change. Le modèle actuel représente Bòlèt par trois `BetType` séparés ;
un vrai produit « tous les lots » demandera un modèle commercial Bòlèt dédié.

```text
ticket_line: selection=12, product="Bòlèt tous les lots" (future), stake_total=30, mode=CUMULATIVE, total=800
coverages:
  MATCH_1_2D  stake 10  odds 50  potential 500
  MATCH_2_2D  stake 10  odds 20  potential 200
  MATCH_3_2D  stake 10  odds 10  potential 100
```

Si `12` apparaît sur les trois lots, les trois coverages gagnent et le payout est la somme :
`500 + 200 + 100 = 800`.

### Exemple — Loto 3 `123` Exact + Permuté (20 HTG)

```text
ticket_line: selection=123, option=LOTTO3_EXACT_PLUS_BOX, stake_total=20, mode=BEST_OF, min=800 max=5000
coverages:
  LOTTO3_STRAIGHT   stake 10  odds 500  potential 5000
  LOTTO3_BOX_6_WAY  stake 10  odds 80   potential 800
```

Pour Maryaj et Loto combinés, si plusieurs coverages matchent le même résultat, le produit paie la
meilleure possibilité (`BEST_OF`), pas la somme.

### Note produit — Dekabès

`Dekabès` n'est pas une option commerciale V0 à sélectionner. C'est un libellé/résultat dérivé quand
un joueur gagne deux fois sur Bòlèt, possiblement avec des numéros différents et sur des lignes/lots
différents. Il doit donc être traité plus tard comme une synthèse post-settlement ou reporting client,
pas comme un `BetOption` ni comme une branche de `CoverageResolution`.

## 3. Resolver — de single à `CoverageResolution`

```java
public record CoverageResolution(
    BetOption option,
    PotentialGainMode potentialGainMode,
    List<CoverageVariant> variants) {}

public record CoverageVariant(PricingVariantCode pricingVariantCode, WinMode winMode) {}
```

- `LOTTO3_BOX` + numéro → **une** couverture (3-way ou 6-way selon le numéro).
- `LOTTO3_EXACT_PLUS_BOX` → **deux** (`LOTTO3_STRAIGHT` + `LOTTO3_BOX_6_WAY`).
- Futur Bòlèt « tous les lots » → trois (`MATCH_1_2D`, `MATCH_2_2D`, `MATCH_3_2D`), mode
  `CUMULATIVE`, à définir dans un change produit Bòlèt séparé.

## 4. Options commerciales combinées (catalog.game)

`BetOption` porte les options commerciales combinées déjà activables quand le runtime coverage est
prêt. À ajouter/maintenir comme options commerciales, **pas** comme variantes :

```text
LOTTO3_EXACT_PLUS_BOX
LOTTO4_EXACT_PLUS_BOX
Produit Bòlèt tous les lots (futur, à définir avec un modèle commercial Bòlèt adapté)
```

## 5. Règle de settlement

Le settlement ne lit plus `ticketLine.oddsSnapshot` mais, par couverture gagnante :
`coverage.stakeAmount × coverage.oddsSnapshot`. Si plusieurs couvertures gagnent → mode produit :
- `BEST_OF` : on paie la meilleure couverture gagnante. Maryaj/Loto.
- `CUMULATIVE` : on somme toutes les couvertures gagnantes. Bòlèt.

V0 produit : Maryaj/Loto = `BEST_OF`; Bòlèt = `CUMULATIVE`.

## 6. Gain potentiel — stockage (aligner preview / reçu / settlement)

Le gain potentiel n'est **pas** stocké seulement au niveau `TicketLine`. Chaque
`ticket_line_coverage` porte : `stakeSnapshot`, `oddsSnapshot`, `potentialGainSnapshot`, `winMode`.
`TicketLine` porte le **résumé** : `potentialGainMode`, `minPotentialGain`, `maxPotentialGain`, et
`totalPotentialGain` **uniquement** pour le mode cumulatif. Ainsi preview/reçu et settlement lisent la
même source.

## 7. Périmètre de non-régression (obligatoire)

Dès que `TicketLineCoverage` existe, **tout ce qui affiche ou additionne des lignes** doit comprendre
« ligne commerciale + couvertures techniques + stake total + stake par couverture + gain min/max ».
Cette spec DOIT couvrir : **vente, preview, confirmation, ticket detail, reçu/reprint,
annulation/refund (si applicable), settlement, payout, reporting minimal**. Un changement du seul
settlement est insuffisant.

## 8. Garde-fou

`EXACT_PLUS_BOX` peut être vendu parce que la vente, les snapshots, le settlement et les surfaces
domaine sont coverage-ready. `IMPLICIT_BEST_MATCH` reste désactivé tant que son comportement produit
n'est pas explicitement figé et testé. Dépend de `core-pricing-consolidation-tenant-odds-v1` (odds par
variante).

## 9. Checkpoint d'implémentation actuel

### Implémenté

- Schéma fresh DB :
  - `sales_ticket_line` porte le résumé commercial :
    `potential_gain_mode`, `min_potential_gain`, `max_potential_gain`, `total_potential_gain`.
  - `sales_ticket_line_coverage` porte les snapshots techniques :
    `pricing_variant_code`, `stake_amount`, `odds_snapshot`, `potential_gain_snapshot`, `win_mode`.
  - Jointure : `sales_ticket_line_coverage.ticket_line_id -> sales_ticket_line.id ON DELETE CASCADE`.
  - Indexes, trigger `updated_at`, RLS sur `sales_ticket_line_coverage`.
- Domaine :
  - `TicketLineCoverage`.
  - `WinMode`.
  - `PotentialGainMode` est un enum public API : `core.sales.api.model.coverage.PotentialGainMode`.
  - `TicketLine` porte les coverages + le résumé min/max/total.
- Resolver :
  - `CoverageResolution` + `CoverageVariant`.
  - `LOTTO3_BOX` reste une couverture unique.
  - `LOTTO3_EXACT_PLUS_BOX` et `LOTTO4_EXACT_PLUS_BOX` existent dans `BetOption`, mais ne sont pas
    activés par défaut dans la config tenant/POS.
- Vente :
  - `TicketLinePreparationService` accepte les coverages multiples pour `EXACT_PLUS_BOX`.
  - V0 répartit la mise totale en centimes par couverture : parts aussi égales que possible, reste
    distribué de manière déterministe aux premières couvertures, et somme des coverages = mise totale.
  - Chaque couverture résout sa propre odds via `ResolveSellerTerminalOddsQuery` et snapshotte son
    stake, odds et gain potentiel.
- Settlement :
  - `TicketWinningCalculator` évalue les coverages.
  - `BEST_OF` / `RANGE_ALTERNATIVE` technique paie la meilleure couverture gagnante.
  - `CUMULATIVE` somme les couvertures gagnantes.
- Surfaces domaine :
  - Preview / prepare sale expose `PotentialGainMode`, min/max/total.
  - Print/reprint porte les coverages en interne.
  - Receipt expose le résumé commercial sans afficher les `PricingVariantCode` techniques.
  - Public verification expose `PotentialGainMode`, min/max/total sans afficher les variants techniques.
  - POS ticket detail expose le même résumé via `TicketPrintView`.
  - La vue SQL `sales_ticket_print_header_v` reste volontairement header-only : le reprint lit les
    lignes et coverages via `TicketJpaRepository` + `TicketJpaMapper`.
- Archive :
  - `sales_ticket_line` exporte le résumé coverage (`potential_gain_mode`, min/max/total).
  - `sales_ticket_line_coverage` est un dataset archive séparé pour conserver les snapshots
    techniques long-terme.
- Annulation / refund :
  - l'annulation passe par l'aggregate, refuse les tickets déjà résultés et ne recalcule aucun odds.
  - payout/refund s'appuie sur `winningAmount` et les montants ticket, pas sur une odds de ligne.

### Validé

```bash
JAVA_HOME="$HOME/.sdkman/candidates/java/current" PATH="$JAVA_HOME/bin:$PATH" \
  ./mvnw -pl tchalanet-core -am \
  -Dtest=TicketAggregateMutatorTest,TicketReceiptAssemblerTest,TicketLinePreparationServiceTest,TicketWinningCalculatorTest,SettlementVariantResolverTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

openspec validate combined-implicit-bet-coverage-v1 --strict
git diff --check
```

Ces tests couvrent :

- snapshot non-rétroactif des odds existants ;
- resolver single + exact/box multi-couverture ;
- vente Exact + Permuté : deux coverages, mise totale conservée, split par coverage, min/max correct ;
- vente Exact + Permuté avec centime impair : split déterministe sans rejet, somme des coverages =
  mise totale ;
- payout Exact + Permuté : exact gagne plus que box quand les deux matchent ;
- round-trip mapper/JPA des coverages ;
- reçu qui expose le résumé min/max sans variant technique.
- public verification qui traverse `PotentialGainMode` + min/max.
- reprint complet via `TicketPrintProjectionReaderAdapter`.

### Reste à compléter

- Modéliser plus tard les produits Bòlèt composés :
  - Bòlèt « tous les lots » : cumulative sur `MATCH_1_2D`, `MATCH_2_2D`, `MATCH_3_2D`, mais pas comme
    `BetOption` dans le modèle actuel.
  - `Dekabès` : ne pas coder comme option ; c'est un label/signal dérivé post-settlement quand le
    joueur gagne deux fois, possiblement avec des numéros différents.
  - le modèle actuel représente Bòlèt par trois `BetType`; il faut donc ajouter un support
    commercial Bòlèt composé sans forcer l'option dans un `BetType` inadéquat.
- Activer `IMPLICIT_BEST_MATCH` seulement après décision explicite sur son comportement produit.
