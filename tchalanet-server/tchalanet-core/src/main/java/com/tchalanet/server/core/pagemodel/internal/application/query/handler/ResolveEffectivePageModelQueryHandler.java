package com.tchalanet.server.core.pagemodel.internal.application.query.handler;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_TENANT_UUID;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelTemplateLoaderPort;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// [Phase 3A] @UseCase + QueryHandler pour câblage CQRS (analysis §MAJEUR command_query_handlers.md §4.2)
// [Phase 3A] new ObjectMapper() → JsonUtils injecté (analysis §MAJEUR)
@UseCase
@RequiredArgsConstructor
public class ResolveEffectivePageModelQueryHandler
    implements QueryHandler<ResolveEffectivePageModelQuery, PageModelDoc> {

  private final PageModelReadPort readPort;
  private final PageModelTemplateLoaderPort templateLoader;
  private final JsonUtils jsonUtils;

  @Override
  public PageModelDoc handle(ResolveEffectivePageModelQuery q) {
    // 1) tenant courant: use the already-bound request/startup context.
    Optional<PageModelDoc> tenantDoc =
        q.tenantId()
            .flatMap(tenantId -> readPort.findPublishedByLogicalId(q.logicalId()).map(this::toDoc));

    if (tenantDoc.isPresent()) return tenantDoc.get();

    // 2) default tenant fallback: this intentionally reads a different tenant under RLS.
    if (q.tenantId().map(TenantId::value).filter(DEFAULT_TENANT_UUID::equals).isPresent()) {
      return templateLoader.loadFromResources(q.logicalId());
    }

    Optional<PageModelDoc> defaultDoc =
        TchContextScope.runWithTemporaryTenantResult(
            DEFAULT_TENANT_UUID,
            "pagemodel:resolve-default",
            () -> readPort.findPublishedByLogicalId(q.logicalId()).map(p -> toDoc(p)));

      return defaultDoc.orElseGet(() -> templateLoader.loadFromResources(q.logicalId()));
  }

  private PageModelDoc toDoc(PageModelInstance inst) {
    if (inst == null) return null;
    try {
      if (inst.modelJson() == null) return new PageModelDoc(null, null, null, null);
      return jsonUtils.treeToValue(inst.modelJson(), PageModelDoc.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert PageModelInstance.modelJson to PageModelDoc", e);
    }
  }
}
