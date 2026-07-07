# Spec ergonomique — Config Barèmes (prix) & Config Tirages (tenant admin)

> **Statut** : proposition ergonomique (no-code) — à challenger.
> **Périmètre** : `admin-portal` uniquement, niveau **tenant** (pas seller-terminal, pas platform).
> **Objectif** : rendre lisibles et cohérents les 2 chantiers de config opérationnelle d'un tenant :
> (A) barèmes/prix des jeux, (B) tirages. **Aucun champ backend nouveau.**
> **Contraintes transverses obligatoires** : readiness, `docs/conventions/theme.md`,
> `docs/conventions/style.md`, mobile-first.

---

## 0. Principe directeur (les deux chantiers)

Un même anti-pattern est présent dans les deux : **un écran réaffiche, en éditable ou en semi-éditable,
des données qui appartiennent à un autre écran**, ce qui fait croire à l'admin qu'il édite au mauvais
endroit. La règle ergonomique unique qu'on applique partout :

> **Une donnée est éditable à UN seul endroit. Partout ailleurs elle est en LECTURE SEULE, avec un
> badge de provenance et un lien vers son écran d'édition.**

C'est la même logique que la resolution backend : une source de vérité, des consommateurs.

---

## A. Config des barèmes / prix (tenant)

### A.1 Ce que le backend stocke (source de vérité — inchangé)

| Donnée | Source | Granularité | Écran d'édition = **canonique** |
|---|---|---|---|
| **Odds (barème de gains)** | `pricing_odds (tenant_id, game_code, bet_type, bet_option, odds)` | **par variante** de pari | table odds (`console-pricing-table`/`-form`) |
| **min / max stake** | réglages tenant-game (`minStake`, `maxStake`) | **par jeu** | `game-settings.dialog` |
| Limites fines (risque) | `limit_policy` (`MAX_STAKE_PER_LINE`, `PER_TICKET`, `PER_BET_TYPE_PER_TICKET`…) | par ligne / ticket / bet_type | feature `limits` |

**Décision assumée** : odds = *multiplicateur de gain par variante* ; min/max stake = *borne de mise par
jeu*. Ce sont deux axes différents → **min/max stake n'entre JAMAIS dans le formulaire d'odds** (sinon
on répète la même valeur pour les 4 options de `HT_LOTO4`).

### A.2 Écran cible : **un jeu = un bloc**

```
┌ Jeu : HT_BOLET ─────────────────────────────────────────────┐
│  [Réglages]  nom affiché · visible POS · disponibilité       │
│              min stake · max stake        (édite ici)         │
├──────────────────────────────────────────────────────────────┤
│  [Barème]    tableau odds — 1 ligne / variante               │
│              MATCH_1_2D → 50× | MATCH_2_2D → 20× | …          │
│              (édite ici, par ligne)                          │
├──────────────────────────────────────────────────────────────┤
│  [Limites]   lecture seule — badge « risque » + lien limits  │
└──────────────────────────────────────────────────────────────┘
```

- Réglages (dont **min/max stake**) et **barème odds** vivent dans le **même bloc jeu**, empilés —
  l'admin pense « je configure HT_BOLET », pas « je vais dans 2 écrans ».
- Les **limites de risque** sont affichées en **lecture seule** (badge) + lien vers `limits`.

### A.3 Question ouverte (à trancher / challenger)

- min/max stake **par jeu** (état actuel, suffisant) **ou par bet_type** ? Si par bet_type → ce n'est
  plus un champ « jeu », c'est de la **limit policy** (`MAX_STAKE_PER_BET_TYPE_PER_TICKET` existe déjà)
  → **pas** un doublon dans le barème. **Défaut recommandé : par jeu.**

---

## B. Config des tirages (tenant) — deux écrans, deux questions

### B.1 Ce que fait chaque écran (confirmé backend)

| Écran | Question | Axe | Édite (canonique) | Réaffiche (doit être RO) |
|---|---|---|---|---|
| **`draw-channels`** (le principal) | *Quels tirages j'ouvre, à quelle heure, comment j'obtiens le résultat ?* | canal + temps + résultat | slots (`drawTime`, `cutoff`, `enabled`), mode d'acquisition résultat | — |
| **`draw-sales-matrix`** (la matrice) | *Sur chaque tirage ouvert, quels jeux je vends ?* | offre jeux × canal | `offeredOnChannel` / `enabledOnChannel` | `minStake/maxStake` (→ barème), `limits` (→ limits) |

**Décision assumée** : garder **deux écrans** — deux tâches mentales, deux fréquences (canaux = rare et
stable ; offre = ajustée à chaque ajout de jeu). Ce n'est **pas** un doublon.

### B.2 Écran cible

- **`draw-channels`** reste la config structurelle (providers → slots → horaires/cutoff/résultat).
  Ajouter un **CTA de continuité** : `Configurer l'offre de jeux →` vers la matrice.
- **`draw-sales-matrix`** : la seule chose éditable = **offrir/activer/retirer un jeu sur un canal**.
  `minStake/maxStake` + limites passent en **lecture seule** (badge « hérité du jeu » / « limites ») +
  lien vers l'écran d'édition. Si un canal manque/inactif → lien retour vers `draw-channels`.
