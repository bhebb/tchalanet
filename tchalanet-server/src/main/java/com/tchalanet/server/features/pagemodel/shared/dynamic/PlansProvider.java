package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.PlansBlock;

public interface PlansProvider {
    PlansBlock buildPlansBlock(PageModel pageModel, String currentLang);
}

