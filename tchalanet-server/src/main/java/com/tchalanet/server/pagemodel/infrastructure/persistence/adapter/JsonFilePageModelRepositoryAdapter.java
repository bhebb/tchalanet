package com.tchalanet.server.pagemodel.infrastructure.persistence.adapter;

import com.tchalanet.server.pagemodel.domain.model.PageModel;
import com.tchalanet.server.pagemodel.domain.ports.out.PageModelRepositoryPort;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component("jsonFilePageModelRepositoryAdapter") // Qualify the bean name
@RequiredArgsConstructor
@Slf4j
public class JsonFilePageModelRepositoryAdapter implements PageModelRepositoryPort {

  private final ResourceLoader resourceLoader;

  private static final String BASE_PATH = "classpath:pages/";
  private static final UUID PLATFORM_TENANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001"); // From GetPublicHomePageService

  @Override
  public Optional<PageModel> findByTenantAndCodeAndLang(UUID tenantId, String code, String lang) {
    String filePath = getJsonFilePath(tenantId, code, lang);

    return getPageModel(tenantId, code, lang, filePath);
  }

  private static String getJsonFilePath(UUID tenantId, String code, String lang) {
    String filePath;
    if (PLATFORM_TENANT_ID.equals(tenantId)) {
      filePath = String.format("%spublic/%s_%s.json", BASE_PATH, code, lang);
    } else {
      filePath =
          String.format("%sprivate/%s/%s_%s.json", BASE_PATH, tenantId.toString(), code, lang);
    }
    return filePath;
  }

  private Optional<PageModel> getPageModel(
      UUID tenantId, String code, String lang, String filePath) {
    Resource resource = resourceLoader.getResource(filePath);
    if (resource.exists()) {
      try {
        String jsonContent = new String(resource.getInputStream().readAllBytes());
        // For simplicity, we'll create a PageModel directly.
        // In a real scenario, you might have a specific DTO for JSON files.
        return Optional.of(new PageModel(UUID.randomUUID(), tenantId, code, lang, jsonContent));
      } catch (IOException e) {
        log.error("Failed to read PageModel from file: {}", filePath, e);
        return Optional.empty();
      }
    }
    return Optional.empty();
  }
}
