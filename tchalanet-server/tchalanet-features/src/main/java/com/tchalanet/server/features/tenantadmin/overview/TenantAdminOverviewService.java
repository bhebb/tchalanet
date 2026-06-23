package com.tchalanet.server.features.tenantadmin.overview;

import com.tchalanet.server.common.context.TchRequestContext;
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
 * Consumes {@link TenantReadinessView} for status/sections — overview never
 * recomputes readiness in its own way (one readiness, multiple projections
 * per dashboard-overview-runtime-v1).
 *
 * Performance target ≤ 6 grouped reads (spec §12). V1 currently issues
 *   1. tenant registry lookup (catalog)
 *   2. address lookup
 *   3. readiness assembly (identity + address + seller_terminals checks)
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
        header,
        readiness.status(),
        readiness.missingCount(),
        readiness.sections(),
        setup);
  }

  private TenantAdminOverviewView.TenantHeader buildHeader(TchRequestContext ctx) {
    if (ctx == null || ctx.tenantId() == null) {
      return new TenantAdminOverviewView.TenantHeader(null, null, null, null, null, null, null, null);
    }

    AddressView address = null;
    try {
      address = addressApi.findPrimaryByTenantId(ctx.tenantId()).orElse(null);
    } catch (RuntimeException ignored) {}

    var registry = tenantCatalog.findById(ctx.tenantId()).orElse(null);
    if (registry == null) {
      return new TenantAdminOverviewView.TenantHeader(
          ctx.tenantId().value().toString(),
          ctx.effectiveTenantCode(),
          null,
          ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : null,
          ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : null,
          null,
          null,
          address);
    }
    return new TenantAdminOverviewView.TenantHeader(
        registry.tenantId().value().toString(),
        registry.code(),
        registry.name(),
        registry.timezone() != null ? registry.timezone().toString() : null,
        registry.currency() != null ? registry.currency().getCurrencyCode() : null,
        registry.type() != null ? registry.type().name() : null,
        registry.status() != null ? registry.status().name() : null,
        address);
  }
}
