package com.tchalanet.server.features.publichome_back.dynamic;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.FeaturesBlock;

public interface PublicFeaturesProvider {
  FeaturesBlock buildFeaturesBlock(PageModel pageModel, String currentLang);
}
