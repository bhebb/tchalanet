package com.tchalanet.server.features.pagemodel;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.TchRole;
import java.util.List;
import java.util.Map;

import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageModelOrchestrator {

    private final PageModelService pageModelService;
    private final PageModelDynamicResolver dynamicResolver;

    public PageModelResponse resolvePublic(String logicalId) {
        assertPrefix(logicalId, "public.");

        PageModel model = pageModelService.loadEffectiveModel(null, logicalId);

        String lang = defaultLang(model);
        List<String> langs = safeLangs(model, lang);

        var dynamic = dynamicResolver.resolve(model, lang, null);

        return new PageModelResponse(lang, langs, model, dynamic, Map.of());
    }

    public PageModelResponse resolveTenant(String logicalId, TchRequestContext ctx) {
        assertPrefix(logicalId, "private.");

        TenantId tenantId = ctx.tenantIdSafe();
        PageModel model = pageModelService.loadEffectiveModel(tenantId, logicalId);

        String lang = defaultLang(model);
        List<String> langs = safeLangs(model, lang);

        var dynamic = dynamicResolver.resolve(model, lang, ctx);

        return new PageModelResponse(lang, langs, model, dynamic, Map.of());
    }

    public PageModelResponse resolvePlatform(String logicalId, TchRequestContext ctx) {
        if (ctx == null || ctx.currentRole() != TchRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Platform access denied");
        }
        assertPrefix(logicalId, "private.");

        PageModel model = pageModelService.loadEffectiveModel(null, logicalId);

        String lang = defaultLang(model);
        List<String> langs = safeLangs(model, lang);

        var dynamic = dynamicResolver.resolve(model, lang, ctx);

        return new PageModelResponse(lang, langs, model, dynamic, Map.of());
    }

    private static void assertPrefix(String logicalId, String prefix) {
        if (logicalId == null || !logicalId.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid logicalId, expected prefix " + prefix + ": " + logicalId);
        }
    }

    private static String defaultLang(PageModel model) {
        if (model == null || model.meta() == null) return "fr";
        String dl = model.meta().defaultLang();
        return (dl == null || dl.isBlank()) ? "fr" : dl;
    }

    private static List<String> safeLangs(PageModel model, String fallback) {
        if (model == null || model.meta() == null || model.meta().langs() == null || model.meta().langs().isEmpty()) {
            return List.of(fallback);
        }
        return model.meta().langs();
    }
}
