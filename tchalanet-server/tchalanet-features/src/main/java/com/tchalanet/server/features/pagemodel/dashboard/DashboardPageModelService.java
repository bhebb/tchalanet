package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.exception.TchForbiddenException;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicResolver;
import com.tchalanet.server.features.pagemodel.security.PageModelAccessPolicy;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    // [harden-pagemodel-security-v2 / D2] Check role-based access before loading providers.
    // No silent fallback — unauthorized access returns 403.
    var currentRole = ctxHolder != null ? ctxHolder.currentRole() : null;
    if (!accessPolicy.permits(logicalId, currentRole)) {
      throw new TchForbiddenException(
          "PAGE_MODEL_ACCESS_DENIED",
          "Role " + currentRole + " is not authorized to access PageModel: " + logicalId);
    }

    Optional<TenantId> tenantId =
        tenantIdOverride.isPresent()
            ? tenantIdOverride
            : Optional.ofNullable(ctxHolder == null ? null : ctxHolder.tenantId());

    PageModelDoc doc = queryBus.ask(new ResolveEffectivePageModelQuery(tenantId, logicalId));

    String currentLang =
        langResolver.resolve(
            new LangResolver.LangResolverContext(
                langFromUrl,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(doc != null && doc.meta() != null ? doc.meta().defaultLang() : null),
                doc != null && doc.meta() != null && doc.meta().langs() != null
                    ? doc.meta().langs()
                    : List.of(),
                "fr"));

    var dynamic = dynamicResolver.resolve(doc, currentLang, ctxHolder);
    NotificationSummaryView notifications =
        ctxHolder == null
            ? null
            : notificationApi.getNotificationSummary(
                new GetNotificationSummaryRequest(
                    ctxHolder.userId(),
                    ctxHolder.currentRole() == null ? null : ctxHolder.currentRole().name()));
    List<String> langs =
        doc != null && doc.meta() != null && doc.meta().langs() != null
            ? doc.meta().langs()
            : List.of();
    return new DashboardPageModelResponse(currentLang, langs, doc, dynamic, notifications);
  }
}
