package com.tchalanet.server.features.pagemodel.dynamic;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelResolutionContext;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.common.web.advice.ApiResponseNotices;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.NoticeSource;
import com.tchalanet.server.features.pagemodel.security.PageModelAllowedRoles;
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
    var resolutionContext = new PageModelResolutionContext();

    if (doc == null) {
      return new PageDynamicPayload(widgets, errors);
    }
    resolveRootShell(doc, lang, ctx, resolutionContext, widgets, errors);
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

  private void resolveRootShell(
      PageModelDoc doc,
      String lang,
      TchRequestContext ctx,
      PageModelResolutionContext resolutionContext,
      Map<String, Object> widgets,
      List<WidgetDynamicError> errors) {
    var shell = doc.shell();
    if (shell == null || shell.binding() == null) return;
    if (!"dynamic".equals(shell.binding().mode())) return;

    var config = new PageModelDoc.WidgetConfig(shell.component(), shell.binding(), shell.props());
    resolveDynamicConfig(
        doc,
        "shell.root",
        shell.component(),
        config,
        shell.binding().source(),
        lang,
        ctx,
        resolutionContext,
        widgets,
        errors);
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
          // [harden-pagemodel-security-v2 / D2] Provider-level role revalidation.
          // If the provider declares @PageModelAllowedRoles and the current role is not in the set,
          // record a widget-level error instead of returning data or throwing a 500.
          PageModelAllowedRoles roleAnnotation =
              provider.getClass().getAnnotation(PageModelAllowedRoles.class);
          if (roleAnnotation != null) {
            var currentRole = ctx != null ? ctx.currentRole() : null;
            boolean permitted =
                currentRole != null
                    && Arrays.asList(roleAnnotation.value()).contains(currentRole);
            if (!permitted) {
              var error = new WidgetDynamicError(
                  widgetId,
                  provider.providerKey(),
                  "PROVIDER_ROLE_DENIED",
                  "Provider " + provider.providerKey()
                      + " requires one of " + Arrays.toString(roleAnnotation.value())
                      + " but current role is " + currentRole
              );
              errors.add(error);
              addWidgetNotice(error, "Page section unavailable.", NoticeSeverity.WARN, null);
              return;
            }
          }
          try {
            Object payload = provider.load(doc, widgetId, config, lang, ctx, resolutionContext);
            widgets.put(widgetId, payload);
          } catch (PageModelDynamicProviderException e) {
            var error = new WidgetDynamicError(
                widgetId,
                provider.providerKey(),
                e.code(),
                safeMsg(e)
            );
            errors.add(error);
            addWidgetNotice(error, "Page section unavailable.", NoticeSeverity.WARN, e);
          } catch (Exception e) {
            var error = new WidgetDynamicError(
                widgetId,
                provider.providerKey(),
                "pagemodel.widget.unavailable",
                safeMsg(e)
            );
            errors.add(error);
            addWidgetNotice(error, "Page section unavailable.", NoticeSeverity.WARN, e);
          }
        }, () -> {
          var error = new WidgetDynamicError(
              widgetId,
              "resolver",
              "pagemodel.widget.no_provider",
              "No provider found for logicalId=" + logicalId
                  + ", widgetType=" + widgetType
                  + ", source=" + source
          );
          errors.add(error);
          addWidgetNotice(error, "Page section unavailable.", NoticeSeverity.WARN, null);
        });
  }

  private static void addWidgetNotice(
      WidgetDynamicError error,
      String message,
      NoticeSeverity severity,
      Exception ex) {
    ApiResponseNotices.add(
        error.code(),
        message,
        "features.pagemodel",
        severity,
        NoticeSource.of(error.widgetId())
            .service(error.provider())
            .operation("loadWidget"),
        ex,
        Map.of(
            "surface", "section",
            "placement", "top",
            "target", error.widgetId()
        )
    );
  }

  private static String safeMsg(Exception e) {
    String msg = e.getMessage();
    return (msg == null || msg.isBlank()) ? e.getClass().getSimpleName() : msg;
  }
}
