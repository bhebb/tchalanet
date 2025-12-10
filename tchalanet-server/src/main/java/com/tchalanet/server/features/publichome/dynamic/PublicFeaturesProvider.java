package com.tchalanet.server.features.publichome.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.FeaturesBlock;

public interface PublicFeaturesProvider {
    FeaturesBlock buildFeaturesBlock(PageModel pageModel, String currentLang);
}

