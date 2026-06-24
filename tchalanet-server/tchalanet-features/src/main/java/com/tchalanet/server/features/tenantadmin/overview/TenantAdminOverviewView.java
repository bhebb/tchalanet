package com.tchalanet.server.features.tenantadmin.overview;

import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessSection;
import com.tchalanet.server.features.tenantadmin.readiness.model.TenantReadinessStatus;
import com.tchalanet.server.platform.address.api.model.AddressView;
import java.util.List;

/**
 * Response for {@code GET /admin/overview}.
 *
 * Tenant overview is a structural diagnosis / navigation surface (spec
 * dashboard-overview-runtime-v1 §tenant-admin-runtime). It MUST NOT include
 * dashboard KPI fields:
 *   - salesToday
 *   - ticketCountToday
 *   - activeSessions
 *   - openDraws
 *   - dashboard top KPI cards
 *
 * Section list mirrors the spec §11 tenant table (Utilisateurs, Points de
 * vente, Terminaux, Sessions, Tickets/Ventes, Tirages, Jeux & prix, Limites,
 * Promotions, Paramètres, Traductions, Apparence, Rapports).
 */
public record TenantAdminOverviewView(
    TenantHeader header,
    TenantReadinessStatus status,
    int missingCount,
    List<TenantReadinessSection> sections,
    TenantSetupView setup) {

  public record TenantHeader(
      String tenantId,
      String tenantCode,
      String tenantName,
      String timezone,
      String currency,
      String tenantType,
      String tenantStatus,
      AddressView address) {}
}
