package com.tchalanet.server.core.pagemodel.internal.infra.resources;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelTemplateLoaderPort;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClasspathPageModelTemplateLoader implements PageModelTemplateLoaderPort {

  private final JsonUtils jsonUtils;

  @Override
  public PageModelDoc loadFromResources(String logicalId) {
    if (logicalId == null || logicalId.isBlank()) {
      throw new IllegalArgumentException("logicalId required");
    }
    String path = "/pagemodel/" + logicalId + ".json";
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        // return a minimal empty PageModelDoc to avoid NPEs in runtime
        return new PageModelDoc(null, null, null, null);
      }
      return jsonUtils.readValue(is, PageModelDoc.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load PageModel template " + logicalId, e);
    }
  }
}
