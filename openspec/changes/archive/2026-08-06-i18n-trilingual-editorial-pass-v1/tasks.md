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
- [x] 3.2 `feature-public` (440) — **140 chaînes réécrites**. Le renommage avait fusionné *trois*
      concepts (point de vente, terminal, vendeur) en « seller-terminal », produisant des phrases
      cassées : « son seller-terminal, son seller-terminal », « their seller-terminal-terminal »,
      « Combien de seller-terminals ou seller-terminals ». Reconstituées sur l'acteur unique du
      modèle. 2 clés mortes supprimées (`cta.close_connexion`, `hero_stat_connexions`).
- [x] 3.3 `surface-public` (48)
> **3.4 à 3.12 — absorbées par les passes transverses.** Le découpage par namespace supposait que
> chaque défaut était local. Il ne l'était pas : `tenant` et `seller-terminal` traversaient tous les
> namespaces, et les traiter terme par terme (phases 3ter et 3quater) a été plus sûr que fichier par
> fichier. Le résultat est vérifié par l'audit, pas par la liste ci-dessous.

- [x] 3.4 `common` (105)
- [x] 3.5 `component` (120)
- [x] 3.6 `domain` (118) — socle terminologique, aligne les autres
- [x] 3.7 `feature-auth` (80)
- [x] 3.8 `surface-admin` (174)
- [x] 3.9 `feature-admin` (1367)
- [x] 3.10 `surface-platform` (250)
- [x] 3.11 `feature-platform` (701)
- [x] 3.12 `feature-seller-terminal` + `surface-seller-terminal`

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
- [ ] 3b.2b **Arbitrage requis** *(reporté → `openspec/BACKLOG.md`)* — 20 clés mortes conservées (`domain.entity.*`, `common.print`,
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
- [ ] 3b.3b **Lot B, reste** *(reporté → `consolidation.md`)* — 88 groupes / 220 clés de synonymes encore à arbitrer, plus
      4 groupes volontairement laissés (`Paramètres`, `Paiements`, `Jeux disponibles`,
      `Seller-terminals actifs`) dont la divergence est justifiée ou dépend de la phase 3.
- [ ] 3b.4 **Lot A** *(suspendu — dépend de l'arbitrage 3b.2b)* — fusionner vers `common`/`domain` (~120 clés), un namespace à la fois
- [x] 3b.5 **Lot C** — documenté dans `consolidation.md` ; rien à faire, c'est le but

## Phase 3ter — Éradication de « tenant » (transverse)

- [x] 3t.1 **378 chaînes réécrites** sur 6 namespaces. Règle du `design.md` appliquée par chaîne :
      **Santral / Operator / Santral** quand la chaîne nomme l'organisation,
      **Espace / Workspace / Espas** quand elle nomme le périmètre de l'utilisateur.
      Le mot ne figure plus dans aucune valeur, dans aucune locale.
- [x] 3t.2 9 **clés littérales pointées** normalisées en imbrication
      (`"advancedSettings.title"` dans `admin.setup`). `ngx-translate` résout les deux formes,
      mais tout outil qui parcourt l'arbre casse dessus.
- [x] 3t.3 3 placeholders `{{tenant}}` restaurés — la substitution les avait renommés.
      Le nom d'un placeholder est un contrat avec le code (`app.html:23` passe
      `{ tenant: session.tenantName }`). L'audit ignore désormais le contenu des placeholders.

## Phase 3quater — Éradication de « seller-terminal » et des concepts retirés

- [x] 3q.1 **116 chaînes réécrites** sur 7 namespaces → Vendeur / Seller / Tèminal POS.
      Formes concurrentes supprimées : `Seller-terminal`, `seller terminal`, `Terminal vendeur`,
      `Terminaux vendeurs`, `Tèminal vandè`.
- [x] 3q.2 Concept **caissier** retiré des 4 dernières clés (`dashboard.titles.cashier`,
      `surface.cashier`, `nav.cashier.*`).
- [x] 3q.3 `TENANT_ADMIN` affiché brut dans `platform.tenants.admin.roleInfo` → « Administrateur ».
- [x] 3q.4 **7 « a operator » corrigés en « an operator »** — régression introduite par la passe
      tenant. L'article anglais ne suit pas une substitution de mot.
- [x] 3q.5 **0 fuite technique dans les trois locales** (départ : 283 fr / 319 en / 23 ht).

## Phase 4 — Mobile

- [x] 4.1 `domain` — `tenant`/`tenants` et `sellerTerminal(s)` alignés sur le glossaire
- [x] 4.2 `feature-auth:auth.login.blocked_message` — « votre tenant » / « ce terminal »
      réécrit dans les 3 locales
- [x] 4.3 `pos.tickets.outlet` — « POINT DE VENTE » / « OUTLET » / « PÒS VANT », concept retiré
- [x] 4.4 Mobile à **0 défaut** sur les 5 classes de l'audit

## Phase 5 — Vérification

- [x] 5.1 Audit complet : **0 défaut**, `--strict` sort en 0. Web 4 033 × 3, mobile 429 × 3.
- [x] 5.2 `nx test core-i18n` (4) + `web-errors` (13) verts ; builds public/admin/platform verts
- [ ] 5.3 Revue visuelle des libellés contraints en HT *(à faire par le relecteur trilingue)*
- [x] 5.4 PR : #541 → #542 → #543 → #545 → #546 (pile), + #544 pour la CI docs

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

---

## Clôture — 2026-08-05

**997 défauts → 0.** `scripts/i18n-audit.py --strict` sort en 0 sur `main`.

| | web | mobile |
|---|---|---|
| Clés × 3 locales | 4 033 | 429 |
| Parité · fuites techniques · FR résiduel · IPN | 0 | 0 |
| Placeholders · valeurs vides | 0 | 0 |

Livré par #541 puis #552 (qui a remplacé #542/#545/#546 : le dépôt fait du squash-merge, ce qui
rend les PR empilées inmergeables — la base disparaît sous un SHA neuf et l'enfant conflicte avec
son propre contenu).

Le garde-fou est branché dans `web-pr.yml` et `mobile-pr.yml`, et détecte désormais aussi les
objets JSON dupliqués — le défaut qui avait rendu 88 traductions anglaises inatteignables.

**Reste ouvert, suivi ailleurs :** les 26 `labelKey` PageModel sans traduction et
`sales.session_closed` (→ `openspec/BACKLOG.md`) ; la longue traîne du Lot B et l'arbitrage du
Lot A (→ `consolidation.md`).
