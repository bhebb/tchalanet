package com.tchalanet.server.features.pagemodel.dashboard;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.notification.application.query.model.GetNotificationSummaryRequest;
import com.tchalanet.server.core.notification.application.query.model.NotificationSummaryView;
import com.tchalanet.server.core.pagemodel.application.query.model.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicResolver;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service de résolution du PageModel pour les dashboards privés (tenants et platform).
 * Inclut le contexte tenant courant via TchRequestContext.
 */
@Service
@RequiredArgsConstructor
public class DashboardPageModelService {

  private final QueryBus queryBus;
  private final TchContextResolver contextResolver;
  private final LangResolver langResolver;
  private final PageModelDynamicResolver dynamicResolver;

  public DashboardPageModelResponse resolve(
      String logicalId, Optional<TenantId> tenantIdOverride, Optional<String> langFromUrl) {
    var ctxHolder = contextResolver.currentOrNull();
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
            : queryBus.ask(
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
