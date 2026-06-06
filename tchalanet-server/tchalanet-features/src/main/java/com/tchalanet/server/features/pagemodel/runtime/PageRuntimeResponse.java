package com.tchalanet.server.features.pagemodel.runtime;

import java.util.List;
import java.util.Map;

/** Resolved, storage-agnostic PageModel contract consumed by frontend runtime surfaces. */
public record PageRuntimeResponse(
    PageMeta meta,
    PageThemeHint theme,
    PageShell shell,
    PageContent content,
    DynamicPayload dynamic) {

  public record PageMeta(
      String logicalId,
      String scope,
      String slug,
      int schemaVersion) {}

  public record PageThemeHint(
      String presetId,
      String mode,
      Integer density) {}

  public sealed interface PageShell permits PublicShell, PrivateShell {
    String type();
  }

  public record PublicShell(
      String type,
      Object header,
      Object footer) implements PageShell {}

  public record PrivateShell(
      String type,
      Object topAppBar,
      Object navigationDrawer) implements PageShell {}

  public record PageContent(
      PageLayout layout,
      Map<String, WidgetConfig> widgets) {}

  public record PageLayout(List<LayoutRow> rows) {}

  public record LayoutRow(
      String id,
      String labelKey,
      List<LayoutColumn> columns) {}

  public record LayoutColumn(
      int span,
      List<String> widgets) {}

  public record WidgetConfig(
      String type,
      Object props) {}

  public record DynamicPayload(
      Map<String, Object> widgets,
      List<WidgetError> errors) {}

  public record WidgetError(
      String widgetId,
      String code) {}
}
