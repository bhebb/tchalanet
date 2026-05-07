package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.drawresult.application.query.model.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicDrawResultsProvider implements PageModelDynamicProvider {

  private static final String SOURCE = "public_draw_results";

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return SOURCE.equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {
    var props = widgetConfig == null ? null : widgetConfig.props();
    var slotKeys = readStringList(props, "slot_keys");
    var provider = readString(props, "provider");

    var slots =
        queryBus.send(new ListPublicDrawResultSlotsQuery(slotKeys, provider));

    return Map.of("slots", slots);
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }

  private static String readString(Map<String, Object> props, String key) {
    if (props == null) {
      return null;
    }

    Object value = props.get(key);
    return value instanceof String s && !s.isBlank() ? s.trim() : null;
  }

  private static List<String> readStringList(Map<String, Object> props, String key) {
    if (props == null || !props.containsKey(key)) {
      return List.of();
    }

    Object value = props.get(key);
    if (value instanceof List<?> list) {
      return list.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .map(String::trim)
          .filter(v -> !v.isBlank())
          .toList();
    }

    if (value instanceof String s) {
      return java.util.Arrays.stream(s.split(","))
          .map(String::trim)
          .filter(v -> !v.isBlank())
          .toList();
    }

    return List.of();
  }
}
