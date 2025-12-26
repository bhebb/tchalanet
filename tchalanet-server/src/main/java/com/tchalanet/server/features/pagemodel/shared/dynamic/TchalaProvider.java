package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.features.pagemodel.shared.PageModel;

public interface TchalaProvider {
  com.tchalanet.server.features.pagemodel.shared.block.TchalaBlock buildTchalaBlock(
      PageModel pageModel, String currentLang);
}
