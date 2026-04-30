# Cache and Event Audit

This file records the implementation audit for `harden-common-runtime-infra`.
It is intentionally scoped to runtime cache and event hardening; source-of-truth
requirements remain in `specs/common-cache/spec.md` and `specs/common-events/spec.md`.

## Cache Inventory

### CacheSpecProvider declarations

- `common.batch.gate.BatchCacheConfig`
- `core.accesscontrol.infra.cache.AuthCacheConfig`
- `core.draw.infra.cache.DrawCacheSpecProvider`
- `core.outlet.infra.cache.OutletCacheConfig`
- `core.uslottery.infra.cache.UsLotteryCacheSpecProvider`
- `features.news.NewsCacheConfig`

### Direct cache manager access

- `common.batch.gate.BatchFlagCache`
- `common.web.CacheAdminController`
- `core.drawresult.infra.cache.DrawResultCacheEvictor`
- `core.uslottery.infra.cache.UsLotteryProviderRawCache`
- `features.news.CombinedNewsCache`

### `@Cacheable` users

- Catalog referentials:
  - `catalog.drawchannel.internal.read.DrawChannelCatalogImpl`
  - `catalog.game.internal.read.GameCatalogImpl`
  - `catalog.i18n.internal.read.I18nOverridesCatalogImpl`
  - `catalog.pagemodeltemplate.internal.read.PageModelTemplateCatalogImpl`
  - `catalog.plan.internal.read.PlanCatalogImpl`
  - `catalog.pricing.internal.read.PricingCatalogImpl`
  - `catalog.resultslot.internal.read.ResultSlotCatalogImpl`
  - `catalog.settings.internal.read.SettingsCatalogImpl`
  - `catalog.theme.internal.read.ThemePresetCatalogImpl`
- Core runtime:
  - `core.accesscontrol.infra.persistence.PermissionCatalogAdminAdapter`
  - `core.accesscontrol.infra.persistence.RolePermissionReaderAdapter`

### `@CacheEvict` users

- Catalog writes:
  - `catalog.drawchannel.internal.write.DrawChannelAdminService`
  - `catalog.drawchannel.internal.write.DrawChannelGameAdminService`
  - `catalog.game.internal.write.GameAdminService`
  - `catalog.i18n.internal.write.I18nOverridesAdminService`
  - `catalog.pagemodeltemplate.internal.write.PageModelTemplateAdminService`
  - `catalog.plan.internal.write.PlanAdminService`
  - `catalog.pricing.internal.write.PricingAdminService`
  - `catalog.resultslot.internal.write.ResultSlotAdminService`
  - `catalog.settings.internal.write.SettingsAdminService`
  - `catalog.theme.internal.write.ThemePresetAdminService`
- Core writes:
  - `core.accesscontrol.infra.persistence.PermissionCatalogAdminAdapter`
  - `core.tenantconfig.infra.persistence.TenantPersistenceAdapter`

### Ownership Findings

- `core.draw.*` specs are now owned by `core.draw.infra.cache`.
- US Lottery raw cache access lives under `core.uslottery.infra.cache` and uses `infra.uslottery.provider_raw`.
- Catalog referential cache annotations live under `catalog.<domain>`.
- News feature cache access lives under `features.news`.
- Remaining legacy names without dotted ownership:
  - `tenant_outlet`, `tenant_terminal`, `tenant_outlet_tree`
  - `user_profile`, `role-permissions`
  - `batch.flags`
    These are intentionally left as migration debt to avoid changing public cache keys in this pass.
- `BatchCacheConfig` now passes `CacheSpec.of(name, ttlL1, ttlL2)` in the correct order: L1 15 seconds, L2 2 minutes.
- Simple cache spec declarations now implement `CacheSpecProvider` directly as `@Component` beans.

### Eviction Findings

- Direct broad eviction remains in:
  - `common.batch.gate.BatchGateCacheImpl.evictAll`
  - `core.draw.infra.event.DrawDomainEventListener`
  - `core.drawresult.infra.cache.DrawResultCacheEvictor`
- `@CacheEvict(allEntries = true)` remains common in catalog admin services.
- Broad eviction is MVP debt; targeted eviction should be introduced per bounded context when the write path has enough key material.

## Event Inventory

### DomainEvent implementations

