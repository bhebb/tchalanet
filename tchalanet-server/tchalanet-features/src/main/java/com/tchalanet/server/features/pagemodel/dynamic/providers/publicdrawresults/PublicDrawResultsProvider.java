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

import com.tchalanet.server.features.publicdrawresults.DrawChannelLabelKeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single provider for source {@code public_draw_results} (dashboard-overview-runtime-v1).
 *
 * Loads the slots with their latest results once per request via {@link PageModelResolutionContext}.
 * Both widgets use the same structure {@link HomeSlot} with i18n label keys.
 *
 * Supported widget ids:
 *   - home.draws                  → slots only (same structure)
 *   - public.draw_results.main    → slots only (same structure)
 */
@Component
@RequiredArgsConstructor
public class PublicDrawResultsProvider implements PageModelDynamicProvider {

  static final String SOURCE = "public_draw_results";

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

    // Les deux widgets retournent la même structure
    return switch (widgetId == null ? "" : widgetId) {
      case "home.draws", "public.draw_results.main" ->
          Map.of("slots", homeSlots(payload.slots()));
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
    if (slots == null) {
      return List.of();
    }
    return slots.stream()
        .filter(Objects::nonNull)
        .map(PublicDrawResultsProvider::homeSlot)
        .toList();
  }

  private static HomeSlot homeSlot(PublicDrawResultSlotView slot) {
    return new HomeSlot(
        slot.slotKey(),
        slot.provider(),
        DrawChannelLabelKeyResolver.resolve(slot.slotKey()),
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
            latest.drawResultId(),
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
      /** Clé i18n stable (ex: "draw_channel.ny.eve.label"). */
      String drawChannelLabelKey,
      /** Label public affiché (fallback si i18n non disponible). */
      String drawChannelLabel,
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
      /** UUID opaque pour naviguer vers le détail. */
      String drawResultId,
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
        false,  // includeHistory - désactivé car non utilisé par les widgets
        0);     // historyLimit - 0 car non utilisé
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
}
