package com.tchalanet.server.features.pagemodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Canonical PageModel used across features (public, tenant, admin).
 * Copied from legacy pagemodel_backup for progressive migration.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageModel(
    @JsonProperty("meta") Meta meta,
    @JsonProperty("theme") Theme theme,
    @JsonProperty("shell") Shell shell,
    @JsonProperty("content") Content content) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Meta(
      @JsonProperty("id") String id,
      @JsonProperty("scope") String scope,
      @JsonProperty("slug") String slug,
      @JsonProperty("context") String context,
      @JsonProperty("schema_version") int schemaVersion,
      @JsonProperty("langs") List<String> langs,
      @JsonProperty("default_lang") String defaultLang) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Theme(
      @JsonProperty("preset") String preset,
      @JsonProperty("mode") String mode,
      @JsonProperty("density") Integer density) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Shell(
      @JsonProperty("header") ShellSectionConfig header,
      @JsonProperty("sidenav") ShellSectionConfig sidenav,
      @JsonProperty("footer") ShellSectionConfig footer) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ShellSectionConfig(
      @JsonProperty("component") String component,
      @JsonProperty("nav") ShellNav nav,
      @JsonProperty("props") Map<String, Object> props) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ShellNav(
      @JsonProperty("primary") List<NavItem> primary,
      @JsonProperty("secondary") List<NavItem> secondary) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record NavItem(
      @JsonProperty("label_key") String labelKey, @JsonProperty("path") String path) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Content(
      @JsonProperty("layout") Layout layout,
      @JsonProperty("widgets") Map<String, WidgetConfig> widgets) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Layout(
      @JsonProperty("component") String component, @JsonProperty("rows") List<LayoutRow> rows) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LayoutRow(
      @JsonProperty("id") String id,
      @JsonProperty("label_key") String labelKey,
      @JsonProperty("columns") List<LayoutColumn> columns) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LayoutColumn(
      @JsonProperty("span") int span, @JsonProperty("widgets") List<String> widgets) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record WidgetConfig(
      @JsonProperty("type") String type,
      @JsonProperty("binding") WidgetBinding binding,
      @JsonProperty("props") Map<String, Object> props) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record WidgetBinding(
      @JsonProperty("mode") String mode, @JsonProperty("source") String source) {}
}

