package com.tchalanet.server.features.publichome.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.HeroBlock;

public interface HeroProvider {
  HeroBlock buildHeroBlock(PageModel pageModel, String currentLang);
}
