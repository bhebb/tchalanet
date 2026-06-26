package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.catalog.i18n.api.I18nOverridesCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.internal.persistence.NotificationTemplateJpaRepository;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class NotificationTemplateRenderer {

  private final NotificationTemplateJpaRepository templates;
  private final I18nOverridesCatalog i18nOverridesCatalog;

  public RenderedNotification render(
      TenantId tenantId,
      String titleKey,
      String messageKey,
      Locale locale,
      String fallbackTitle,
      String fallbackMessage,
      JsonNode payload) {
    if ((titleKey == null || titleKey.isBlank()) && (messageKey == null || messageKey.isBlank())) {
      return new RenderedNotification(fallbackTitle, fallbackMessage);
    }

    var tenantUuid = tenantId == null ? null : tenantId.value();
    var languageTag = locale == null ? "fr" : locale.toLanguageTag();
    var variables = variables(payload);
    var overrides =
        tenantId == null
            ? i18nOverridesCatalog.resolveLocale(languageTag)
            : i18nOverridesCatalog.resolveLocaleForTenant(languageTag, tenantId);
    if (overrides == null) {
      overrides = Map.of();
    }

    var overrideTitle = translationFromOverrides(overrides, titleKey, variables);
    var overrideMessage = translationFromOverrides(overrides, messageKey, variables);
    if (overrideTitle != null || overrideMessage != null) {
      return new RenderedNotification(
          overrideTitle == null ? fallbackTitle : overrideTitle,
          overrideMessage == null ? fallbackMessage : overrideMessage);
    }

    var templateKey = firstNonBlank(titleKey, messageKey);
    return templates.findBest(tenantUuid, templateKey, languageTag)
        .map(template -> new RenderedNotification(
            renderTemplate(template.getTitleTemplate(), variables),
            renderTemplate(template.getBodyTemplate(), variables)))
        .orElseGet(() -> new RenderedNotification(fallbackTitle, fallbackMessage));
  }

  private Map<String, Object> variables(JsonNode payload) {
    var variables = new LinkedHashMap<String, Object>();
    if (payload == null || !payload.isObject()) {
      return variables;
    }

    payload.properties().forEach(entry -> variables.put(entry.getKey(), entry.getValue().asText()));
    return variables;
  }

  private String translationFromOverrides(
      Map<String, String> overrides, String key, Map<String, Object> variables) {
    if (key == null || key.isBlank()) {
      return null;
    }
    var value = overrides.get(key);
    return value == null ? null : renderTemplate(value, variables);
  }

  private String renderTemplate(String template, Map<String, Object> variables) {
    var rendered = template == null ? "" : template;
    for (var entry : variables.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
    }
    return rendered;
  }

  private static String firstNonBlank(String first, String second) {
    return first != null && !first.isBlank() ? first : second;
  }
}
