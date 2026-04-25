package com.tchalanet.server.core.pagemodel.application.query.handler;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_TENANT_UUID;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContextRunner;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelTemplateLoaderPort;
import com.tchalanet.server.core.pagemodel.application.query.model.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// [Phase 3A] @UseCase + QueryHandler pour câblage CQRS (analysis §MAJEUR command_query_handlers.md §4.2)
// [Phase 3A] new ObjectMapper() → JsonUtils injecté (analysis §MAJEUR)
@UseCase
@RequiredArgsConstructor
public class ResolveEffectivePageModelHandler
    implements QueryHandler<ResolveEffectivePageModelQuery, PageModelDoc> {

  private final PageModelReadPort readPort;
  private final PageModelTemplateLoaderPort templateLoader;
  private final JsonUtils jsonUtils;

  @Override
  public PageModelDoc handle(ResolveEffectivePageModelQuery q) {
    // 1) tenant courant (si fourni)
    Optional<PageModelDoc> tenantDoc =
        q.tenantId()
            .map(TenantId::value)
            .flatMap(
                tenantUuid ->
                    TchContextRunner.runAsTenantResult(
                        tenantUuid,
                        "pagemodel:resolve",
                        () -> readPort.findPublishedByLogicalId(q.logicalId()).map(p -> toDoc(p)))) ;

    if (tenantDoc.isPresent()) return tenantDoc.get();

    // 2) default tenant
    Optional<PageModelDoc> defaultDoc =
        TchContextRunner.runAsTenantResult(
            DEFAULT_TENANT_UUID,
            "pagemodel:resolve-default",
            () -> readPort.findPublishedByLogicalId(q.logicalId()).map(p -> toDoc(p)));

    if (defaultDoc.isPresent()) return defaultDoc.get();

    // 3) resources
    return templateLoader.loadFromResources(q.logicalId());
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
