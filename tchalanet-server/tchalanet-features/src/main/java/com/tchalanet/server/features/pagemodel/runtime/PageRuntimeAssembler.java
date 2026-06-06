package com.tchalanet.server.features.pagemodel.runtime;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.shared.PageDynamicPayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/** Maps the internal PageModel definition to the small frontend runtime contract. */
@Component
public class PageRuntimeAssembler {

  private static final Set<String> INTERNAL_KEYS =
      Set.of(
          "binding",
          "fileKey",
          "fragmentType",
          "schemaVersion",
          "fragment_type",
          "schema_version");

  private final JsonUtils jsonUtils;

  public PageRuntimeAssembler(JsonUtils jsonUtils) {
    this.jsonUtils = jsonUtils;
  }

  public PageRuntimeResponse assemble(PageModelDoc doc, PageDynamicPayload resolved) {
    Map<String, Object> resolvedWidgets =
        resolved == null || resolved.widgets() == null
            ? Map.of()
            : resolved.widgets();

    var content = content(doc == null ? null : doc.content(), resolvedWidgets);
    var shell = shell(doc, resolvedWidgets);
    var dynamic = dynamic(doc, resolved, resolvedWidgets);

    return new PageRuntimeResponse(meta(doc), theme(doc), shell, content, dynamic);
  }

  private PageRuntimeResponse.PageMeta meta(PageModelDoc doc) {
    var meta = doc == null ? null : doc.meta();
    return new PageRuntimeResponse.PageMeta(
        meta == null ? null : meta.id(),
        meta == null ? null : meta.scope(),
        meta == null ? null : meta.slug(),
        meta == null ? 0 : meta.schemaVersion());
  }

  private PageRuntimeResponse.PageThemeHint theme(PageModelDoc doc) {
    var theme = doc == null ? null : doc.theme();
    return theme == null
        ? null
        : new PageRuntimeResponse.PageThemeHint(theme.presetId(), theme.mode(), theme.density());
  }

  private PageRuntimeResponse.PageShell shell(
      PageModelDoc doc, Map<String, Object> resolvedWidgets) {
    boolean isPublic = doc != null && doc.meta() != null && "public".equals(doc.meta().scope());
    if (isPublic) {
      return new PageRuntimeResponse.PublicShell(
          "public",
          publicHeader(resolvedWidgets.get("shell.header")),
          normalize(resolvedWidgets.get("shell.footer")));
    }

    Object root = normalize(resolvedWidgets.get("shell.root"));
    if (root instanceof Map<?, ?> rootMap) {
      return new PageRuntimeResponse.PrivateShell(
          "private",
          value(rootMap, "topAppBar"),
          value(rootMap, "navigationDrawer"));
    }
    return new PageRuntimeResponse.PrivateShell("private", Map.of(), Map.of());
  }

  private Object publicHeader(Object value) {
    Object normalized = normalize(value);
    if (normalized instanceof Map<?, ?> raw) {
      @SuppressWarnings("unchecked")
      Map<String, Object> header = new LinkedHashMap<>((Map<String, Object>) raw);
      if (!header.containsKey("actions") && header.containsKey("secondary")) {
        header.put("actions", header.remove("secondary"));
      }
      return header;
    }
    return normalized;
  }

  private PageRuntimeResponse.PageContent content(
      PageModelDoc.Content content,
      Map<String, Object> resolvedWidgets) {
    Map<String, PageRuntimeResponse.WidgetConfig> widgets = new LinkedHashMap<>();
    if (content != null && content.widgets() != null) {
      content.widgets().forEach(
          (id, config) -> {
            if (config == null) {
              return;
            }
            Object props =
                isJsonFragment(config)
                    ? normalize(resolvedWidgets.get(id))
                    : normalize(config.props());
            widgets.put(id, new PageRuntimeResponse.WidgetConfig(config.type(), props));
          });
    }

    List<PageRuntimeResponse.LayoutRow> rows = new ArrayList<>();
    if (content != null && content.layout() != null && content.layout().rows() != null) {
      for (var row : content.layout().rows()) {
        List<PageRuntimeResponse.LayoutColumn> columns = new ArrayList<>();
        if (row.columns() != null) {
          for (var column : row.columns()) {
            columns.add(
                new PageRuntimeResponse.LayoutColumn(
                    column.span(),
                    column.widgets() == null ? List.of() : column.widgets()));
          }
        }
        rows.add(new PageRuntimeResponse.LayoutRow(row.id(), row.labelKey(), columns));
      }
    }

    return new PageRuntimeResponse.PageContent(
        new PageRuntimeResponse.PageLayout(rows),
        widgets);
  }

