package com.tchalanet.server.features.pagemodel.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PageModelDynamicResolverTest {

  @Test
  void preservesPartialErrorsAndWidgetOrder() {
    var resolver = new PageModelDynamicResolver(List.of(new FixedProvider()));
    var widgets = new LinkedHashMap<String, PageModelDoc.WidgetConfig>();
    widgets.put("ok", widget("ok_source"));
    widgets.put("missing", widget("missing_source"));

    var payload = resolver.resolve(doc(widgets), "fr", null);

    assertThat(payload.widgets()).containsOnlyKeys("ok");
    assertThat(payload.widgets().keySet()).containsExactly("ok");
    assertThat(payload.errors()).hasSize(1);
    assertThat(payload.errors().get(0).widgetId()).isEqualTo("missing");
    assertThat(payload.errors().get(0).code()).isEqualTo("NO_PROVIDER");
  }

  private static PageModelDoc doc(Map<String, PageModelDoc.WidgetConfig> widgets) {
    return new PageModelDoc(
        new PageModelDoc.Meta("test.page", "public", "test", "test", 1, List.of("fr"), "fr"),
        null,
        null,
        new PageModelDoc.Content(null, widgets));
  }

  private static PageModelDoc.WidgetConfig widget(String source) {
    return new PageModelDoc.WidgetConfig(
        "TestWidget",
        new PageModelDoc.WidgetBinding("dynamic", source),
        Map.of());
  }

  private static final class FixedProvider implements PageModelDynamicProvider {
    @Override
    public boolean supports(String logicalId, String widgetType, String source) {
      return "ok_source".equals(source);
    }

    @Override
    public Object load(
        PageModelDoc pageModel,
        String widgetId,
        PageModelDoc.WidgetConfig widgetConfig,
        String lang,
        TchRequestContext ctx) {
      return Map.of("ok", true);
    }

    @Override
    public String providerKey() {
      return "ok_source";
    }
  }
}
