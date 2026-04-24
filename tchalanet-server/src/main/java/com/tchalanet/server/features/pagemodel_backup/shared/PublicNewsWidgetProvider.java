package com.tchalanet.server.features.pagemodel_backup.shared;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.news.publicnews.PublicNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicNewsWidgetProvider implements PageModelDynamicProvider {

  private final PublicNewsService newsService;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "news".equals(source);
  }

  @Override
  public String providerKey() {
    return "publicnews";
  }

  @Override
  public Object load(PageModel model, PageModel.WidgetConfig config, String lang, TchRequestContext ctx) {
    int maxItems = (int) config.props().getOrDefault("max_items", 4);
    return newsService.listAll().stream().limit(maxItems).toList();
  }
}

