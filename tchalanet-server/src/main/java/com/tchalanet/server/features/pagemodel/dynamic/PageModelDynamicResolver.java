package com.tchalanet.server.features.pagemodel.dynamic;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.shared.PageDynamicPayload;
import com.tchalanet.server.features.pagemodel.shared.WidgetDynamicError;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageModelDynamicResolver {

  private final List<PageModelDynamicProvider> providers;

  public PageDynamicPayload resolve(PageModelDoc doc, String lang, TchRequestContext ctx) {
    Map<String, Object> widgets = new HashMap<>();
    List<WidgetDynamicError> errors = new ArrayList<>();

    if (doc == null) return new PageDynamicPayload(widgets, errors);
    if (doc.content() == null || doc.content().widgets() == null) {
      return new PageDynamicPayload(widgets, errors);
    }

    doc.content().widgets().forEach((widgetId, config) -> {
      if (config == null) return;
      if (config.binding() == null) return;
      if (!"dynamic".equals(config.binding().mode())) return;

      String source = config.binding().source();
      String widgetType = config.type();
      String logicalId = doc.meta() != null ? doc.meta().id() : null;

      providers.stream()
          .filter(p -> p.supports(logicalId, widgetType, source))
          .findFirst()
          .ifPresentOrElse(provider -> {
            try {
              Object payload = provider.load(doc, widgetId, config, lang, ctx);
              widgets.put(widgetId, payload);
            } catch (Exception e) {
              errors.add(new WidgetDynamicError(
                  widgetId,
                  provider.providerKey(),
                  "PROVIDER_ERROR",
                  safeMsg(e)
              ));
            }
          }, () -> errors.add(new WidgetDynamicError(
              widgetId,
              "resolver",
              "NO_PROVIDER",
              "No provider found for source=" + source
          )));
    });

    return new PageDynamicPayload(widgets, errors);
  }

  private static String safeMsg(Exception e) {
    String msg = e.getMessage();
    return (msg == null || msg.isBlank()) ? e.getClass().getSimpleName() : msg;
  }
}

