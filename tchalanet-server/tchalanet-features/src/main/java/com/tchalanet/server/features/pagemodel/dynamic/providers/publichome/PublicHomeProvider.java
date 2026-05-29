package com.tchalanet.server.features.pagemodel.dynamic.providers.publichome;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single provider for source {@code public_home} (dashboard-overview-runtime-v1).
 * Loads the bundled payload once per request via {@link PageModelResolutionContext}
 * and dispatches the relevant slice by widgetId.
 *
 * Supported widget ids (V1 révision 2 — hero/features/tchala sont désormais sur json_file) :
 *   - home.news  → ticker des dernières news (limit via props)
 *   - home.plans → grille des plans actifs
 */
@Component
@RequiredArgsConstructor
public class PublicHomeProvider implements PageModelDynamicProvider {

  static final String SOURCE = "public_home";
  private static final int DEFAULT_NEWS_LIMIT = 5;

  private final PublicHomePayloadAssembler assembler;

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

    int newsLimit = readInt(widgetConfig, "limit", DEFAULT_NEWS_LIMIT);
    // Memo key includes the limit so two widgets with different limits don't collide.
    String memoKey = SOURCE + ":news=" + newsLimit;

    PublicHomePayloadAssembler.Payload payload =
        resolutionContext.getOrLoad(memoKey, () -> assembler.assemble(newsLimit, ctx));

    return switch (widgetId == null ? "" : widgetId) {
      case "home.news" -> Map.of("items", payload.news());
      case "home.plans" -> Map.of("plans", payload.plans());
      default -> throw new PageModelDynamicProviderException(
          "PUBLIC_HOME_UNKNOWN_WIDGET",
          "Unknown widgetId for source=" + SOURCE + ": " + widgetId);
    };
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }

  private static int readInt(PageModelDoc.WidgetConfig config, String key, int defaultValue) {
    if (config == null || config.props() == null) return defaultValue;
    Object value = config.props().get(key);
    if (value instanceof Number n) return n.intValue();
    if (value instanceof String s) {
      try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
    }
    return defaultValue;
  }
}
