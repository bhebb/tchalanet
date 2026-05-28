package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.exception.TchForbiddenException;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicResolver;
import com.tchalanet.server.features.pagemodel.security.PageModelAccessPolicy;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Private dashboard PageModel resolver.
 * <p>
 * Private rule:
 * - Requires authenticated context.
 * - Checks access before dynamic providers.
 * - Blocks unsafe tenant override.
 * - Checks access again after document resolution.
 */
@Service
@RequiredArgsConstructor
public class DashboardPageModelService {

    private final QueryBus queryBus;
    private final TchContextResolver contextResolver;
    private final LangResolver langResolver;
    private final PageModelDynamicResolver dynamicResolver;
    private final NotificationApi notificationApi;
    private final PageModelAccessPolicy accessPolicy;

    public DashboardPageModelResponse resolve(
        String logicalId, Optional<TenantId> tenantIdOverride, Optional<String> langFromUrl) {
        var ctxHolder = contextResolver.currentOrNull();

        if (ctxHolder == null) {
            throw new TchForbiddenException(
                "PAGE_MODEL_ACCESS_DENIED",
                "Authentication required");
        }

        var currentRole = ctxHolder.currentRole();

        if (!accessPolicy.permits(logicalId, currentRole)) {
            throw new TchForbiddenException(
                "PAGE_MODEL_ACCESS_DENIED",
                "Role " + currentRole + " is not authorized to access PageModel: " + logicalId);
        }

        if (tenantIdOverride.isPresent() && !accessPolicy.canOverrideTenant(currentRole)) {
            throw new TchForbiddenException(
                "TENANT_OVERRIDE_DENIED",
                "Current role cannot override tenant context");
        }

        Optional<TenantId> tenantId =
            tenantIdOverride.isPresent()
                ? tenantIdOverride
                : Optional.ofNullable(ctxHolder.tenantId());

        PageModelDoc doc = queryBus.ask(new ResolveEffectivePageModelQuery(tenantId, logicalId));

        if (doc == null) {
            throw new TchNotFoundException(
                "PAGE_MODEL_NOT_FOUND",
                "Page model not found: " + logicalId);
        }

        accessPolicy.assertCanAccess(logicalId, doc, ctxHolder);

        var currentLang = resolveLang(doc, langFromUrl);
        var dynamic = dynamicResolver.resolve(doc, currentLang, ctxHolder);

        NotificationSummaryView notifications =
            notificationApi.getNotificationSummary(
                new GetNotificationSummaryRequest(
                    ctxHolder.userId(),
                    currentRole == null ? null : currentRole.name()));

        var langs = langsOrCurrent(doc, currentLang);
        return new DashboardPageModelResponse(currentLang, langs, doc, dynamic, notifications);
    }

    private String resolveLang(PageModelDoc doc, Optional<String> langFromUrl) {
        boolean hasDocLangs = doc.meta() != null
            && doc.meta().langs() != null
            && !doc.meta().langs().isEmpty();

        return langResolver.resolve(
            new LangResolver.LangResolverContext(
                langFromUrl,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(doc.meta() != null ? doc.meta().defaultLang() : null),
                hasDocLangs ? doc.meta().langs() : List.of(),
                "fr"));
    }

    private List<String> langsOrCurrent(PageModelDoc doc, String currentLang) {
        boolean hasDocLangs = doc.meta() != null
            && doc.meta().langs() != null
            && !doc.meta().langs().isEmpty();

        return hasDocLangs ? doc.meta().langs() : List.of(currentLang);
    }
}
