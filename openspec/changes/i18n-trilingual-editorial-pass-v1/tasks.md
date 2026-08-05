# Tasks — i18n Trilingual Editorial Pass V1

> **Checkpoint** : lire ce fichier en premier à chaque reprise de session.
> Cocher chaque tâche **dès qu'elle est réellement terminée**, avant de passer à la suivante.

## Phase 0 — Outillage

- [x] 0.1 Audit du corpus : parité, orphelines, FR résiduel, placeholders, orthographe HT
- [x] 0.2 Confirmer que les placeholders sont à 0 écart et qu'aucune valeur n'est vide
- [x] 0.3 Committer le script d'audit dans `scripts/i18n-audit.py` (rejouable après chaque phase)
      — baseline mesurée : **997 défauts**

## Phase 1 — Base terminologique

- [x] 1.1 Table terminologique trilingue arrêtée → `design.md`
- [x] 1.2 Réécrire `tchalanet-docs/docs-public/glossaire/index.md` en FR/EN/HT sur le modèle
      SellerTerminal (retire « Terminal POS », « Caissier », « Session » comme concepts séparés)
- [x] 1.3 Ajouter dans `docs/00-guidelines/glossary.md` un renvoi : « registre public → glossaire public »

## Phase 2 — Hygiène des clés (avant toute rédaction)

- [x] 2.1 Vérifier les 122 orphelines contre le code — **0 référencée** sur 1 082 fichiers source
- [x] 2.2 Supprimer les 54 orphelines HT (ancien modèle : `session`, `outlet`, `seller`, `terminals`)
- [x] 2.3 Supprimer les 68 orphelines EN (génération intermédiaire : `connection`, `seller_terminals`)
- [x] 2.4 ~~Créer~~ **Récupérer** les 88 clés EN « manquantes »
- [x] 2.5 Re-run audit → parité stricte atteinte : fr = en = ht = **4 050 clés**

### Défaut découvert en phase 2 — objets JSON dupliqués

`JSON.parse` applique *last-wins* : un objet déclaré deux fois rend le premier sous-arbre
**inatteignable au runtime**. 11 objets dupliqués dans 6 fichiers.

