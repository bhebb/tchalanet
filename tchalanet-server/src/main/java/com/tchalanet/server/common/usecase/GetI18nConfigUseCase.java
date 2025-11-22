package com.tchalanet.server.common.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.context.TchRequestContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * Use case pour récupérer la configuration i18n. Charge les traductions depuis les fichiers JSON
 * statiques.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetI18nConfigUseCase {

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  /**
   * Récupère les traductions i18n pour une langue donnée.
   *
   * @param context le contexte de la requête (pour évolutions futures)
   * @param lang la langue demandée (ex: "fr", "en")
   * @return Map des traductions pour cette langue
   */
  public Map<String, Object> execute(TchRequestContext context, String lang) {
    var translationsByLang = loadI18nOverrides();
    return translationsByLang.getOrDefault(lang, Collections.emptyMap());
  }

  /**
   * Charge les surcharges i18n depuis le fichier de configuration. TODO: Plus tard, interroger la
   * base de données pour les surcharges personnalisées par tenant.
   */
  private Map<String, Map<String, Object>> loadI18nOverrides() {
    try {
      log.debug("Loading i18n overrides from classpath:static/config/i18n-mock.json");
      Resource resource = resourceLoader.getResource("classpath:static/config/i18n-mock.json");
      InputStream inputStream = resource.getInputStream();
      return objectMapper.readValue(inputStream, new TypeReference<>() {});
    } catch (IOException e) {
      log.error("Failed to load i18n overrides", e);
      return Collections.emptyMap();
    }
  }
}
