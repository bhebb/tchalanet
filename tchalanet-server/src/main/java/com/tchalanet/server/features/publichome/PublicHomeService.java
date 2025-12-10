package com.tchalanet.server.features.publichome;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.features.i18n.TenantI18nOverrideService;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.PageModelService;
import com.tchalanet.server.features.pagemodel.shared.PageModelTypeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PublicHomeService {

    public static final String GLOBAL_FALLBACK_LANG = "fr";
    private final PageModelService pageModelService;
    private final LangResolver langResolver;
    private final TchRequestContextHolder tenantContext;
    private final TenantI18nOverrideService i18nOverrideService;
    private final PublicHomeDynamicDataService dynamicDataService;
    private final PageModelTypeResolver pageModelTypeResolver;

    public PublicHomeResponse getPublicHome(Optional<String> langFromUrl) {
        var tenantId = tenantContext.get().tenantUuid();
        var type = pageModelTypeResolver.forPublicHome();
        PageModel pageModel = pageModelService.loadEffectiveModel(tenantId, type.logicalId());
        var meta = pageModel.meta();

        var ctx = new LangResolver.LangResolverContext(
            langFromUrl,
            Optional.empty(),
            Optional.empty(),
            Optional.ofNullable(meta != null ? meta.defaultLang() : null),
            meta != null && meta.langs() != null ? meta.langs() : List.of(),
            GLOBAL_FALLBACK_LANG
        );

        var currentLang = langResolver.resolve(ctx);
        List<String> langs = meta != null && meta.langs() != null ? meta.langs() : List.of(currentLang);

        var dynamic = dynamicDataService.buildDynamicData(pageModel, currentLang);

        var overridesPage = i18nOverrideService.pageByTenantAndLocale(tenantId, currentLang, PageRequest.of(0, 1000));
        Map<String, Object> i18n = Map.of(
            "totalOverrides", overridesPage.getTotalElements(),
            "pageSize", overridesPage.getSize()
        );

        return new PublicHomeResponse(
            currentLang,
            langs,
            pageModel,
            dynamic,
            i18n,
            List.of()
        );
    }
}
