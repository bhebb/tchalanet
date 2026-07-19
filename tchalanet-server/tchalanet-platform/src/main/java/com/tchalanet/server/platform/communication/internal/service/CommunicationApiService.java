package com.tchalanet.server.platform.communication.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.error.CommunicationErrorCodes;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
import com.tchalanet.server.platform.communication.api.model.value.MessageId;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProviderRegistry;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaEntity;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaRepository;
import java.time.Clock;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class CommunicationApiService implements CommunicationApi {

  private final OutboundMessageJpaRepository messages;
  private final MessageRenderingService renderingService;
  private final DeliveryPolicyResolver deliveryPolicyResolver;
  private final DeliveryProviderRegistry providers;
  private final JsonUtils jsonUtils;
  private final Clock clock;

  @Override
  @Transactional
  public MessageId enqueue(SendOutboundMessageRequest request) {
    if (request == null || request.channel() == null) {
      throw ProblemRest.of(CommunicationErrorCodes.REQUEST_INVALID);
    }
    if (!deliveryPolicyResolver.canCreateOutboundMessage(request)) {
      throw ProblemRest.of(CommunicationErrorCodes.CHANNEL_DISABLED);
    }

    try {
      var tenantId =
          request.recipient() == null || request.recipient().tenantId() == null
              ? null
              : request.recipient().tenantId().value();
      var correlationKey = normalize(request.correlationKey());

      if (correlationKey != null) {
        var existing = messages.findByTenantAndCorrelationKey(tenantId, correlationKey);
        if (existing.isPresent()) {
          return MessageId.of(existing.get().getId());
        }
      }

      var rendered = renderingService.render(request);
      var entity = new OutboundMessageJpaEntity();
      entity.setTenantId(tenantId);
      entity.setSourceEventId(null);
      entity.setChannel(request.channel());
      entity.setRecipientType(recipientType(request));
      entity.setRecipientValue(recipientValue(request));
      entity.setTemplateKey(request.templateKey());
      entity.setLocale(request.locale() == null ? null : request.locale().toLanguageTag());
      entity.setSubject(rendered.subject());
      entity.setBody(rendered.body());
      entity.setPayload(jsonUtils.toJsonNode(request.metadataWithAttachments()));
      entity.setPriority(request.priority());
      entity.setStatus(DeliveryStatus.PENDING);
      entity.setCorrelationKey(correlationKey);
      entity.setNextAttemptAt(clock.instant());

      return MessageId.of(messages.save(entity).getId());
    } catch (ProblemRestException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      log.error("communication.enqueue.failed channel={}", request.channel(), ex);
      throw ProblemRest.of(CommunicationErrorCodes.ENQUEUE_FAILED, java.util.Map.of(), ex);
    }
  }

  @Override
  public SendOutboundMessageResult sendNow(SendOutboundMessageRequest request) {
    if (request == null || request.channel() == null) {
      throw ProblemRest.of(CommunicationErrorCodes.REQUEST_INVALID);
    }
    try {
      return providers.providerFor(request.channel()).send(request);
    } catch (ProblemRestException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      log.error("communication.send_now.failed channel={}", request.channel(), ex);
      throw ProblemRest.of(CommunicationErrorCodes.DELIVERY_FAILED, java.util.Map.of(), ex);
    }
  }

  private String recipientType(SendOutboundMessageRequest request) {
    return switch (request.channel()) {
      case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK -> "SLACK_CHANNEL";
      case EMAIL -> "EMAIL";
      case SMS, WHATSAPP -> "PHONE";
      case PUSH -> "PUSH_TOKEN";
    };
  }

  private String recipientValue(SendOutboundMessageRequest request) {
    var recipient = request.recipient();
    if (recipient == null) {
      return "unknown";
    }

    return switch (request.channel()) {
      case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK ->
          firstNonBlank(recipient.channelKey(), "default");
      case EMAIL, SMS, WHATSAPP, PUSH -> firstNonBlank(recipient.to(), "unknown");
    };
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }

    var normalized = value.trim();
    return normalized.isBlank() ? null : normalized.toLowerCase(Locale.ROOT);
  }
}
