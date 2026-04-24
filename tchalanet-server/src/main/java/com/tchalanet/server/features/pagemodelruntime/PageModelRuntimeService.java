package com.tchalanet.server.features.pagemodelruntime;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.application.query.model.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.PageModelDynamicResolver;
import com.tchalanet.server.features.pagemodel.LangResolver;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageModelRuntimeService {

  private final QueryBus queryBus;
  private final TchContextResolver contextResolver;
  private final LangResolver langResolver;
  private final PageModelDynamicResolver dynamicResolver;

  public PageModelRuntimeResponse resolvePublic(String logicalId, Optional<String> langFromUrl) {
    var ctxHolder = contextResolver.currentOrNull();
    Optional<TenantId> tenantId = Optional.ofNullable(ctxHolder == null ? null : ctxHolder.tenantId());

    PageModelDoc doc = queryBus.send(new ResolveEffectivePageModelQuery(tenantId, logicalId));

    String currentLang = langResolver.resolve(new LangResolver.LangResolverContext(
        langFromUrl, Optional.empty(), Optional.empty(),
        Optional.ofNullable(doc.meta() != null ? doc.meta().defaultLang() : null),
        doc.meta() != null && doc.meta().langs() != null ? doc.meta().langs() : List.of(),
        "fr"
    ));

    var dynamic = dynamicResolver.resolve(doc, currentLang, ctxHolder);
    List<String> langs = doc.meta() != null && doc.meta().langs() != null ? doc.meta().langs() : List.of();
    return new PageModelRuntimeResponse(currentLang, langs, doc, dynamic);
  }
}
