package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedResultsByGameProvider implements ResultsByGameProvider {

  private final SharedResultsByGameAggregator aggregator;

  @Override
  public ResultsByGameBlock buildResultsBlock(PageModel pageModel, String currentLang) {
    return aggregator.buildResultsBlock(currentLang);
  }
}
