package com.tchalanet.server.features.privatedashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.ServiceHealth;
import com.tchalanet.server.common.web.api.ServiceStatus;
import com.tchalanet.server.core.pagemodel.application.query.model.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.features.pagemodel.dashboard.app.PageModelTypeResolver;
import com.tchalanet.server.features.privatedashboard.block.PrivateDashboardDynamicPayload;
import com.tchalanet.server.features.privatedashboard.dynamic.PrivateDashboardDynamicDataService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrivateDashboardService {

  private final QueryBus queryBus;
  private final LangResolver langResolver;
  private final TchContextResolver contextResolver;
  private final PrivateDashboardDynamicDataService dynamicDataService;
  private final PageModelTypeResolver pageModelTypeResolver;

  public ApiResponse<PrivateDashboardResponse> getDashboard(
      Optional<String> langFromUrl, UserId userId, String userPreferredLang) {
    var holder = contextResolver.currentOrNull();
    var tenantId = holder != null ? TenantId.of(holder.tenantUuid()) : null;
    var role = holder != null ? holder.currentRole() : null;
    var type = pageModelTypeResolver.forDashboard(role);
    PageModelDoc pageModel = queryBus.send(new ResolveEffectivePageModelQuery(
        Optional.ofNullable(tenantId), type.logicalId()));

    var meta = pageModel != null ? pageModel.meta() : null;
    var ctx =
        new LangResolver.LangResolverContext(
            langFromUrl,
            Optional.ofNullable(userPreferredLang),
            Optional.empty(),
            Optional.ofNullable(meta != null ? meta.defaultLang() : null),
            meta != null && meta.langs() != null ? meta.langs() : List.of(),
            "fr");

    var currentLang = langResolver.resolve(ctx);
    List<String> langs = meta != null && meta.langs() != null ? meta.langs() : List.of(currentLang);

    // Try to build dynamic data, catch failures
    List<ServiceStatus> services = List.of();
    List<ApiNotice> notices = List.of();
    PrivateDashboardDynamicPayload dynamic;
    try {
      dynamic = dynamicDataService.buildDynamicData(tenantId, userId, role, currentLang, pageModel);
    } catch (Exception e) {
      dynamic = null;
      services =
          List.of(
              new ServiceStatus(
                  "dynamicDataService",
                  ServiceHealth.DOWN,
                  "Failed to load dynamic data: " + e.getMessage()));
      notices =
          List.of(
              new ApiNotice(
                  "SERVICE_DEGRADED",
                  "Some dashboard content may be unavailable",
                  "private_dashboard",
                  NoticeSeverity.WARN,
                  Map.of()));
    }

    var response = new PrivateDashboardResponse(currentLang, langs, pageModel, dynamic, Map.of());

    if (!services.isEmpty()) {
      return ApiResponse.partial(response, services, notices);
    } else {
      return ApiResponse.success(response);
    }
  }

  public ApiResponse<PrivateDashboardDynamicPayload> getTenantDashboardForSuperadmin(
      TenantId tenantId, Optional<String> lang, UserId userId) {
    var type = pageModelTypeResolver.forDashboard(TchRole.TENANT_ADMIN);
    PageModelDoc pageModel = queryBus.send(new ResolveEffectivePageModelQuery(
        Optional.of(tenantId), type.logicalId()));

    String resolvedLang =
        langResolver.resolve(
            new LangResolver.LangResolverContext(
                lang, Optional.empty(), Optional.empty(), Optional.empty(), List.of(), "fr"));

    List<ServiceStatus> services = List.of();
    List<ApiNotice> notices = List.of();
    PrivateDashboardDynamicPayload payload;
    try {
      payload =
          dynamicDataService.buildDynamicData(
              tenantId, userId, TchRole.TENANT_ADMIN, resolvedLang, pageModel);
    } catch (Exception e) {
      payload = null;
      services =
          List.of(
              new ServiceStatus(
                  "dynamicDataService",
                  ServiceHealth.DOWN,
                  "Failed to load tenant dashboard data: " + e.getMessage()));
      notices =
          List.of(
              new ApiNotice(
                  "SERVICE_DEGRADED",
                  "Tenant dashboard content may be unavailable",
                  "private_dashboard",
                  NoticeSeverity.WARN,
                  Map.of()));
    }

    if (!services.isEmpty()) {
      return ApiResponse.partial(payload, services, notices);
    } else {
      return ApiResponse.success(payload);
    }
  }

  public ApiResponse<PrivateDashboardDynamicPayload> getCashierDashboardForSuperadmin(
      TenantId tenantId, UserId cashierId, Optional<String> lang, UserId userId) {
    var type = pageModelTypeResolver.forDashboard(TchRole.CASHIER);
    PageModelDoc pageModel = queryBus.send(new ResolveEffectivePageModelQuery(
        Optional.of(tenantId), type.logicalId()));

    String resolvedLang =
        langResolver.resolve(
            new LangResolver.LangResolverContext(
                lang, Optional.empty(), Optional.empty(), Optional.empty(), List.of(), "fr"));

    List<ServiceStatus> services = List.of();
    List<ApiNotice> notices = List.of();
    PrivateDashboardDynamicPayload payload;
    try {
      payload =
          dynamicDataService.buildDynamicData(
              tenantId, cashierId, TchRole.CASHIER, resolvedLang, pageModel);
    } catch (Exception e) {
      payload = null;
      services =
          List.of(
              new ServiceStatus(
                  "dynamicDataService",
                  ServiceHealth.DOWN,
                  "Failed to load cashier dashboard data: " + e.getMessage()));
      notices =
          List.of(
              new ApiNotice(
                  "SERVICE_DEGRADED",
                  "Cashier dashboard content may be unavailable",
                  "private_dashboard",
                  NoticeSeverity.WARN,
                  Map.of()));
    }

    if (!services.isEmpty()) {
      return ApiResponse.partial(payload, services, notices);
    } else {
      return ApiResponse.success(payload);
    }
  }
}
