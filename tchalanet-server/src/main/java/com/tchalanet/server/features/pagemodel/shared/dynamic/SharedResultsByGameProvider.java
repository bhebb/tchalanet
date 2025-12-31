package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedResultsByGameProvider implements ResultsByGameProvider {

  private final SharedResultsByGameAggregator aggregator;
  private final TchContextResolver tchContextResolver;

  @Override
  public ResultsByGameBlock buildResultsBlock(PageModel pageModel, String currentLang) {
    // currentLang utile plus tard si tu veux filtrer/ordonner,
    // mais pour l’instant on s’en sert pas.
    var holder = tchContextResolver.currentOrNull();
    var tenantId = holder != null ? holder.tenantid() : null;
    return aggregator.buildResultsBlock(tenantId);
  }
}
