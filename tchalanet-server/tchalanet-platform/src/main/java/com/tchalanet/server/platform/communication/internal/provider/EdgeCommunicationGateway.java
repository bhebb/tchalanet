package com.tchalanet.server.platform.communication.internal.provider;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.internal.config.EdgeCommunicationProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Adapter for sending outbound messages through tchalanet-edge-service. */
@Component
@RequiredArgsConstructor
@Slf4j
public class EdgeCommunicationGateway {

  private static final String PROVIDER = "edge";

  private final EdgeCommunicationProperties properties;
  private final EdgeHmacSigner hmacSigner;

  @Qualifier("edgeCommunicationClient")
  private final RestClient edgeCommunicationClient;

  public SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    if (!properties.enabled()) {
      log.debug("Edge communication integration disabled, skipping send");
      return SendOutboundMessageResult.skipped(PROVIDER, "disabled");
    }

    var requestId = extractRequestId(request);
    var idempotencyKey = extractIdempotencyKey(request, requestId);
    var edgeRequest = toEdgeRequest(request, requestId);
    var signed = hmacSigner.sign(properties.hmacSecret(), edgeRequest);

    try {
      edgeCommunicationClient
          .post()
          .uri(properties.messagesPath())
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-Request-Id", requestId)
          .header("Idempotency-Key", idempotencyKey)
          .header("X-Tch-Timestamp", signed.timestamp())
          .header("X-Tch-Signature", signed.signature())
          .body(signed.rawJsonBody())
          .retrieve()
          .toBodilessEntity();

      log.debug(
          "Edge message sent successfully: type={} channel={}", request.type(), request.channel());
      return SendOutboundMessageResult.sent(PROVIDER);
    } catch (Exception e) {
      log.error(
          "Failed to send edge message: type={} channel={}", request.type(), request.channel(), e);
      throw new EdgeCommunicationException("Failed to send message through edge service", e);
    }
  }

  private EdgeCommunicationRequest toEdgeRequest(
      SendOutboundMessageRequest request, String requestId) {
    return new EdgeCommunicationRequest(
        extractEventId(request, requestId),
        extractString(request, "severity", "INFO"),
        extractString(request, "title", request.type()),
        extractString(request, "message", ""),
        buildRecipients(request),
        buildContext(request));
  }

  private List<EdgeCommunicationRecipient> buildRecipients(SendOutboundMessageRequest request) {
    var channel = request.channel().name();
    var recipient = request.recipient();

    return switch (request.channel()) {
      case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK ->
          List.of(
              new EdgeCommunicationRecipient(
                  channel,
                  null,
                  firstNonBlank(
                      recipient != null ? recipient.channelKey() : null,
                      extractOptionalString(request, "channelKey"),
                      "batch-draws")));
      case EMAIL, SMS, WHATSAPP, PUSH ->
          List.of(
              new EdgeCommunicationRecipient(
                  channel,
                  firstNonBlank(
                      recipient != null ? recipient.to() : null,
                      extractOptionalString(request, "to"),
                      null),
                  null));
    };
  }

  private Map<String, Object> buildContext(SendOutboundMessageRequest request) {
    var context = new HashMap<String, Object>(request.metadata());
    var recipient = request.recipient();

    if (recipient != null) {
      if (recipient.tenantId() != null) {
        context.put("tenantId", recipient.tenantId().value().toString());
      }
      if (recipient.userId() != null) {
        context.put("userId", recipient.userId().value().toString());
      }
    }

    if (request.locale() != null) {
      context.put("locale", request.locale().toString());
    }

    context.put("type", request.type());
    return context;
  }

  private String extractEventId(SendOutboundMessageRequest request, String requestId) {
    return firstNonBlank(extractOptionalString(request, "eventId"), requestId, "evt_" + UUID.randomUUID());
  }

  private String extractRequestId(SendOutboundMessageRequest request) {
    return firstNonBlank(extractOptionalString(request, "requestId"), UUID.randomUUID().toString());
  }

  private String extractIdempotencyKey(SendOutboundMessageRequest request, String requestId) {
    return firstNonBlank(extractOptionalString(request, "idempotencyKey"), request.type() + "_" + requestId);
  }

  private String extractString(
      SendOutboundMessageRequest request, String key, String defaultValue) {
    return firstNonBlank(extractOptionalString(request, key), defaultValue);
  }

  private String extractOptionalString(SendOutboundMessageRequest request, String key) {
    var value = request.metadata().get(key);
    return value == null ? null : value.toString();
  }

  private String firstNonBlank(String first, String second) {
    return firstNonBlank(first, second, null);
  }

  private String firstNonBlank(String first, String second, String third) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    if (second != null && !second.isBlank()) {
      return second;
    }
    return third;
  }

  record EdgeCommunicationRequest(
      String eventId,
      String severity,
      String title,
      String message,
      List<EdgeCommunicationRecipient> recipients,
      Map<String, Object> context) {}

  record EdgeCommunicationRecipient(String channel, String to, String channelKey) {}
}
