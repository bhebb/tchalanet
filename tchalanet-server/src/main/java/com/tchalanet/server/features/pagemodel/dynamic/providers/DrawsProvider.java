package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.features.publicdraw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicLatestDrawResultsResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provider des derniers résultats de tirages pour affichage public ou dashboard.
 * Utilise la query BFF officielle {@link GetLatestPublicDrawResultsQuery}.
 *
 * Prop 'limit_per_slot' (int, def 1) : nombre de résultats à retourner pour chaque canal (NY, FL, etc).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawsProvider implements PageModelDynamicProvider {

  private final QueryBus queryBus;

  @Override
  public boolean supports(String logicalId, String widgetType, String source) {
    return "results_by_game".equals(source) || "draws".equals(source);
  }

  @Override
  public Object load(
      PageModelDoc pageModel,
      String widgetId,
      PageModelDoc.WidgetConfig widgetConfig,
      String lang,
      TchRequestContext ctx) {

    int limitPerSlot = readLimitPerSlot(widgetConfig == null ? null : widgetConfig.props());

    try {
      List<PublicLatestDrawResultsResponse> results =
          queryBus.send(new GetLatestPublicDrawResultsQuery(limitPerSlot));

      return Map.of("draws", results);
    } catch (Exception e) {
      log.error("DrawsProvider: failed to load latest draws for widget {}", widgetId, e);
      return Map.of("draws", List.of());
    }
  }

  @Override
  public String providerKey() {
    return "draws";
  }

  private int readLimitPerSlot(Map<String, Object> props) {
    int v = readInt(props, "limit_per_slot", -1);
    if (v == -1) {
      // Backward compatibility logic
      v = readInt(props, "max_items", 1);
      if (props != null && props.containsKey("max_items")) {
        log.info("DrawsProvider: prop 'max_items' is deprecated, use 'limit_per_slot'");
      }
    }
    return v;
  }

  private static int readInt(Map<String, Object> props, String key, int def) {
    if (props == null) return def;
    Object v = props.get(key);
    if (v instanceof Integer i) return i;
    if (v instanceof Number n) return n.intValue();
    if (v instanceof String s) {
      try {
        return Integer.parseInt(s);
      } catch (Exception ignore) {
      }
    }
    return def;
  }
}