- [x] 2.6 Fusionner les doublons au lieu de les écraser (union récursive, dernier gagne sur conflit
      de feuille → comportement à l'écran inchangé)
- [x] 2.7 `admin.settings` (EN) : **88 clés récupérées** — ce n'était pas un trou de traduction,
      les chaînes existaient mais ne se chargeaient jamais
- [x] 2.8 `platform.identity.activation.error.{title,message}` : récupéré dans **les 3 locales**
      (bug runtime toutes langues). Le FR récupéré n'avait jamais été relu :
      « Activation du compte incomplete », « verification », « n a pas pu » → à corriger en 3.1

## Phase 3 — Passe éditoriale web (par namespace, FR → EN → HT)

Pour chaque namespace : terminologie (table du `design.md`) → registre/ton → longueur UI →
cohérence intra-locale → placeholders intacts.

- [x] 3.1 `errors` (616) — **138 chaînes réécrites**, chacune vérifiée contre sa valeur attendue
      avant écriture (aucune substitution regex sur la terminologie). Résultat sur ce namespace :
      0 fuite technique, 0 faute d'accent FR, 0 faute IPN. `core-i18n` (4) et `web-errors` (13) verts.
- [ ] 3.2 `feature-public` (440) — vitrine ; contient les doublons `seller-terminal` les plus visibles
- [ ] 3.3 `surface-public` (48)
- [ ] 3.4 `common` (105)
- [ ] 3.5 `component` (120)
- [ ] 3.6 `domain` (118) — socle terminologique, aligne les autres
- [ ] 3.7 `feature-auth` (80)
- [ ] 3.8 `surface-admin` (174)
- [ ] 3.9 `feature-admin` (1367) — le plus gros ; sous-découper par section si besoin
- [ ] 3.10 `surface-platform` (250)
- [ ] 3.11 `feature-platform` (701)
- [ ] 3.12 `feature-seller-terminal` (24) + `surface-seller-terminal` (5) — vérifier d'abord ce qui
      survit à la phase 2

## Phase 3bis — Consolidation (analyse faite, exécution à venir)

Analyse complète dans `consolidation.md`. Chiffres : 88 termes déjà dans `common`/`domain`
redéfinis dans 286 clés ; 188 termes FR traduits différemment selon l'endroit (746 clés) ;
**5 clés réellement mortes** (et 79 fausses positives, résolues dynamiquement).

- [x] 3b.1 **Lot 0** — hub assaini : `domain.entity.tenant`/`tenants` → « Santral » / « Operator ».
      Découverte en cours de route : **13 des 17 clés mortes de `domain` sont les `domain.entity.*`
      eux-mêmes** — le hub que je voulais corriger n'est référencé nulle part.
- [x] 3b.2 **11 clés mortes supprimées** (× 3 locales) après trois passes de vérification :
      `domain.draw.provider.*` ×4, `domain.entity.sellerTerminal(s)` ×2,
      `dashboard.period.previous_{day,week}` ×2, `shell.error.backendUnavailable.*` ×2,
      `app.nav.dashboard`. Corpus : 4 050 → **4 039 clés**, parité stricte maintenue.
- [ ] 3b.2b **Arbitrage requis** — 20 clés mortes conservées (`domain.entity.*`, `common.print`,
      `common.verify`…) parce qu'elles sont les cibles du Lot A. Les garder implique un refactor
      de composants ; les supprimer implique de renoncer à la mutualisation. Voir `consolidation.md`.
- [x] 3b.3 **Lot B** — divergences alignées sans fusionner. 186 groupes au départ → **88 restants**
      (733 clés → 220). Trois passes : variantes non traduites, fuites françaises dans l'anglais,
      puis synonymes arbitrés un par un.
      Résolus au passage : `Tablo debò` (contre `Tablo bò` / `Tablo kontwòl`), `Needs setup`,
      `Verify a ticket`, `Kanal tiraj` (contre `Règ tiraj`, qui voulait dire « règles de tirage »),
      `Deviz` (contre `Lajan` = argent), `Premye lo` / `1st prize` pour les paliers borlette.
      Trois groupes résolus en corrigeant le **français**, qui était le fautif :
      `Mon entreprise`, `Commission par défaut`, `Nom de famille`.
- [ ] 3b.3b **Lot B, reste** — 88 groupes / 220 clés de synonymes encore à arbitrer, plus
      4 groupes volontairement laissés (`Paramètres`, `Paiements`, `Jeux disponibles`,
      `Seller-terminals actifs`) dont la divergence est justifiée ou dépend de la phase 3.
- [ ] 3b.4 **Lot A** — fusionner vers `common`/`domain` (~120 clés), un namespace à la fois
- [ ] 3b.5 **Lot C** — ne rien toucher ; documenté dans `consolidation.md` pour que personne
      ne « corrige » ces homonymes plus tard

## Phase 4 — Mobile

- [ ] 4.1 `domain` (45) + `common` (83) — 5 chaînes FR résiduelles en EN
- [ ] 4.2 `feature-seller-terminal` (250) — alignement terminologique sur la table
- [ ] 4.3 `component` (20), `feature-auth` (30), `surface-seller-terminal` (1)
- [ ] 4.4 Vérifier la cohérence web ↔ mobile sur les clés de même nom

## Phase 5 — Vérification

- [ ] 5.1 Re-run audit complet : parité 0 écart, 0 fuite technique, 0 FR résiduel, 0 faute HT listée
- [ ] 5.2 `nx test core-i18n` + `error-i18n-contract.spec.ts` verts
- [ ] 5.3 Revue visuelle des libellés contraints (boutons, colonnes) en HT — la locale la plus longue
- [ ] 5.4 PR (jamais de push direct sur `main`)

### Défaut découvert en phase 3.1 — français sans accents

22 chaînes FR écrites en ASCII pur (« Un probleme est survenu », « champs signales », « n a pas »),
**toutes dans `errors.json`** — donc dans les messages génériques, les plus vus du produit.
Corrigées. Aucune autre occurrence dans le reste du corpus web ni dans le mobile.

### À signaler hors périmètre

- `sales.session_closed` est encore émis par le backend
  (`SaleIssueFactory.java:115`, `case "SESSION_CLOSED"`) alors que `SalesSession` est un concept
  **retiré** du modèle. La copy a été reformulée sans « caisse » / « cashier », mais le code
  d'erreur lui-même devrait disparaître côté backend.

## Décisions tranchées

- **HT garde « Tèminal POS »** (250 occurrences), là où FR dit « Vendeur » et EN « Seller ».
  Terme installé chez les vendeurs bòlèt ; on n'aligne pas de force la langue la mieux écrite du
  corpus sur une cohérence de tableau. Inscrit dans `design.md` et dans le glossaire public,
  et exclu de l'audit (`HT_ALLOWED`). `seller-terminal` reste interdit dans les trois langues.
  → 12 chaînes HT de `errors.json` restaurées sur ce terme après la passe 3.1.

## Hors périmètre (suite recommandée)

- Garde-fou CI : test de parité fr/en/ht + interdiction des identifiants techniques dans les valeurs.
  Sans lui, la dérive corrigée ici reviendra au prochain renommage.
