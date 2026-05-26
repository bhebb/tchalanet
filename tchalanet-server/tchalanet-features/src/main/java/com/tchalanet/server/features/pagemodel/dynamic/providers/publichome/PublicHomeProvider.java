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
 * Supported widget ids:
 *   - home.hero
 *   - home.features
 *   - home.plans
 */
@Component
@RequiredArgsConstructor
public class PublicHomeProvider implements PageModelDynamicProvider {

  static final String SOURCE = "public_home";
  private static final String MEMO_KEY = SOURCE;

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

    PublicHomePayloadAssembler.Payload payload =
        resolutionContext.getOrLoad(MEMO_KEY, () -> assembler.assemble(lang, ctx));

    return switch (widgetId == null ? "" : widgetId) {
      case "home.hero" -> payload.hero();
      case "home.features" -> Map.of("items", payload.features());
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
}
