package com.tchalanet.server.features.private_dashboard;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.features.i18n.TenantI18nOverrideService;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.features.pagemodel.shared.PageModelService;
import com.tchalanet.server.features.pagemodel.shared.PageModelTypeResolver;
import com.tchalanet.server.features.private_dashboard.dynamic.PrivateDashboardDynamicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivateDashboardService {

    private final PageModelService pageModelService;
    private final LangResolver langResolver;
    private final TchRequestContextHolder tenantContext;
    private final TenantI18nOverrideService i18nOverrideService;
    private final PrivateDashboardDynamicDataService dynamicDataService;
    private final PageModelTypeResolver pageModelTypeResolver;

    public PrivateDashboardResponse getDashboard(Optional<String> langFromUrl, UUID userId, String userPreferredLang) {
        var tenantId = tenantContext.get().tenantUuid();
        var role = tenantContext.get().currentRole();
        var type = pageModelTypeResolver.forDashboard(role);
        var pageModel = pageModelService.loadEffectiveModel(tenantId, type.logicalId());

        var meta = pageModel.meta();
        var ctx = new LangResolver.LangResolverContext(
            langFromUrl,
            Optional.ofNullable(userPreferredLang),
            Optional.empty(),
            Optional.ofNullable(meta != null ? meta.defaultLang() : null),
            meta != null && meta.langs() != null ? meta.langs() : List.of(),
            "fr"
        );

        var currentLang = langResolver.resolve(ctx);
        List<String> langs = meta != null && meta.langs() != null ? meta.langs() : List.of(currentLang);

        var dynamic = dynamicDataService.buildDynamicData(tenantId, userId, role, currentLang, pageModel);

        var overridesPage = i18nOverrideService.pageByTenantAndLocale(tenantId, currentLang, PageRequest.of(0, 1000));
        var i18n = java.util.Map.<String, Object>of(
            "totalOverrides", overridesPage.getTotalElements(),
            "pageSize", overridesPage.getSize()
        );

        return new PrivateDashboardResponse(
            currentLang,
            langs,
            pageModel,
            dynamic,
            i18n,
            List.of()
        );
    }
}
