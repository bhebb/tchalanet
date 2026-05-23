# Tasks

## 1. Mobile runtime profile

- [ ] Add `TchSurface` enum: `POS_HANDHELD`, `MOBILE`, `TABLET`.
- [ ] Add `TchScreenClass` enum: `COMPACT`, `PHONE`, `TABLET`.
- [ ] Add `TchRuntimeProfile` with surface, screenClass, size, orientation, safeArea, capabilities.
- [ ] Expose runtime profile to widgets through a top-level scope/context extension.
- [ ] Expose runtime profile to Dio/API layer for `X-Tch-Surface`.
- [ ] Add POS/mobile layout guardrails for cashier home and sell flow.

## 2. Profile/current bootstrap

- [ ] Decide final package owner: `features/profile/current` for BFF bootstrap; keep `platform.identity.api` for identity-only API.
- [ ] Return `startupState`, `blockers`, `allowedNextActions`, device state, operational context state, subscription/capabilities summary.
- [ ] Ensure `profile/current` returns `200` with blockers for non-ready startup states.
- [ ] Add tests for `DEVICE_NOT_ENROLLED`, `SESSION_CLOSED`, `READY_FOR_CASHIER`, `PLAN_FEATURE_BLOCKED`.

## 3. Device binding and operational context

- [ ] Add/confirm device enrollment model and signed device headers.
- [ ] Resolve device binding in context pipeline without a separate OperationalContextFilter.
- [ ] Accept trusted operational context only from `SIGNED_DEVICE_BINDING` or `ADMIN_SELECTION` for sensitive actions.
- [ ] Block cashier/home, preview, sell, payout, offline grant/sync if context is `CLIENT_CLAIM` or `NONE`.
- [ ] Add backend tests for direct API calls that bypass Flutter screens.

## 4. Seller preview/sell

- [ ] Add `POST /tenant/seller/tickets/preview` or map to existing ticket preview path.
- [ ] Add `PREVIEW_TICKET` action and response model.
- [ ] Ensure `SELL_TICKET` revalidates all critical checks without relying on `previewId`.
- [ ] Require `Idempotency-Key` on final sell.
- [ ] Add tests: preview OK then sell rejected because session/plan/cutoff changed.

## 5. I18n/backend dictionary

- [ ] Document `catalog.i18n` scope-aware resolution rules.
- [ ] Add bundle resolve endpoint or include minimal seller dictionary in `profile/current`.
- [ ] Standardize backend action/blocker contract: `code`, `messageKey`/`labelKey`, `fallback`, `params`.
- [ ] Ensure PLATFORM scope never implicitly merges TENANT overrides.
- [ ] Add tests for `resolveLocale` and `resolveLocaleForTenant` cross-tenant safety.

## 6. Ticket document i18n

- [ ] Define `TicketReceiptDocument` model with resolved labels.
- [ ] Ensure document renderer receives resolved labels and does not translate.
- [ ] Store language and critical label snapshot on final sale.
- [ ] Add reprint test: tenant changes override after sale; old ticket reprint keeps old labels.
