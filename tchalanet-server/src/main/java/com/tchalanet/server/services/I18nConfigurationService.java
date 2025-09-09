package com.tchalanet.server.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.config.context.RequestContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class I18nConfigurationService {

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  public I18nConfigurationService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
    this.objectMapper = objectMapper;
    this.resourceLoader = resourceLoader;
  }

  private Map<String, Map<String, Object>> loadI18nOverrides() {
    try {
      log.debug("Loading i18n overrides from classpath:static/config/i18n-mock.json");
      Resource resource = resourceLoader.getResource("classpath:static/config/i18n-mock.json");
      InputStream inputStream = resource.getInputStream();
      return objectMapper.readValue(inputStream, new TypeReference<>() {});
    } catch (IOException e) {
      // In a real app, you'd want to log this error.
      // For this example, we'll just initialize with an empty map.
      log.error("Failed to load i18n overrides", e);
    }
    log.info("No i18n overrides found");
    return Collections.emptyMap();
  }

  /**
   * Récupère les surcharges de traduction pour un contexte et une langue donnés. NOTE : Plus tard,
   * cette méthode interrogera la base de données.
   */
  public Map<String, Object> getMergedI18n(RequestContext context, String lang) {
    // The context parameter is not used for now, but it's there for future enhancements.
    var translationsByLang = loadI18nOverrides();
    return translationsByLang.getOrDefault(lang, Collections.emptyMap());
  }
}
