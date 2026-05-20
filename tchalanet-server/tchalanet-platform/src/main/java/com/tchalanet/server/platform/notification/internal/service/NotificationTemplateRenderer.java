package com.tchalanet.server.platform.notification.internal.service;

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

  public RenderedNotification render(
      TenantId tenantId,
      String templateKey,
      Locale locale,
      String fallbackTitle,
      String fallbackMessage,
      JsonNode payload) {
    if (templateKey == null || templateKey.isBlank()) {
      return new RenderedNotification(fallbackTitle, fallbackMessage);
    }

    var tenantUuid = tenantId == null ? null : tenantId.value();
    var languageTag = locale == null ? "fr" : locale.toLanguageTag();
    var variables = variables(payload);

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

  private String renderTemplate(String template, Map<String, Object> variables) {
    var rendered = template == null ? "" : template;
    for (var entry : variables.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
    }
    return rendered;
  }
}
