package com.tchalanet.server.features.pagemodel_backup.shared.dynamic;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.PlansBlock;

public interface PlansProvider {
  PlansBlock buildPlansBlock(PageModel pageModel, String currentLang);
}
