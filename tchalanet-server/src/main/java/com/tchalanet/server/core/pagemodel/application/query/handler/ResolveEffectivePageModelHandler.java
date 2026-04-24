package com.tchalanet.server.core.pagemodel.application.query.handler;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_TENANT_UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.context.TchContextRunner;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.application.port.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelTemplateLoaderPort;
import com.tchalanet.server.core.pagemodel.application.query.model.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResolveEffectivePageModelHandler {

  private final PageModelReadPort readPort;
  private final PageModelTemplateLoaderPort templateLoader;
  private final ObjectMapper mapper = new ObjectMapper();

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
    // Convert modelJson (JsonNode) into PageModelDoc if possible
    try {
      if (inst.modelJson() == null) return new PageModelDoc(null, null, null, null);
      return mapper.treeToValue(inst.modelJson(), PageModelDoc.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert PageModelInstance.modelJson to PageModelDoc", e);
    }
  }
}
