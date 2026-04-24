package com.tchalanet.server.core.pagemodel.infra.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelTemplateLoaderPort;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import java.io.InputStream;
import org.springframework.stereotype.Component;

@Component
public class ClasspathPageModelTemplateLoader implements PageModelTemplateLoaderPort {

  private final ObjectMapper mapper = new ObjectMapper();

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
      return mapper.readValue(is, PageModelDoc.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load PageModel template " + logicalId, e);
    }
  }
}

