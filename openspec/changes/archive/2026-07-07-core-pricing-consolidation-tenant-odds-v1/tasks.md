# Tasks: core-pricing-consolidation-tenant-odds-v1

## Slice 0 — Décisions figées (cf. design)

- [x] D1 : clé `pricing_odds` = `pricing_variant_code`, unique `(tenant, game, pricing_variant_code)`.
- [x] D2 : ownership `core.pricing`, ancien `catalog.pricing` retiré du runtime cible, Java d'abord, pas de rename SQL slice 1.
- [x] D3 : pas de référentiel pricing plateforme V0 ; odds = runtime tenant + overrides seller-terminal.
- [x] Inventaire des classes `catalog.pricing` à migrer.

## Slice 1 — `PricingVariantCode` + odds keyées variante

- [x] Promouvoir l'enum en `core.pricing.api.model.PricingVariantCode` (absorbe `SettlementVariant` comme clé d'odds) ; `core.sales` l'utilise.
- [x] Ajouter colonne `pricing_variant_code` + contrainte unique (sans exécuter le SQL avant validation pré-go-live).
- [x] Reconciliation seed V38 : 1:1 direct ; BOX → N lignes (valeurs métier requises).
- [x] Préparation de vente : résoudre variante puis snapshoter son odds effective.
- [x] Tests : 112→3-way, 736→6-way ; lookup par variante ; non-rétroactivité.
- [x] (Non bloquant) noter le passage `adminLabel` → clé i18n `pricing.variant.*`.

## Slice 2 — Ownership core.pricing + write tenant

- [x] `core.pricing.api` : `UpsertTenantOddsCommand`, `DeleteTenantOddsCommand`, `ListTenantPricingQuery`.
- [x] Migration classe : `ResolveSellerTerminalOddsQueryHandler` lit les tenant defaults via `TenantPricingOddsReaderPort`, ne lit plus via `catalog.pricing.PricingCatalog`.
- [x] Migration classe : provisioning default Haïti via `EnsureDefaultHaitiLotteryOddsCommand`, suppression `catalog.pricing.PricingProvisioningApi/Service`.
- [x] Migration classe : overview platform ne dépend plus de `catalog.pricing.PricingCatalog`.
- [x] Cache runtime core pricing : tenant odds list/by-variant + éviction sur save + groupe ops `pricing`.
- [x] PageModel superadmin : retrait entrée navigation `/app/platform/catalog/pricing`.
- [x] Suppression legacy catalog `/platform/pricing` : controller/service/web models/API `PricingCatalog`/JPA catalog/page web platform retirés.
- [x] Shell web fallback : retrait entrée `catalog-pricing` de la navigation statique.
- [x] `features.tenantadmin` : `PUT/DELETE /admin/pricing/odds`, perm `game-pricing.update`, RLS, `@AuditLog`.
- [x] `features.tenantadmin` ne dépend que de `core.pricing.api`.
- [x] Décision corrigée : ne pas créer de bridge `/platform/pricing` cible ; endpoint legacy catalog retiré du runtime V0.
- [x] Tests : write tenant-scopé, refus sans permission, audit émis.

## Slice 2b — Seller-terminal overrides keyed by variant

- [x] `seller_terminal_pricing_odds_override` clé active = `(tenant_id, seller_terminal_id, game_code, pricing_variant_code)`.
- [x] API/command/view override portent `pricingVariantCode`; `betType`/`betOption` restent descriptifs.
- [x] Résolution runtime lit l'override par `PricingVariantCode`, puis tenant default, puis erreur.
- [x] Test : override seller-terminal variant-specific.

## Slice 3 — Web barème éditable

- [x] `admin-games-pricing-api` : méthodes write.
- [x] Barème groupé option→variantes, éditable (`console-pricing-table`/`-form`), mobile-first, tokens `--tch-*`.
- [x] i18n `domain.bet.*` / `domain.entity.odds`.
- [x] Première passe : `admin-pricing-api` branché sur `/admin/pricing/odds` + édition tenant odds via dialog.

## Validation

- [x] `openspec validate core-pricing-consolidation-tenant-odds-v1 --strict`.
- [x] `./mvnw test` ciblé sur `core.pricing`, `core.sales`, `features.tenantadmin` touchés.
- [x] `./mvnw -pl tchalanet-features -am -DskipTests compile`.
- [x] `./mvnw -pl tchalanet-app -am -Dmaven.test.skip=true compile`.
- [ ] web build/typecheck.
  - [x] `pnpm exec tsc -p apps/admin-portal/tsconfig.app.json --noEmit`.
  - [x] `pnpm exec tsc -p apps/platform-portal/tsconfig.app.json --noEmit`.
  - [x] `pnpm exec tsc -p libs/web/shell/tsconfig.lib.json --noEmit`.
  - [x] `pnpm exec tsc -p libs/web/console/tsconfig.lib.json --noEmit`.
  - [x] `pnpm nx test web-console --watch=false`.
  - [x] ESLint ciblé sur les fichiers pricing touchés.
  - [ ] `pnpm nx build admin-portal --configuration=development` bloque sur un crash esbuild (`fatal error: all goroutines are asleep - deadlock`) après correction SCSS.
