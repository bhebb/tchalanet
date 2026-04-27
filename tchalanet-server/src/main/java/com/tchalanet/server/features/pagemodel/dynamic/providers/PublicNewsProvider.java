package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.news.publicnews.PublicNewsService;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicNewsProvider implements PageModelDynamicProvider {

  private final PublicNewsService newsService;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "news".equals(source);
  }

  @Override
  public Object load(PageModelDoc pageModel, String widgetId, PageModelDoc.WidgetConfig widgetConfig, String lang, TchRequestContext ctx) {
    int max = readInt(widgetConfig == null ? null : widgetConfig.props(), "max_items", 4);
    return newsService.listAll().stream().limit(max).toList();
  }

  @Override
  public String providerKey() {
    return "public-news";
  }

  private static int readInt(Map<String, Object> props, String key, int def) {
    if (props == null) return def;
    Object v = props.get(key);
    if (v instanceof Integer i) return i;
    if (v instanceof Number n) return n.intValue();
    if (v instanceof String s) {
      try { return Integer.parseInt(s); } catch (Exception ignore) {}
    }
    return def;
  }
}
