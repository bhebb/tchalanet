# Proposal: tenant-draws-channels-config-v1

## Summary

Clarifier et rendre cohérente la **configuration des tirages** du tenant, indépendamment du pricing
(qui vit dans les 3 changes pricing/betoption). Deux écrans, deux questions : `draw-channels`
(quels tirages, horaires, résultat) et `draw-sales-matrix` (quels jeux vendus sur quels canaux).
Expliciter **ce qui régénère les draws quotidiens** en tenant compte des **calendar overrides**.

## Goals

1. `draw-channels` : afficher provider + slots (`draw_time`, `cutoff`, `timezone`, mode résultat) —
   modèle provider/slot (pas « devices »).
2. `draw-sales-matrix` : seule édition = offre jeu×canal ; stake/limites en **lecture seule** +
   provenance ; `saleReady` comme signal de synthèse.
3. Chaînage : `draw-channels → offre` et retour matrice → canaux si canal manquant.
4. Deux calendriers **jamais mélangés** (cf. `docs/CALENDARS.md`, `openspec/context/86`), et
   **génération ≠ ouverture des ventes** :
   - **Génération des draws** (`core.draw`, J→J+7) = canaux actifs × result-slot (daysOfWeek/actif) ×
     **provider calendar** (`result_slot_calendar_override`, global, SUPER_ADMIN). Idempotent
     (`UNIQUE(tenant, channel, date)`), non-rétroactif (slot devenu indispo → `CANCELED` + reason,
     pas de `SKIPPED`).
   - **Ouverture des ventes** = draw généré × canal actif × fenêtre cutoff/open × **business calendar
     tenant** (`resolveBusinessDay` : override daté > holidays > closedWeekdays > defaultOpen).
   - **Cas A** : tenant fermé + provider ouvert → draw **généré**, vente **impossible**, settle à 0.
   - Opt-out par-date/par-canal (`draw_channel_calendar_override`) = futur, pas V0.
5. Readiness = source backend ; front n'affiche pas ne recalcule pas.
6. Les configs JSON de `result_slot` sont des configs provider/platform, pas tenant :
   - `source_cfg` décrit comment récupérer le résultat externe.
   - `projection_cfg` décrit comment projeter ce résultat en résultat Haïti.
   Elles sont affichées en référentiel résultats, modifiables seulement par super admin via un
   update expert validé. Tenant admin = lecture seule.

## Non-Goals

- Pricing / barème / options (couverts par les 3 changes pricing/betoption).
- Matrice `canal × jeu × option`.
- Config d'options de pari dans l'écran canaux.
- SQL sans validation pré-go-live.

## Règles design (transverses)

- Une donnée éditable à un seul endroit ; ailleurs lecture seule + provenance.
- Mobile-first (`bp.up(medium/expanded)`) ; matrice = accordéon par canal sur mobile.
- Une carte = un composant ; page = orchestrateur ; réutiliser briques console (`tch-draw-label`,
  `tch-console-facts`, `tch-status-badge`) ; tokens `--tch-*` ; pas de chrome global recréé ;
  pas de Tailwind/CDN/HTML brut (mockups Stitch = référence UX only).

## Impact

- **Backend** : `core.draw` — valider/documenter la prise en compte des calendar overrides à la
  régénération ; `catalog.resultslot` / `core.drawresult` / `core.haiti` — cadrer l'édition validée
  des configs `source_cfg` et `projection_cfg`.
- **Web** : `draw-channels`, `draw-sales-matrix` (restructuration cartes, lecture seule + provenance,
  chaînage, mobile-first) ; référentiel résultats pour afficher les configs result-slot brutes et
  lisibles.
