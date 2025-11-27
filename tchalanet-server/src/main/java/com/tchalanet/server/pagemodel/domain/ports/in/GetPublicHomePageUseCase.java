package com.tchalanet.server.pagemodel.domain.ports.in;

import com.tchalanet.server.pagemodel.application.dto.PublicHomeDynamicData;
import com.tchalanet.server.pagemodel.domain.model.PageModel;

public interface GetPublicHomePageUseCase {

  /** Returns the PageModel for the public home page, enriched with dynamic data. */
  EnrichedPageModel getPublicHome(String lang);

  /** Record combining the static PageModel with dynamic data. */
  record EnrichedPageModel(PageModel pageModel, PublicHomeDynamicData dynamicData) {}
}
