package com.tchalanet.server.features.pagemodel_backup.shared.dynamic;

import com.tchalanet.server.features.pagemodel_backup.shared.PageModel;

public interface TchalaProvider {
  com.tchalanet.server.features.pagemodel_backup.shared.block.TchalaBlock buildTchalaBlock(
      PageModel pageModel, String currentLang);
}
