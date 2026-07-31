package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.api.model.PageModelType;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicResolver;
import com.tchalanet.server.features.pagemodel.error.PageModelErrorCodes;
import com.tchalanet.server.features.pagemodel.runtime.PageRuntimeAssembler;
import com.tchalanet.server.features.pagemodel.runtime.PageRuntimeResponse;
import com.tchalanet.server.features.pagemodel.security.PageModelAccessPolicy;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.platform.tenant.api.TenantConfigApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Private dashboard PageModel resolver.
 *
 * <p>Private rule: - Requires authenticated context. - Checks access before dynamic providers. -
 * Blocks unsafe tenant override. - Checks access again after document resolution.
 */
@Service
@RequiredArgsConstructor
public class DashboardPageModelService {

  private final QueryBus queryBus;
  private final TchContextResolver contextResolver;
  private final LangResolver langResolver;
  private final PageModelDynamicResolver dynamicResolver;
  private final PageModelAccessPolicy accessPolicy;
  private final PageRuntimeAssembler runtimeAssembler;
  private final TenantConfigApi tenantConfigApi;
  private final TenantPreContextLookupApi tenantLookupApi;

  public PageRuntimeResponse resolve(
      String logicalId, Optional<TenantId> tenantIdOverride, Optional<String> langFromUrl) {
    return resolve(logicalId, tenantIdOverride, langFromUrl, Optional.empty(), 0);
  }

  public PageRuntimeResponse resolve(
      String logicalId,
      Optional<TenantId> tenantIdOverride,
      Optional<String> langFromUrl,
      Optional<String> period,
      int performancePage) {
    var ctxHolder = contextResolver.currentOrNull();

    if (ctxHolder == null) {
      throw ProblemRest.of(PageModelErrorCodes.ACCESS_DENIED);
    }

    var currentRole = ctxHolder.currentRole();

    if (!accessPolicy.permits(logicalId, currentRole)) {
      throw ProblemRest.of(PageModelErrorCodes.ACCESS_DENIED);
    }

    if (tenantIdOverride.isPresent() && !accessPolicy.canOverrideTenant(currentRole)) {
      throw ProblemRest.of(PageModelErrorCodes.TENANT_OVERRIDE_FORBIDDEN);
    }

    Optional<TenantId> tenantId =
        tenantIdOverride.isPresent() ? tenantIdOverride : Optional.ofNullable(ctxHolder.tenantId());

    PageModelDoc doc = queryBus.ask(new ResolveEffectivePageModelQuery(tenantId, logicalId));

    if (doc == null) {
      throw ProblemRest.of(PageModelErrorCodes.NOT_FOUND);
    }

    accessPolicy.assertCanAccess(logicalId, doc, ctxHolder);

    var currentLang = resolveLang(doc, langFromUrl);
    var dynamic =
        dynamicResolver.resolve(
            doc,
            currentLang,
            ctxHolder,
            Map.of(
                "dashboard.period", period.orElse("TODAY"),
                "dashboard.performancePage", Math.max(0, performancePage)));

    return withTenantSettings(logicalId, ctxHolder, runtimeAssembler.assemble(doc, dynamic));
  }

  private String resolveLang(PageModelDoc doc, Optional<String> langFromUrl) {
    boolean hasDocLangs =
        doc.meta() != null && doc.meta().langs() != null && !doc.meta().langs().isEmpty();

    return langResolver.resolve(
        new LangResolver.LangResolverContext(
            langFromUrl,
            Optional.empty(),
            Optional.empty(),
            Optional.ofNullable(doc.meta() != null ? doc.meta().defaultLang() : null),
            hasDocLangs ? doc.meta().langs() : List.of(),
            "fr"));
  }

  private PageRuntimeResponse withTenantSettings(
      String logicalId,
      com.tchalanet.server.common.context.TchRequestContext ctx,
      PageRuntimeResponse response) {
    if (!PageModelType.DASHBOARD_TENANT_ADMIN.logicalId().equals(logicalId)
        || ctx == null
        || ctx.effectiveTenantIdOrNull() == null
        || response == null
        || response.dynamic() == null) {
      return response;
    }

    try {
      var tenantId = ctx.tenantIdRequired();
      TenantContextLookupView tenant = tenantLookupApi.findById(tenantId).orElse(null);
      TenantInternalSettings settings =
          tenantConfigApi.getTenantInternalSettings(new GetTenantByIdRequest(tenantId));
      Map<String, Object> widgets = new LinkedHashMap<>(response.dynamic().widgets());
      widgets.put("tenant.settings", TenantDashboardSettingsPayload.from(tenant, settings));
      return new PageRuntimeResponse(
          response.meta(),
          response.theme(),
          response.shell(),
          response.content(),
          new PageRuntimeResponse.DynamicPayload(widgets, response.dynamic().errors()));
    } catch (RuntimeException e) {
      return response;
    }
  }

  private record TenantDashboardSettingsPayload(
      String tenantCode,
      String displayName,
      String timezone,
      String currency,
      String defaultLanguage,
      String defaultLocale,
      List<String> supportedLanguages,
      List<String> supportedCurrencies,
      String fallbackLanguage,
      TenantInternalSettings settings) {

    static TenantDashboardSettingsPayload from(
        TenantContextLookupView tenant, TenantInternalSettings settings) {
      var locale = settings != null ? settings.locale() : null;
      return new TenantDashboardSettingsPayload(
          tenant != null ? tenant.code() : null,
          tenant != null ? tenant.displayName() : null,
          tenant != null && tenant.timezone() != null ? tenant.timezone().getId() : null,
          tenant != null && tenant.currency() != null ? tenant.currency().getCurrencyCode() : null,
          tenant != null ? tenant.defaultLanguage() : null,
          tenant != null ? tenant.defaultLocale() : null,
          locale != null ? locale.effectiveSupportedLanguages() : List.of(),
          tenant != null && tenant.currency() != null
              ? List.of(tenant.currency().getCurrencyCode())
              : List.of(),
          locale != null ? locale.fallbackLanguage() : null,
          settings);
    }
  }
}
