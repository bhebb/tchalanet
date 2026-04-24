package com.tchalanet.server.features.pagemodel_backup.shared.dynamic;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;
import com.tchalanet.server.features.pagemodel_backup.shared.block.NewsBlock;

public interface NewsProvider {
  NewsBlock buildNewsBlock(PageModel pageModel, String currentLang);
}
