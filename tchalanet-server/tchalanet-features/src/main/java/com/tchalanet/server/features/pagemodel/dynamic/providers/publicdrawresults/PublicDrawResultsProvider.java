package com.tchalanet.server.features.pagemodel.dynamic.providers.publicdrawresults;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single provider for source {@code public_draw_results} (dashboard-overview-runtime-v1).
 *
 * Loads the bundled payload once per request via {@link PageModelResolutionContext}
 * and dispatches the relevant slice by widgetId. Reads props to decide whether to
 * include the bounded history (cheap path for the home widget vs full path for the
 * dedicated results page).
 *
 * Supported widget ids:
 *   - home.draws                  → slots only (props.include_history=false)
 *   - public.draw_results.main    → slots + history (props.include_history=true)
 */
@Component
@RequiredArgsConstructor
public class PublicDrawResultsProvider implements PageModelDynamicProvider {

  static final String SOURCE = "public_draw_results";
  private static final int DEFAULT_HISTORY_LIMIT = 10;

  private final PublicDrawResultsPayloadAssembler assembler;

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
      TchRequestContext ctx,
      PageModelResolutionContext resolutionContext) {

    PublicDrawResultsPayloadAssembler.Spec spec = buildSpec(widgetConfig);
    PublicDrawResultsPayloadAssembler.Payload payload =
        resolutionContext.getOrLoad(spec.memoKey(), () -> assembler.assemble(spec));

    return switch (widgetId == null ? "" : widgetId) {
      case "home.draws" -> Map.of(
          "slots", payload.slots());
      case "public.draw_results.main" -> Map.of(
          "slots", payload.slots(),
          "history", payload.history(),
          "historyLimit", spec.historyLimit(),
          "includeHistory", spec.includeHistory());
      default -> throw new PageModelDynamicProviderException(
          "PUBLIC_DRAW_RESULTS_UNKNOWN_WIDGET",
          "Unknown widgetId for source=" + SOURCE + ": " + widgetId);
    };
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }

  private static PublicDrawResultsPayloadAssembler.Spec buildSpec(
      PageModelDoc.WidgetConfig config) {
    Map<String, Object> props = config == null ? null : config.props();
    return new PublicDrawResultsPayloadAssembler.Spec(
        readStringList(props, "slot_keys"),
        readString(props, "provider"),
        readBoolean(props, "include_history", false),
        readInt(props, "history_limit", DEFAULT_HISTORY_LIMIT));
  }

  private static String readString(Map<String, Object> props, String key) {
    if (props == null) return null;
    Object v = props.get(key);
    return v instanceof String s && !s.isBlank() ? s.trim() : null;
  }

  private static List<String> readStringList(Map<String, Object> props, String key) {
    if (props == null || !props.containsKey(key)) return List.of();
    Object v = props.get(key);
    if (v instanceof List<?> list) {
      return list.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .toList();
    }
    if (v instanceof String s) {
      return Arrays.stream(s.split(","))
          .map(String::trim)
          .filter(t -> !t.isBlank())
          .toList();
    }
    return List.of();
  }

  private static boolean readBoolean(Map<String, Object> props, String key, boolean defaultValue) {
    if (props == null) return defaultValue;
    Object v = props.get(key);
    if (v instanceof Boolean b) return b;
    if (v instanceof String s) return Boolean.parseBoolean(s);
    return defaultValue;
  }

  private static int readInt(Map<String, Object> props, String key, int defaultValue) {
    if (props == null) return defaultValue;
    Object v = props.get(key);
    if (v instanceof Number n) return n.intValue();
    if (v instanceof String s) {
      try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
    }
    return defaultValue;
  }
}
