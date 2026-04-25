package com.tchalanet.server.features.pagemodel.dynamic.providers;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.core.drawresult.domain.model.DrawResult;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * E.1 — Provider des résultats de tirages récents.
 * Source : "results_by_game" ou "draws"
 * Limite : 4 résultats (un par tirage majeur : Miami, NY, TX, GA)
 */
@Component
@RequiredArgsConstructor
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
    int limit = readInt(widgetConfig == null ? null : widgetConfig.props(), "max_items", 4);

    try {
      TchPage<DrawResult> page =
          queryBus.send(
              new ListDrawResultsQuery(
                  null, null,
                  LocalDate.now().minusDays(7),
                  LocalDate.now(),
                  PageRequest.of(0, limit)));

      List<Map<String, Object>> draws =
          page.items().stream()
              .map(
                  r ->
                      Map.<String, Object>of(
                          "drawnAt", r.occurredAt() != null ? r.occurredAt().toString() : null,
                          "name", r.source() != null ? r.source().name() : "",
                          "results", r.numbersMain()))
              .toList();

      return Map.of("draws", draws);
    } catch (Exception e) {
      // Graceful fallback — no draws available
      return Map.of("draws", List.of());
    }
  }

  @Override
  public String providerKey() {
    return "draws";
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