  private PageRuntimeResponse.DynamicPayload dynamic(
      PageModelDoc doc,
      PageDynamicPayload resolved,
      Map<String, Object> resolvedWidgets) {
    Map<String, Object> widgets = new LinkedHashMap<>();
    resolvedWidgets.forEach(
        (id, payload) -> {
          if (!id.startsWith("shell.") && !isJsonFragment(doc, id)) {
            widgets.put(id, normalize(payload));
          }
        });

    List<PageRuntimeResponse.WidgetError> errors =
        resolved == null || resolved.errors() == null
            ? List.of()
            : resolved.errors().stream()
                .filter(
                    error ->
                        error != null
                            && error.widgetId() != null
                            && !error.widgetId().startsWith("shell."))
                .map(error -> new PageRuntimeResponse.WidgetError(error.widgetId(), error.code()))
                .toList();

    return new PageRuntimeResponse.DynamicPayload(widgets, errors);
  }

  private boolean isJsonFragment(PageModelDoc doc, String widgetId) {
    if (doc == null || doc.content() == null || doc.content().widgets() == null) {
      return false;
    }
    return isJsonFragment(doc.content().widgets().get(widgetId));
  }

  private boolean isJsonFragment(PageModelDoc.WidgetConfig config) {
    return config != null
        && config.binding() != null
        && "dynamic".equals(config.binding().mode())
        && "jsonFile".equalsIgnoreCase(config.binding().source());
  }

  private Object normalize(Object value) {
    if (value == null) {
      return Map.of();
    }
    Object plain =
        jsonUtils.convertValue(value, new TypeReference<Object>() {});
    return normalizePlain(plain);
  }

  private Object normalizePlain(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> normalized = new LinkedHashMap<>();
      map.forEach(
          (rawKey, rawValue) -> {
            String key = camelCase(String.valueOf(rawKey));
            if (INTERNAL_KEYS.contains(key) || INTERNAL_KEYS.contains(String.valueOf(rawKey))) {
              return;
            }
            normalized.put(key, normalizePlain(rawValue));
          });
      normalizeDestination(normalized);
      return normalized;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(this::normalizePlain).toList();
    }
    return value;
  }

  private void normalizeDestination(Map<String, Object> value) {
    Object path = value.remove("path");
    Object href = value.remove("href");
    String kind = value.get("kind") instanceof String string ? string : null;
    boolean actionLike =
        path != null
            || href != null
            || (kind != null && (value.containsKey("destination") || value.containsKey("labelKey")));
    if (!actionLike) {
      return;
    }
    value.remove("type");
    value.remove("style");
    value.remove("confirm");
    if (path instanceof String route && !route.isBlank()) {
      value.put("destination", Map.of("kind", "route", "value", route));
    } else if (href instanceof String url && !url.isBlank()) {
      value.put("destination", Map.of("kind", "url", "value", url));
    }
    if ("internal".equals(kind)) {
      value.put("kind", "link");
    } else if ("external".equals(kind)) {
      value.put("kind", "externalLink");
    } else if ("action".equals(kind)) {
      value.put("kind", "button");
    }
    Object activeMatch = value.get("activeMatch");
    if (activeMatch != null && !"exact".equals(activeMatch) && !"prefix".equals(activeMatch)) {
      value.remove("activeMatch");
    }
  }

  private Object value(Map<?, ?> map, String key) {
    Object value = map.get(key);
    return value == null ? Map.of() : value;
  }

  private String camelCase(String key) {
    if (!key.contains("_")) {
      return key;
    }
    String[] parts = key.toLowerCase(Locale.ROOT).split("_");
    StringBuilder result = new StringBuilder(parts[0]);
    for (int index = 1; index < parts.length; index++) {
      if (!parts[index].isEmpty()) {
        result.append(Character.toUpperCase(parts[index].charAt(0))).append(parts[index].substring(1));
      }
    }
    return result.toString();
  }
}
