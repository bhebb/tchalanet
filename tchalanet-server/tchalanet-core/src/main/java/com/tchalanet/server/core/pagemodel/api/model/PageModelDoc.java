package com.tchalanet.server.core.pagemodel.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageModelDoc(
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
      @JsonProperty("schemaVersion") int schemaVersion,
      @JsonProperty("langs") List<String> langs,
      @JsonProperty("defaultLang") String defaultLang) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Theme(@JsonProperty("presetId") String presetId,
                      @JsonProperty("mode") String mode,
                      @JsonProperty("density") Integer density,
                      @JsonProperty("overrides") Map<String, String> overrides) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Shell(@JsonProperty("header") ShellSectionConfig header,
                      @JsonProperty("sidenav") ShellSectionConfig sidenav,
                      @JsonProperty("footer") ShellSectionConfig footer) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ShellSectionConfig(@JsonProperty("component") String component,
                                   @JsonProperty("binding") WidgetBinding binding,
                                   @JsonProperty("nav") ShellNav nav,
                                   @JsonProperty("props") Map<String, Object> props) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ShellNav(@JsonProperty("primary") List<ActionItem> primary,
                         @JsonProperty("secondary") List<ActionItem> secondary) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ActionItem(@JsonProperty("id") String id,
                           @JsonProperty("kind") String kind,
                           @JsonProperty("labelKey") String labelKey,
                           @JsonProperty("label") String label,
                           @JsonProperty("destination") NavigationDestination destination,
                           @JsonProperty("icon") String icon,
                           @JsonProperty("image") String image,
                           @JsonProperty("activeMatch") String activeMatch,
                           @JsonProperty("disabled") Boolean disabled,
                           @JsonProperty("reasonKey") String reasonKey,
                           @JsonProperty("badge") Object badge,
                           @JsonProperty("children") List<ActionItem> children) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record NavigationDestination(@JsonProperty("kind") String kind,
                                      @JsonProperty("value") String value,
                                      @JsonProperty("requiredRoles") List<String> requiredRoles) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Content(@JsonProperty("layout") Layout layout,
                        @JsonProperty("widgets") Map<String, WidgetConfig> widgets) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Layout(@JsonProperty("component") String component,
                       @JsonProperty("rows") List<LayoutRow> rows) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LayoutRow(@JsonProperty("id") String id,
                          @JsonProperty("labelKey") String labelKey,
                          @JsonProperty("columns") List<LayoutColumn> columns) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LayoutColumn(@JsonProperty("span") int span,
                             @JsonProperty("widgets") List<String> widgets) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
  public record WidgetConfig(@JsonProperty("type") String type,
                              @JsonProperty("binding") WidgetBinding binding,
                              @JsonProperty("props") Map<String, Object> props) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
  public record WidgetBinding(@JsonProperty("mode") String mode,
                               @JsonProperty("source") String source) {}
}
