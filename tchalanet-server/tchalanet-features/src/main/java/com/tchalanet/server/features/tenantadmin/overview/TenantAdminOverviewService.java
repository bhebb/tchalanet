package com.tchalanet.server.features.tenantadmin.overview;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.features.shared.bff.BffSlicePolicy;
import com.tchalanet.server.features.shared.bff.BffSlices;
import com.tchalanet.server.features.tenantadmin.error.TenantAdminErrorCodes;
import com.tchalanet.server.features.tenantadmin.readiness.TenantReadinessAssembler;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessView;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.address.api.model.AddressView;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Assembles {@code GET /admin/overview} responses (tenant overview).
 *
 * <p>Consumes {@link TenantReadinessView} for status/sections — overview never recomputes readiness
 * in its own way (one readiness, multiple projections per dashboard-overview-runtime-v1).
 *
 * <p>Performance target ≤ 6 grouped reads (spec §12). V1 currently issues 1. tenant registry lookup
 * (catalog) 2. address lookup 3. readiness assembly (identity + address + seller_terminals checks)
 * which remains well within budget.
 */
@Service
@RequiredArgsConstructor
public class TenantAdminOverviewService {

  private final TenantPreContextLookupApi tenantCatalog;
  private final TenantReadinessAssembler readinessAssembler;
  private final AddressApi addressApi;

  public TenantAdminOverviewView getOverview(TchRequestContext ctx) {
    TenantReadinessView readiness = readinessAssembler.assemble(ctx);
    TenantSetupView setup = readinessAssembler.computeSetup(readiness);
    TenantAdminOverviewView.TenantHeader header = buildHeader(ctx);

    return new TenantAdminOverviewView(
        header, readiness.status(), readiness.missingCount(), readiness.sections(), setup);
  }

  private TenantAdminOverviewView.TenantHeader buildHeader(TchRequestContext ctx) {
    if (ctx == null || ctx.effectiveTenantIdOrNull() == null) {
      return new TenantAdminOverviewView.TenantHeader(
          null, null, null, null, null, null, null, null, false, false);
    }

    var tenantId = ctx.tenantIdRequired();
    HeaderSlice<AddressView> address = loadAddress(tenantId);
    var registry = loadRegistry(ctx);
    if (!registry.available() || registry.value() == null) {
      return new TenantAdminOverviewView.TenantHeader(
          tenantId.value().toString(),
          ctx.effectiveTenantCode(),
          null,
          ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : null,
          ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : null,
          null,
          null,
          address.value(),
          registry.available(),
          address.available());
    }
    return new TenantAdminOverviewView.TenantHeader(
        registry.value().tenantId().value().toString(),
        registry.value().code(),
        registry.value().displayName(),
        registry.value().timezone() != null ? registry.value().timezone().toString() : null,
        registry.value().currency() != null ? registry.value().currency().getCurrencyCode() : null,
        registry.value().type() != null ? registry.value().type().name() : null,
        registry.value().status() != null ? registry.value().status().name() : null,
        address.value(),
        true,
        address.available());
  }

  private HeaderSlice<AddressView> loadAddress(
      com.tchalanet.server.common.types.id.TenantId tenantId) {
    return BffSlices.optional(
        BffSlicePolicy.warn(
                TenantAdminErrorCodes.OVERVIEW_ADDRESS_UNAVAILABLE.code(),
                "features.tenantadmin",
                NoticeSource.of("tenantAdminOverview").operation("loadAddress"),
                HeaderSlice.<AddressView>unavailable())
            .target("tenantadmin.overview.address"),
        () -> HeaderSlice.available(addressApi.findPrimaryByTenantId(tenantId).orElse(null)));
  }

  private HeaderSlice<com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView>
      loadRegistry(TchRequestContext ctx) {
    return BffSlices.optional(
        BffSlicePolicy.warn(
                TenantAdminErrorCodes.OVERVIEW_REGISTRY_UNAVAILABLE.code(),
                "features.tenantadmin",
                NoticeSource.of("tenantAdminOverview").operation("loadRegistry"),
                HeaderSlice
                    .<com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView>
                        unavailable())
            .target("tenantadmin.overview.registry"),
        () -> HeaderSlice.available(tenantCatalog.findById(ctx.tenantIdRequired()).orElse(null)));
  }

  private record HeaderSlice<T>(T value, boolean available) {

    private static <T> HeaderSlice<T> available(T value) {
      return new HeaderSlice<>(value, true);
    }

    private static <T> HeaderSlice<T> unavailable() {
      return new HeaderSlice<>(null, false);
    }
  }
}
