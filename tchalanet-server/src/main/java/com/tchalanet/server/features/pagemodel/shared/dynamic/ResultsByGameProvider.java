package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;

public interface ResultsByGameProvider {

  ResultsByGameBlock buildResultsBlock(PageModel pageModel, String currentLang);
}
