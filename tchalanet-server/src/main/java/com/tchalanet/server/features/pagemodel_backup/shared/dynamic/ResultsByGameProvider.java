package com.tchalanet.server.features.pagemodel_backup.shared.dynamic;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.ResultsByGameBlock;

public interface ResultsByGameProvider {

  ResultsByGameBlock buildResultsBlock(PageModel pageModel, String currentLang);
}
