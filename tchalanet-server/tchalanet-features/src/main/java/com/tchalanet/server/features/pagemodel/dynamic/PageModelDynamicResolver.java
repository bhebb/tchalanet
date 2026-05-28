package com.tchalanet.server.features.pagemodel.dynamic;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
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
    Map<String, Object> widgets = new LinkedHashMap<>();
    List<WidgetDynamicError> errors = new ArrayList<>();
    PageModelResolutionContext resolutionContext = new PageModelResolutionContext();

    if (doc == null) return new PageDynamicPayload(widgets, errors);
    resolveShell(doc, "shell.header", doc.shell() == null ? null : doc.shell().header(), lang, ctx, resolutionContext, widgets, errors);
    resolveShell(doc, "shell.sidenav", doc.shell() == null ? null : doc.shell().sidenav(), lang, ctx, resolutionContext, widgets, errors);
    resolveShell(doc, "shell.footer", doc.shell() == null ? null : doc.shell().footer(), lang, ctx, resolutionContext, widgets, errors);

    if (doc.content() == null || doc.content().widgets() == null) {
      return new PageDynamicPayload(widgets, errors);
    }

    doc.content().widgets().forEach((widgetId, config) -> {
      if (config == null) return;
      if (config.binding() == null) return;
      if (!"dynamic".equals(config.binding().mode())) return;

      String source = config.binding().source();
      String widgetType = config.type();

      resolveDynamicConfig(doc, widgetId, widgetType, config, source, lang, ctx, resolutionContext, widgets, errors);
    });

    return new PageDynamicPayload(widgets, errors);
  }

  private void resolveShell(
      PageModelDoc doc,
      String sectionId,
      PageModelDoc.ShellSectionConfig section,
      String lang,
      TchRequestContext ctx,
      PageModelResolutionContext resolutionContext,
      Map<String, Object> widgets,
      List<WidgetDynamicError> errors) {
    if (section == null || section.binding() == null) return;
    if (!"dynamic".equals(section.binding().mode())) return;

    PageModelDoc.WidgetConfig config =
        new PageModelDoc.WidgetConfig(section.component(), section.binding(), section.props());

    resolveDynamicConfig(
        doc,
        sectionId,
        section.component(),
        config,
        section.binding().source(),
        lang,
        ctx,
        resolutionContext,
        widgets,
        errors);
  }

  private void resolveDynamicConfig(
      PageModelDoc doc,
      String widgetId,
      String widgetType,
      PageModelDoc.WidgetConfig config,
      String source,
      String lang,
      TchRequestContext ctx,
      PageModelResolutionContext resolutionContext,
      Map<String, Object> widgets,
      List<WidgetDynamicError> errors) {
    String logicalId = doc.meta() != null ? doc.meta().id() : null;

    providers.stream()
        .filter(p -> p.supports(logicalId, widgetType, source))
        .findFirst()
        .ifPresentOrElse(provider -> {
          try {
            Object payload = provider.load(doc, widgetId, config, lang, ctx, resolutionContext);
            widgets.put(widgetId, payload);
          } catch (PageModelDynamicProviderException e) {
            errors.add(new WidgetDynamicError(
                widgetId,
                provider.providerKey(),
                e.code(),
                safeMsg(e)
            ));
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
            "No provider found for logicalId=" + logicalId
                + ", widgetType=" + widgetType
                + ", source=" + source
        )));
  }

  private static String safeMsg(Exception e) {
    String msg = e.getMessage();
    return (msg == null || msg.isBlank()) ? e.getClass().getSimpleName() : msg;
  }
}
