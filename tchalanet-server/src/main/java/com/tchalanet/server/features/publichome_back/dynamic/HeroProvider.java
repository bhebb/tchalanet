package com.tchalanet.server.features.publichome_back.dynamic;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.HeroBlock;

public interface HeroProvider {
  HeroBlock buildHeroBlock(PageModel pageModel, String currentLang);
}
