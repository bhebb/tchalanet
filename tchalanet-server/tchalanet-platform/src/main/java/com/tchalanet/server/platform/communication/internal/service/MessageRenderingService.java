package com.tchalanet.server.platform.communication.internal.service;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.internal.persistence.MessageTemplateJpaRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageRenderingService {

  private final MessageTemplateJpaRepository templates;

  public RenderedMessage render(SendOutboundMessageRequest request) {
    var tenantId = request.recipient() == null || request.recipient().tenantId() == null
        ? null
        : request.recipient().tenantId().value();
    var locale = request.locale() == null ? "fr" : request.locale().toLanguageTag();

    return templates.findBest(tenantId, request.templateKey(), request.channel(), locale)
        .map(template -> new RenderedMessage(
            render(template.getSubjectTemplate(), request.metadata()),
            render(template.getBodyTemplate(), request.metadata())))
        .orElseGet(() -> new RenderedMessage(request.subject(), request.body()));
  }

  private String render(String template, Map<String, Object> variables) {
    if (template == null) {
      return null;
    }

    var rendered = template;
    for (var entry : variables.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
    }
    return rendered;
  }
}
