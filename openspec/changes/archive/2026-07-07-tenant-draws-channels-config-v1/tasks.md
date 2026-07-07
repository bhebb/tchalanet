# Tasks: tenant-draws-channels-config-v1

## Slice 0 — Calendriers (cadrage, deux calendriers séparés)

- [x] Valider : génération = **provider/result-slot calendar** (`result_slot_calendar_override`, global) ; vente = **business calendar tenant** (`resolveBusinessDay`). Ne jamais mélanger.
- [x] Test : tenant fermé + provider ouvert → draw généré, vente impossible, settle à 0 (cas A).
- [x] Test : provider fermé → pas de génération/ouverture (cas B).
- [x] Test : slot devenu indispo → `CANCELED` + reason (pas `SKIPPED`) ; opened/resulted/settled intacts.
- [x] Idempotence `UNIQUE(tenant, channel, date)`.

## Slice 1 — Canaux (web)

- [x] `draw-channel-provider-card` : modèle provider+slots (pas « devices ») ; purger métriques inventées.
- [x] CTA « Configurer l'offre de jeux → » (chaînage matrice).
- [x] `tch-draw-label` pour l'identité canal/slot ; mobile-first ; i18n `domain.draw.*`.

## Slice 2 — Matrice (web)

- [x] `matrix-game-card` : seul éditable = toggle offre ; `saleReady` via `tch-status-badge`.
- [x] stake/limites en lecture seule (`tch-console-facts`) + provenance ; lien retour canaux si manquant.
- [x] Accordéon par canal sur mobile ; tableau dense en `expanded+`.

## Slice 3 — Result-slot configs (référentiel résultats)

- [ ] Afficher `result_slot.source_cfg` / `projection_cfg` en lecture seule côté tenant admin
      (provenance brute + résumé lisible).
- [x] Ajouter un update expert super-admin pour `source_cfg` / `projection_cfg`, avec validation
      serveur et éviction cache.
- [x] Refuser les updates invalides au lieu de retomber silencieusement sur les defaults runtime.

## Validation

- [x] `openspec validate tenant-draws-channels-config-v1 --strict`.
- [x] Tests `core.draw` ciblés.
- [x] Tests `catalog.resultslot` ciblés.
- [ ] web build/typecheck ; check mobile/desktop light/dark.
  - [x] `pnpm exec tsc -p apps/admin-portal/tsconfig.app.json --noEmit`.
