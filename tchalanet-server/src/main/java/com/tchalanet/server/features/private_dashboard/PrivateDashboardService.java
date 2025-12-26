package com.tchalanet.server.features.private_dashboard;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.ServiceHealth;
import com.tchalanet.server.common.web.api.ServiceStatus;
import com.tchalanet.server.features.i18n.TenantI18nOverrideService;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.PageModelService;
import com.tchalanet.server.features.pagemodel.shared.PageModelTypeResolver;
import com.tchalanet.server.features.private_dashboard.block.PrivateDashboardDynamicPayload;
import com.tchalanet.server.features.private_dashboard.dynamic.PrivateDashboardDynamicDataService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrivateDashboardService {

  private final PageModelService pageModelService;
  private final LangResolver langResolver;
  private final TchRequestContextHolder tenantContext;
  private final TenantI18nOverrideService i18nOverrideService;
  private final PrivateDashboardDynamicDataService dynamicDataService;
  private final PageModelTypeResolver pageModelTypeResolver;

  public ApiResponse<PrivateDashboardResponse> getDashboard(
      Optional<String> langFromUrl, UserId userId, String userPreferredLang) {
    var tenantId = TenantId.of(tenantContext.get().tenantUuid());
    var role = tenantContext.get().currentRole();
    var type = pageModelTypeResolver.forDashboard(role);
    var pageModel = pageModelService.loadEffectiveModel(tenantId.uuid(), type.logicalId());

    var meta = pageModel.meta();
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
      // Service failure - return partial response
      dynamic = null; // or empty payload
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

    var overridesPage =
        i18nOverrideService.pageByTenantAndLocale(
            tenantId.uuid(), currentLang, PageRequest.of(0, 1000));
    var i18n =
        java.util.Map.<String, Object>of(
            "totalOverrides", overridesPage.getTotalElements(),
            "pageSize", overridesPage.getSize());

    var response = new PrivateDashboardResponse(currentLang, langs, pageModel, dynamic, i18n);

    // Return appropriate ApiResponse based on service status
    if (!services.isEmpty()) {
      return ApiResponse.partial(response, services, notices);
    } else {
      return ApiResponse.success(response);
    }
  }

  public ApiResponse<PrivateDashboardDynamicPayload> getTenantDashboardForSuperadmin(
      TenantId tenantId, Optional<String> lang, UserId userId) {
    var type = pageModelTypeResolver.forDashboard(TchRole.TENANT_ADMIN);
    PageModel pageModel = pageModelService.loadEffectiveModel(tenantId.uuid(), type.logicalId());

    String resolvedLang =
        langResolver.resolve(
            new LangResolver.LangResolverContext(
                lang, Optional.empty(), Optional.empty(), Optional.empty(), List.of(), "fr"));

    // Try to build dynamic data, catch failures
    List<ServiceStatus> services = List.of();
    List<ApiNotice> notices = List.of();
    PrivateDashboardDynamicPayload payload;
    try {
      payload =
          dynamicDataService.buildDynamicData(
              tenantId, userId, TchRole.TENANT_ADMIN, resolvedLang, pageModel);
    } catch (Exception e) {
      // Service failure - return partial response
      payload = null; // or empty payload
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

    // Return appropriate ApiResponse based on service status
    if (!services.isEmpty()) {
      return ApiResponse.partial(payload, services, notices);
    } else {
      return ApiResponse.success(payload);
    }
  }

  public ApiResponse<PrivateDashboardDynamicPayload> getCashierDashboardForSuperadmin(
      TenantId tenantId, UserId cashierId, Optional<String> lang, UserId userId) {
    var type = pageModelTypeResolver.forDashboard(TchRole.CASHIER);
    PageModel pageModel = pageModelService.loadEffectiveModel(tenantId.uuid(), type.logicalId());

    String resolvedLang =
        langResolver.resolve(
            new LangResolver.LangResolverContext(
                lang, Optional.empty(), Optional.empty(), Optional.empty(), List.of(), "fr"));

    // Try to build dynamic data, catch failures
    List<ServiceStatus> services = List.of();
    List<ApiNotice> notices = List.of();
    PrivateDashboardDynamicPayload payload;
    try {
      payload =
          dynamicDataService.buildDynamicData(
              tenantId, cashierId, TchRole.CASHIER, resolvedLang, pageModel);
    } catch (Exception e) {
      // Service failure - return partial response
      payload = null; // or empty payload
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

    // Return appropriate ApiResponse based on service status
    if (!services.isEmpty()) {
      return ApiResponse.partial(payload, services, notices);
    } else {
      return ApiResponse.success(payload);
    }
  }
}
