package com.tchalanet.server.features.pagemodel.dynamic.providers.json;

import com.tchalanet.server.common.context.TchRequestContext;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelDoc;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProvider;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProviderException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class JsonFileProvider implements PageModelDynamicProvider {

  public static final String SOURCE = "json_file";

  private final PageModelJsonFragmentRegistry registry;
  private final JsonUtils jsonUtils;
  private final ConcurrentMap<String, JsonNode> cache = new ConcurrentHashMap<>();

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
      TchRequestContext ctx) {
    String fileKey = readString(widgetConfig == null ? null : widgetConfig.props(), "file_key");
    String resourcePath = registry.resolve(fileKey);
    return cache.computeIfAbsent(fileKey, ignored -> loadJson(fileKey, resourcePath));
  }

  private JsonNode loadJson(String fileKey, String resourcePath) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new PageModelDynamicProviderException(
            "JSON_FRAGMENT_NOT_FOUND", "JSON fragment resource not found: " + fileKey);
      }

      return jsonUtils.parse(is);
    } catch (PageModelDynamicProviderException e) {
      throw e;
    } catch (Exception e) {
      throw new PageModelDynamicProviderException(
          "JSON_FRAGMENT_INVALID", "Invalid JSON fragment: " + fileKey, e);
    }
  }

  @Override
  public String providerKey() {
    return SOURCE;
  }

  private static String readString(Map<String, Object> props, String key) {
    if (props == null) {
      return null;
    }

    Object value = props.get(key);
    return value instanceof String s ? s.trim() : null;
  }
}
