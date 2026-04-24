package com.tchalanet.server.features.pagemodel_backup.shared;

import com.tchalanet.server.common.context.TchRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PageModelDynamicResolver {
  private final List<PageModelDynamicProvider> providers;

  public PageDynamicPayload resolve(
      PageModel model,
      String lang,
      TchRequestContext ctx) {

    Map<String, Object> widgets = new HashMap<>();
    List<WidgetDynamicError> errors = new ArrayList<>();

    if (model == null || model.content() == null || model.content().widgets() == null) {
      return new PageDynamicPayload(widgets, errors);
    }

    model.content().widgets().forEach((widgetId, config) -> {
      if (config.binding() == null || !"dynamic".equals(config.binding().mode())) {
        return;
      }

      String source = config.binding().source();
      providers.stream()
          .filter(p -> p.supports(model.meta().id(), config.type(), source))
          .findFirst()
          .ifPresentOrElse(provider -> {
            try {
              Object payload = provider.load(model, config, lang, ctx);
              widgets.put(widgetId, payload);
            } catch (Exception e) {
              errors.add(new WidgetDynamicError(
                  widgetId,
                  provider.providerKey(),
                  "PROVIDER_ERROR",
                  e.getMessage()
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
}

