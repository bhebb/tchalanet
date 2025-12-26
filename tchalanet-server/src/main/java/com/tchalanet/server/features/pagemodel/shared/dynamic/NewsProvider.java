package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.NewsBlock;

public interface NewsProvider {
  NewsBlock buildNewsBlock(PageModel pageModel, String currentLang);
}
