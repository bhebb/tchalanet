# Tasks: tenant-game-bet-option-config-v1

## Slice 0 — Modèle & ownership

- [x] Réutiliser `BetOption` (catalog.game) — pas de type parallèle.
- [x] Définir `SelectionPolicy` : `EXPLICIT_ONLY`, `EXPLICIT_WITH_AUTO_OPTION`, `IMPLICIT_BEST_MATCH` (flag).
- [x] Trancher l'ownership de la config option (core.pricing vs tenant_game).
- [x] Décider la persistance (nouvelles tables vs extension) — extension JSONB `tenant_game.bet_option_config`.

## Slice 1 — Config tenant (backend)

- [x] Config par `(gameCode, betType)` : options offertes, ordre, visible POS, option par défaut, policy.
- [x] Endpoint tenant-admin read + write, perm `game-pricing.update`, RLS, `@AuditLog`.
- [x] Query : options offertes + policy pour un `(gameCode, betType)`.
- [x] Tests : options désactivées absentes du POS ; policy persistée.

## Slice 2 — Calcul indépendant de la policy

- [x] Vérifier/garantir : `SettlementVariantResolver` tourne quelle que soit la policy pour les options explicites single-variant.
- [ ] Test : mode implicite → client saisit un numéro sans option → meilleure possibilité gagnante + paiement correct (`combined-implicit-bet-coverage-v1`, nécessite `TicketLineCoverage`).
- [x] V0 guard : `IMPLICIT_BEST_MATCH` non exposé/sauvegardable avant `combined-implicit-bet-coverage-v1`.

## Slice 3 — UX admin (Config Jeux)

- [x] Section « Options de vente » avant « Barème » (mode de sélection + options offertes).
- [x] Barème groupé par option commerciale, variantes techniques dessous.
- [x] Réutilise `tch-status-badge`, `tch-console-facts`, composants console ; mobile-first ; tokens `--tch-*`.
- [x] i18n : options via `domain.bet.*` / labels `BetOption` ; copy page `admin.gamesPricing.*`.

## Slice 4 — POS / vendeur

- [x] Le POS liste seulement les options commerciales offertes (jamais les variantes techniques).
- [x] Aperçu/reçu affichent l'option commerciale, pas la variante.

## Validation

- [x] `openspec validate tenant-game-bet-option-config-v1 --strict`.
- [x] Tests backend ciblés.
  - [x] `./mvnw -pl tchalanet-core -am -Dtest=SettlementVariantResolverTest,TicketLinePreparationServiceTest,TicketWinningCalculatorTest -Dsurefire.failIfNoSpecifiedTests=false test`.
- [ ] web build/typecheck ; check mobile/desktop.
  - [x] `pnpm exec tsc -p apps/admin-portal/tsconfig.app.json --noEmit`.
  - [ ] `pnpm nx build admin-portal --configuration=development` échoue encore sans diagnostic applicatif exploitable après le crash esbuild déjà observé.
