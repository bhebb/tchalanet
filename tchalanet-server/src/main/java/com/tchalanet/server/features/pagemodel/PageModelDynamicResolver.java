package com.tchalanet.server.features.pagemodel;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PageModelDynamicResolver {

  private final List<PageModelDynamicProvider> providers;

  public PageDynamicPayload resolve(
      com.tchalanet.server.features.pagemodel.PageModel model,
      String lang,
      TchRequestContext ctx
  ) {

    Map<String, Object> widgets = new HashMap<>();
    List<WidgetDynamicError> errors = new ArrayList<>();

    if (model == null) {
      return new PageDynamicPayload(widgets, errors);
    }
    if (model.content() == null || model.content().widgets() == null) {
      return new PageDynamicPayload(widgets, errors);
    }

    model.content().widgets().forEach((widgetId, config) -> {

      if (config == null) return;
      if (config.binding() == null) return;
      if (!"dynamic".equals(config.binding().mode())) return;

      String source = config.binding().source();
      String widgetType = config.type();

      providers.stream()
          .filter(p -> p.supports(model.meta().id(), widgetType, source))
          .findFirst()
          .ifPresentOrElse(provider -> {
            try {
              Object payload = provider.load(model, widgetId, config, lang, ctx);
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

  // Overload to accept core PageModelDoc and convert to features.pagemodel.PageModel
  public PageDynamicPayload resolve(PageModelDoc doc, String lang, TchRequestContext ctx) {
    if (doc == null) return new PageDynamicPayload(Map.of(), List.of());
    com.tchalanet.server.features.pagemodel.PageModel shared = toShared(doc);
    return resolve(shared, lang, ctx);
  }

  private com.tchalanet.server.features.pagemodel.PageModel toShared(PageModelDoc d) {
    if (d == null) return null;
    var meta = d.meta();
    com.tchalanet.server.features.pagemodel.PageModel.Meta sharedMeta = meta == null ? null :
        new com.tchalanet.server.features.pagemodel.PageModel.Meta(
            meta.id(), meta.scope(), meta.slug(), meta.context(), meta.schemaVersion(), meta.langs(), meta.defaultLang()
        );

    var theme = d.theme();
    com.tchalanet.server.features.pagemodel.PageModel.Theme sharedTheme = theme == null ? null :
        new com.tchalanet.server.features.pagemodel.PageModel.Theme(theme.preset(), theme.mode(), theme.density());

    var shell = d.shell();
    com.tchalanet.server.features.pagemodel.PageModel.Shell sharedShell = null;
    if (shell != null) {
      com.tchalanet.server.features.pagemodel.PageModel.ShellSectionConfig header = shell.header() == null ? null :
          new com.tchalanet.server.features.pagemodel.PageModel.ShellSectionConfig(
              shell.header().component(), shell.header().nav() == null ? null : new com.tchalanet.server.features.pagemodel.PageModel.ShellNav(
                  mapNavItems(shell.header().nav().primary()), mapNavItems(shell.header().nav().secondary())
              ), shell.header().props()
          );
      com.tchalanet.server.features.pagemodel.PageModel.ShellSectionConfig sidenav = shell.sidenav() == null ? null :
          new com.tchalanet.server.features.pagemodel.PageModel.ShellSectionConfig(
              shell.sidenav().component(), shell.sidenav().nav() == null ? null : new com.tchalanet.server.features.pagemodel.PageModel.ShellNav(
                  mapNavItems(shell.sidenav().nav().primary()), mapNavItems(shell.sidenav().nav().secondary())
              ), shell.sidenav().props()
          );
      com.tchalanet.server.features.pagemodel.PageModel.ShellSectionConfig footer = shell.footer() == null ? null :
          new com.tchalanet.server.features.pagemodel.PageModel.ShellSectionConfig(
              shell.footer().component(), shell.footer().nav() == null ? null : new com.tchalanet.server.features.pagemodel.PageModel.ShellNav(
                  mapNavItems(shell.footer().nav().primary()), mapNavItems(shell.footer().nav().secondary())
              ), shell.footer().props()
          );
      sharedShell = new com.tchalanet.server.features.pagemodel.PageModel.Shell(header, sidenav, footer);
    }

    var content = d.content();
    com.tchalanet.server.features.pagemodel.PageModel.Content sharedContent = null;
    if (content != null) {
      com.tchalanet.server.features.pagemodel.PageModel.Layout sharedLayout = null;
      if (content.layout() != null) {
        var rows = content.layout().rows();
        List<com.tchalanet.server.features.pagemodel.PageModel.LayoutRow> sharedRows = null;
        if (rows != null) {
          sharedRows = new ArrayList<>();
          for (var r : rows) {
            List<com.tchalanet.server.features.pagemodel.PageModel.LayoutColumn> sharedCols = null;
            if (r.columns() != null) {
              sharedCols = new ArrayList<>();
              for (var c : r.columns()) {
                sharedCols.add(new com.tchalanet.server.features.pagemodel.PageModel.LayoutColumn(c.span(), c.widgets()));
              }
            }
            sharedRows.add(new com.tchalanet.server.features.pagemodel.PageModel.LayoutRow(r.id(), r.labelKey(), sharedCols));
          }
        }
        sharedLayout = new com.tchalanet.server.features.pagemodel.PageModel.Layout(content.layout().component(), sharedRows);
      }

      Map<String, com.tchalanet.server.features.pagemodel.PageModel.WidgetConfig> sharedWidgets = null;
      if (content.widgets() != null) {
        sharedWidgets = new LinkedHashMap<>();
        for (var e : content.widgets().entrySet()) {
          String k = e.getKey();
          var v = e.getValue();
          com.tchalanet.server.features.pagemodel.PageModel.WidgetBinding b = v.binding() == null ? null :
              new com.tchalanet.server.features.pagemodel.PageModel.WidgetBinding(v.binding().mode(), v.binding().source());
          sharedWidgets.put(k, new com.tchalanet.server.features.pagemodel.PageModel.WidgetConfig(v.type(), b, v.props()));
        }
      }

      sharedContent = new com.tchalanet.server.features.pagemodel.PageModel.Content(sharedLayout, sharedWidgets);
    }

    return new com.tchalanet.server.features.pagemodel.PageModel(sharedMeta, sharedTheme, sharedShell, sharedContent);
  }

  private static String safeMsg(Exception e) {
    String msg = e.getMessage();
    return (msg == null || msg.isBlank()) ? e.getClass().getSimpleName() : msg;
  }

  private List<com.tchalanet.server.features.pagemodel.PageModel.NavItem> mapNavItems(List<com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc.NavItem> src) {
    if (src == null) return null;
    List<com.tchalanet.server.features.pagemodel.PageModel.NavItem> out = new ArrayList<>();
    for (var n : src) {
      out.add(new com.tchalanet.server.features.pagemodel.PageModel.NavItem(n.labelKey(), n.path()));
    }
    return out;
  }
}