- `core.draw.domain.event.DrawResultAppliedEvent`
- `core.draw.domain.event.DrawSettledEvent`
- `core.drawresult.domain.event.DrawResultIngestedEvent`
- `core.outlet.domain.event.OutletConfigUpdatedEvent`
- `core.outlet.domain.event.OutletDayClosedEvent`
- `core.outlet.domain.event.OutletDayReopenedEvent`
- `core.pagemodel.domain.event.PageModelResetEvent`
- `core.payout.infra.event.PayoutRegisteredEvent`
- `core.sales.domain.event.TicketCancelledEvent`
- `core.sales.domain.event.TicketPaidEvent`
- `core.sales.domain.event.TicketPaymentPendingEvent`
- `core.sales.domain.event.TicketPlacedEvent`
- `core.sales.domain.event.TicketResultedEvent`
- `core.sales.domain.event.TicketResultOverriddenEvent`
- `core.session.domain.event.SessionClosedEvent`
- `core.session.domain.event.SessionOpenedEvent`
- `core.tenantconfig.domain.event.TenantIdentityUpdatedEvent`
- `core.tenantconfig.domain.event.TenantStatusChangedEvent`

### DomainEventPublisher calls

- After-commit publication already used by:
  - `core.draw.application.command.handler.ApplyExternalResultsWindowCommandHandler`
  - `core.draw.application.command.handler.SettleDrawsCommandHandler`
  - `core.drawresult.application.command.handler.OverrideDrawResultCommandHandler`
  - `core.outlet.application.command.handler.CloseOutletDayCommandHandler`
  - `core.outlet.application.command.handler.ReopenOutletDayCommandHandler`
  - `core.outlet.application.command.handler.UpdateOutletConfigCommandHandler`
  - `core.pagemodel.application.command.handler.ResetPageModelHandler`
  - `core.payout.application.command.handler.ExecutePayoutCommandHandler`
  - `core.payout.application.command.handler.MarkTicketPayoutPaidCommandHandler`
  - `core.payout.application.command.handler.MarkTicketPayoutPendingCommandHandler`
  - `core.payout.application.command.handler.RegisterPayoutCommandHandler`
  - `core.sales.application.command.handler.ApproveTicketSaleCommandHandler`
  - `core.sales.application.command.handler.CancelSaleCommandHandler`
  - `core.sales.application.command.handler.OverrideTicketResultCommandHandler`
  - `core.sales.application.command.handler.RecordDrawTicketsResultCommandHandler`
  - `core.sales.application.command.handler.SellTicketCommandHandler`
  - `core.session.application.command.handler.CloseSessionCommandHandler`
  - `core.session.application.command.handler.OpenSessionCommandHandler`
  - `core.tenantconfig.application.command.handler.*Tenant*CommandHandler`

### Event listeners

- Safe synchronous listeners:
  - `common.event.infra.spring.LoggingDomainEventListener` logs only.
  - `common.security.bootstrap.KeycloakBootstrapSyncListener` listens for `ApplicationReadyEvent`, not domain state.
- After-commit side-effect listeners:
  - `core.draw.infra.event.DrawDomainEventListener`
  - `core.limitpolicy.infra.event.LimitPolicyEventsListener`
  - `core.pagemodel.infra.event.PageModelTemplateUpdatedListener`
  - `core.payout.infra.event.PayoutLedgerListener`
  - `core.sales.application.event.DrawResultedEventListener`
  - `core.sales.infra.event.SalesLedgerListener`
  - `core.session.infra.event.SalesSessionTotalsProjectionListener`
  - `features.stats.aggregates.StatsAggregatesEventListener`

### Ownership Findings

- Event classes generally live with their producer bounded context.
- `PayoutRegisteredEvent` currently lives in `core.payout.infra.event` while it is used as a domain event. This is legacy debt; it should move to `core.payout.domain.event` in a dedicated payout cleanup.
- `DrawResultIngestedEvent` is still consumed by `DrawDomainEventListener` for draw cache eviction. This is the known semantic mismatch: ingestion is global/provider state, while draw cache invalidation should react to `DrawResultAppliedEvent`. The actual migration is deferred to `align-draw-uslottery-results-pipeline`.

### Idempotency Findings

- `ProcessedEventPort` now exposes `markProcessedIfAbsent(handlerKey, eventId)`.
- Cross-domain consumers using `processed_event` now use the atomic method instead of `alreadyProcessed(...)` followed by `markProcessed(...)`.
- `StatsAggregatesEventListener` now reserves the event id through the `stats_event_log` primary key before applying projections, inside the same listener transaction.

## Deferred Work

- Add tests for `markProcessedIfAbsent` duplicate handling.
- Add ArchUnit rule that side-effect listeners use `@TransactionalEventListener(AFTER_COMMIT)`.
- Move `PayoutRegisteredEvent` to a producer-owned domain event package.
- Replace `DrawResultIngestedEvent` cache invalidation with `DrawResultAppliedEvent` after the draw/results pipeline change.
- Convert legacy broad cache eviction to targeted eviction where key material is available.
