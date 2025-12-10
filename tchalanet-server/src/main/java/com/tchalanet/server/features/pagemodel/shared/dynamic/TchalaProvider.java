package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.TchalaBlock;

public interface TchalaProvider {
    TchalaBlock buildTchalaBlock(PageModel pageModel, String currentLang);
}

