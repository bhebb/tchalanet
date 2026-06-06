package com.tchalanet.server.features.pagemodel.dynamic.providers.publicdrawresults;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultView;
import com.tchalanet.server.core.drawresult.api.query.view.PublicNextResultTimeView;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 *   - home.draws                  → slots only (props.includeHistory=false)
 *   - public.draw_results.main    → slots + history (props.includeHistory=true)
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
    Map<String, Object> props = widgetConfig == null ? null : widgetConfig.props();

    return switch (widgetId == null ? "" : widgetId) {
      case "home.draws" -> Map.of(
          "slots", homeSlots(payload.slots(), readInt(props, "maxSlots", Integer.MAX_VALUE)));
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

  static List<HomeSlot> homeSlots(List<PublicDrawResultSlotView> slots) {
    return homeSlots(slots, Integer.MAX_VALUE);
  }

  static List<HomeSlot> homeSlots(List<PublicDrawResultSlotView> slots, int maxSlots) {
    if (slots == null) {
      return List.of();
    }
    return slots.stream()
        .filter(Objects::nonNull)
        .map(PublicDrawResultsProvider::homeSlot)
        .limit(Math.max(0, maxSlots))
        .toList();
  }

  private static HomeSlot homeSlot(PublicDrawResultSlotView slot) {
    return new HomeSlot(
        slot.slotKey(),
        slot.provider(),
        slot.label(),
        slot.timezone(),
        slot.drawTime(),
        homeNext(slot.next()),
        homeLatest(slot.latest()));
  }

  private static HomeNext homeNext(PublicNextResultTimeView next) {
    return next == null
        ? null
        : new HomeNext(next.expectedAt(), next.localTime(), next.status(), next.countdownSeconds());
  }

  private static HomeLatest homeLatest(PublicDrawResultView latest) {
    return latest == null
        ? null
        : new HomeLatest(
            latest.resultDate(),
            latest.occurredAt(),
            latest.status(),
            latest.quality(),
            new HomeHaiti(
                text(latest, "lot1"),
                text(latest, "lot2"),
                text(latest, "lot3"),
                text(latest, "lot4")));
  }

  private static String text(PublicDrawResultView latest, String field) {
    return latest.haiti() == null ? null : latest.haiti().path(field).asString(null);
  }

  record HomeSlot(
      String slotKey,
      String provider,
      String label,
      String timezone,
      LocalTime drawTime,
      HomeNext next,
      HomeLatest latest) {}

  record HomeNext(
      Instant expectedAt,
      LocalTime localTime,
      String status,
      long countdownSeconds) {}

  record HomeLatest(
      LocalDate resultDate,
      Instant occurredAt,
      String status,
      String quality,
      HomeHaiti haiti) {}

  record HomeHaiti(
      String lot1,
      String lot2,
      String lot3,
      String lot4) {}

  static PublicDrawResultsPayloadAssembler.Spec buildSpec(
      PageModelDoc.WidgetConfig config) {
    Map<String, Object> props = config == null ? null : config.props();
    return new PublicDrawResultsPayloadAssembler.Spec(
        readStringList(props, "slotKeys"),
        readString(props, "provider"),
        readBoolean(props, "includeHistory", false),
        readInt(props, "historyLimit", DEFAULT_HISTORY_LIMIT));
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
