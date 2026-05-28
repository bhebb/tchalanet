package com.tchalanet.server.platform.communication.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.communication.api.model.request.OutboundAttachment;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProviderRegistry;
import com.tchalanet.server.platform.communication.internal.persistence.MessageDeliveryAttemptJpaEntity;
import com.tchalanet.server.platform.communication.internal.persistence.MessageDeliveryAttemptJpaRepository;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaEntity;
import com.tchalanet.server.platform.communication.internal.persistence.OutboundMessageJpaRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboundMessageDispatcher {

  private static final int BATCH_SIZE = 25;

  private final OutboundMessageJpaRepository messages;
  private final MessageDeliveryAttemptJpaRepository attempts;
  private final DeliveryProviderRegistry providers;
  private final DeliveryRetryPlanner retryPlanner;
  private final JsonUtils jsonUtils;
  private final Clock clock;

  @Transactional
  public int dispatchDueMessages() {
    var due = messages.findDueForDispatch(
        DeliveryStatus.PENDING,
        clock.instant(),
        PageRequest.of(0, BATCH_SIZE));

    due.forEach(this::dispatchOne);
    return due.size();
  }

  private void dispatchOne(OutboundMessageJpaEntity message) {
    var now = clock.instant();
    message.setStatus(DeliveryStatus.DISPATCHING);
    var request = toRequest(message);

    try {
      var result = providers.providerFor(message.getChannel()).send(request);
      recordAttempt(message, result.sent() ? DeliveryStatus.SENT : DeliveryStatus.SKIPPED,
          result.provider(), null, result.reason());
      if (result.sent()) {
        message.setStatus(DeliveryStatus.SENT);
        message.setSentAt(now);
        message.setFailureReason(null);
      } else {
        message.setStatus(DeliveryStatus.SKIPPED);
        message.setFailedAt(now);
        message.setFailureReason(result.reason());
      }
    } catch (RuntimeException ex) {
      log.warn("Communication dispatch failed messageId={} channel={}",
          message.getId(), message.getChannel(), ex);
      recordAttempt(message, DeliveryStatus.FAILED, message.getChannel().name(), "PROVIDER_ERROR", ex.getMessage());
      message.setStatus(DeliveryStatus.PENDING);
      message.setNextAttemptAt(retryPlanner.nextAttempt(now, 1));
      message.setFailureReason(ex.getMessage());
    }
  }

  private void recordAttempt(
      OutboundMessageJpaEntity message,
      DeliveryStatus status,
      String provider,
      String errorCode,
      String errorMessage) {
    var attempt = new MessageDeliveryAttemptJpaEntity();
    attempt.setMessageId(message.getId());
    attempt.setAttemptedAt(clock.instant());
    attempt.setStatus(status);
    attempt.setProvider(provider == null ? "unknown" : provider);
    attempt.setErrorCode(errorCode);
    attempt.setErrorMessage(errorMessage);
    attempts.save(attempt);
  }

  private SendOutboundMessageRequest toRequest(OutboundMessageJpaEntity message) {
    var recipient = switch (message.getChannel()) {
      case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK -> OutboundRecipient.slack(message.getRecipientValue());
      case EMAIL, SMS, WHATSAPP, PUSH -> new OutboundRecipient(null, null, message.getRecipientValue(), null);
    };

    var persistedMetadata = metadata(message);
    var metadata = new LinkedHashMap<String, Object>(persistedMetadata);
    var attachments = attachments(metadata.remove("attachments"));
    metadata.put("subject", nullToEmpty(message.getSubject()));
    metadata.put("title", nullToEmpty(message.getSubject()));
    metadata.put("message", nullToEmpty(message.getBody()));
    metadata.put("body", nullToEmpty(message.getBody()));
    metadata.put("correlationKey", nullToEmpty(message.getCorrelationKey()));

    return new SendOutboundMessageRequest(
        message.getTemplateKey(),
        message.getChannel(),
        recipient,
        message.getLocale() == null ? Locale.FRENCH : Locale.forLanguageTag(message.getLocale()),
        metadata,
        attachments);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> metadata(OutboundMessageJpaEntity message) {
    if (message.getPayload() == null || message.getPayload().isNull()) {
      return Map.of();
    }
    return jsonUtils.convertValue(message.getPayload(), new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
  }

  private List<OutboundAttachment> attachments(Object raw) {
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    return list.stream()
        .map(OutboundAttachment::fromMetadata)
        .toList();
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