- Conserver **`saleReady`** (par canal×jeu) comme **signal de synthèse** — « ce jeu est réellement
  vendable sur ce tirage ».

### B.3 Ordre logique (matérialisé dans l'UI)

`1) canaux → 2) offre (matrice)`. La matrice **dépend** des canaux (impossible d'offrir sur un canal
inexistant). Le parcours doit refléter cette dépendance (CTA, liens retour), pas la laisser implicite.

---

## C. Contraintes transverses (obligatoires, valent pour A et B)

### C.1 Readiness (source unique = backend `TenantReadinessAssembler`)

- **Ne pas recalculer** les statuts en front. La page consomme les sections readiness :
  `games_pricing`, `draws`, `generated_draws`, `settings`.
- `games_pricing` = READY seulement si ≥1 jeu actif **a des odds configurés** (`pricing.configured`) —
  déjà en place. `MISSING` si aucun, `PARTIAL` si certains seulement.
- Chaque écran affiche **son** statut readiness et pointe la même route que la carte du setup
  (pas de statut divergent entre le setup et l'écran).
- **Limite connue à signaler** : `SetupChecklistStatus` (front) n'a pas de `PARTIAL` → un `PARTIAL`
  retombe visuellement sur `UNKNOWN`. À décider : ajouter `PARTIAL` au composant checklist, ou
  documenter la retombée. (S'applique à `draws` et `games_pricing`.)

### C.2 `theme.md`

- **Zéro couleur en dur.** Uniquement les rôles `--tch-*` (`--tch-color-surface`,
  `--tch-color-on-surface`, `--tch-color-primary`, `--tch-color-error`, `--tch-radius-md`…).
- Badges de statut/provenance = `tch-status-badge` (mapping explicite vers `BadgeStatus`), **jamais**
  une pastille couleur locale.
- Or (`accent`) réservé aux CTA/accents (rôle M3 `tertiary`), pas aux badges d'état.
- Doit rendre correctement en **light / dark / system** et sur le preset `tchalanet` — aucune
  constante dark-mode locale.
- Un composant réutilisable expose ses `--comp-*` **avec fallback `--tch-*`** ; ne crée pas de nouveau
  token global juste pour styler un tableau de barème.

### C.3 `style.md`

- **BEM** + préfixe `tch-` si la brique est générique (les tables/forms odds vivent déjà dans
  `@tch/web/console` → `console-*`).
- Réutiliser les briques console existantes (`tch-admin-section-card`, `tch-admin-detail-layout`,
  `console-pricing-table`, `console-games-table`, `tch-pagination`, `tch-status-badge`) — **ne pas**
  recréer un langage de carte/table par écran.
- `rem` pour typo/espacements ; `px` pour hairlines/icônes ; `%`/`fr`/`min()`/`clamp()` pour le layout.
- Focus-visible sur tout élément interactif (mixin `ui.focus-visible`), touch target ≥ 48px.
- Overrides Material centralisés — pas de `::ng-deep` dans les features.

### C.4 Mobile-first (impératif)

- **Une colonne par défaut**, densification à partir de `medium` (≥ 600px) via `@include bp.up(medium)`
  — jamais de `max-width:600px` desktop-first, jamais de breakpoint en dur.
- **Barème (A)** : sur mobile, le tableau odds passe en **liste de cartes empilées** (pattern
  `data-label` déjà utilisé dans `console-pricing-table`), pas un tableau qui déborde.
- **Matrice (B)** : une grille provider×slot×game **ne tient pas** sur mobile en tableau. Cible mobile =
  **liste par canal → sous-liste de jeux** (accordéon/section), le tableau dense réservé à `expanded+`.
  C'est le point mobile le plus risqué → à prototyper en premier.
- Blocs « réglages jeu + barème » empilés en une colonne sur mobile, côte à côte à partir de `medium`.

---

## D. Ce qui NE change PAS

- **Aucun champ ni endpoint backend nouveau** pour A et B tels que cadrés ici. C'est du parcours, du
  regroupement, du passage en lecture seule, et du responsive.
- Le settlement/payout reste sur snapshot (`TicketLine.oddsSnapshot`) — hors périmètre.
- Pas de migration `V*.sql`.

## E. Recommandation OpenSpec

- **A (barème)** et **B (tirages)** tels que cadrés : **pas d'OpenSpec** (UI only).
- OpenSpec **requis seulement si** on décide : min/max stake **par bet_type** (A.3), ou l'ajout d'un
  niveau de résolution prix **« global Tchalanet »** (hors de ce spec, déjà noté séparément).

## F. À challenger (questions ouvertes)

1. min/max stake **par jeu** vs **par bet_type** (A.3) ?
2. Fusionner « réglages jeu » + « barème » en **une seule vue** par jeu, ou garder le dialog settings
   séparé du tableau odds ?
3. Matrice mobile : **accordéon par canal** vs **par jeu** — lequel est le mental model dominant du
   tenant ?
4. `SetupChecklistStatus` : ajouter `PARTIAL` au composant, ou accepter la retombée sur `UNKNOWN` ?
5. Draw-channels ↔ matrice : deux écrans chaînés (proposé) vs **un seul écran à onglets**
   (Canaux | Offre) ?
