package com.tchalanet.server.platform.communication.internal.event;

import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.MessagePriority;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.notification.api.NotificationRecipientResolver;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationPublishedEvent;
import com.tchalanet.server.platform.notification.api.model.view.NotificationRecipientContact;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublishedCommunicationListener {

  private static final String DEFAULT_SLACK_CHANNEL_KEY = "notifications";

  private final CommunicationApi communicationApi;
  private final List<NotificationRecipientResolver> recipientResolvers;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(NotificationPublishedEvent event) {
    if (event.deliveryChannels() == null || event.deliveryChannels().isEmpty()) {
      return;
    }

    for (var channel : event.deliveryChannels()) {
      if (channel == NotificationDeliveryChannel.IN_APP) {
        continue;
      }
      try {
        toRequests(event, channel).forEach(communicationApi::enqueue);
      } catch (Exception e) {
        log.warn(
            "notification.communication.enqueue.failed notificationId={} channel={}",
            event.notificationId(),
            channel,
            e);
      }
    }
  }

  private List<SendOutboundMessageRequest> toRequests(
      NotificationPublishedEvent event, NotificationDeliveryChannel channel) {
    var communicationChannel = communicationChannel(channel);
    var recipients = recipients(event, communicationChannel);
    if (recipients.isEmpty()) {
      log.info(
          "notification.communication.skipped_missing_recipient notificationId={} channel={}",
          event.notificationId(),
          channel);
      return List.of();
    }

    return recipients.stream()
        .map(recipient -> toRequest(event, communicationChannel, recipient))
        .toList();
  }

  private SendOutboundMessageRequest toRequest(
      NotificationPublishedEvent event,
      CommunicationChannel communicationChannel,
      OutboundRecipient recipient) {
    var metadata = new LinkedHashMap<String, Object>();
    metadata.put("templateKey", "notification.published");
    metadata.put("correlationKey", correlationKey(event, communicationChannel, recipient));
    metadata.put("notificationId", event.notificationId().value().toString());
    if (event.publicationId() != null) {
      metadata.put("publicationId", event.publicationId().value().toString());
    }
    if (event.tenantId() != null) {
      metadata.put("tenantId", event.tenantId().value().toString());
    }
    if (recipient.userId() != null) {
      metadata.put("recipientUserId", recipient.userId().value().toString());
    }
    metadata.put("audienceType", event.audienceType().name());
    if (event.targets() != null && !event.targets().isEmpty()) {
      metadata.put(
          "targets",
          event.targets().stream()
              .map(target -> target.actorType().name() + ":" + target.actorId())
              .toList());
    }
    metadata.put("severity", event.severity().name());
    metadata.put("category", event.category().name());
    metadata.put("kind", event.kind().name());
    metadata.put("subject", event.title());
    metadata.put("title", event.title());
    metadata.put("body", event.message() == null ? "" : event.message());
    metadata.put("message", event.message() == null ? "" : event.message());
    metadata.put("priority", priority(event).name());
    if (event.actionUrl() != null && !event.actionUrl().isBlank()) {
      metadata.put("actionUrl", event.actionUrl());
    }

    return new SendOutboundMessageRequest(
        "NOTIFICATION_PUBLISHED", communicationChannel, recipient, Locale.FRENCH, metadata);
  }

  private CommunicationChannel communicationChannel(NotificationDeliveryChannel channel) {
    return switch (channel) {
      case EMAIL -> CommunicationChannel.EMAIL;
      case SMS -> CommunicationChannel.SMS;
      case WHATSAPP -> CommunicationChannel.WHATSAPP;
      case SLACK -> CommunicationChannel.SLACK_INTERNAL;
      case IN_APP -> throw new IllegalArgumentException("IN_APP is not an external channel");
    };
  }

  private List<OutboundRecipient> recipients(
      NotificationPublishedEvent event, CommunicationChannel channel) {
    return switch (channel) {
      case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK ->
          List.of(new OutboundRecipient(event.tenantId(), null, null, DEFAULT_SLACK_CHANNEL_KEY));
      case EMAIL -> recipientsForUsers(event, channel, "email", "to");
      case SMS -> recipientsForUsers(event, channel, "sms", "phone", "to");
      case WHATSAPP -> recipientsForUsers(event, channel, "whatsapp", "phone", "to");
      case PUSH -> List.of();
    };
  }

  private List<OutboundRecipient> recipientsForUsers(
      NotificationPublishedEvent event, CommunicationChannel channel, String... explicitKeys) {
    var recipients = new LinkedHashMap<String, OutboundRecipient>();
    destination(event, explicitKeys)
        .ifPresent(
            value ->
                addRecipient(
                    recipients,
                    new OutboundRecipient(event.tenantId(), null, value, null),
                    channel));

    for (var contact : contactsFor(event)) {
      var destination = destination(contact, channel);
      if (destination == null) {
        continue;
      }
      addRecipient(
          recipients,
          new OutboundRecipient(contact.tenantId(), contact.userId(), destination, null),
          channel);
    }

    return new ArrayList<>(recipients.values());
  }

  private List<NotificationRecipientContact> contactsFor(NotificationPublishedEvent event) {
    if (event.audienceType() == com.tchalanet.server.platform.notification.api.model.NotificationAudienceType.SPECIFIC_ACTORS) {
      return recipientResolvers.stream()
          .filter(resolver -> event.targets() != null && event.targets().stream().anyMatch(target -> resolver.supportsTarget(target.actorType())))
          .flatMap(resolver -> resolver.resolveTargets(event.tenantId(), event.targets()).stream())
          .toList();
    }
    return recipientResolvers.stream()
        .filter(resolver -> resolver.supportsAudience(event.audienceType()))
        .flatMap(resolver -> resolver.resolveAudience(event.tenantId(), event.audienceType()).stream())
        .toList();
  }

  private String destination(NotificationRecipientContact contact, CommunicationChannel channel) {
    return switch (channel) {
      case EMAIL -> normalize(contact.email());
      case SMS, WHATSAPP -> normalize(contact.phone());
      case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK, PUSH -> null;
    };
  }

  private void addRecipient(
      LinkedHashMap<String, OutboundRecipient> recipients,
      OutboundRecipient recipient,
      CommunicationChannel channel) {
    var key =
        switch (channel) {
          case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK -> "slack:" + recipient.channelKey();
          case EMAIL, SMS, WHATSAPP, PUSH -> recipient.to() == null ? null : channel.name() + ":" + recipient.to();
        };
    if (key != null && !key.isBlank()) {
      recipients.putIfAbsent(key, recipient);
    }
  }

  private java.util.Optional<String> destination(NotificationPublishedEvent event, String... keys) {
    if (event.payload() == null || !event.payload().isObject()) {
      return java.util.Optional.empty();
    }
    for (var key : keys) {
      var node = event.payload().get(key);
      if (node != null && !node.isNull() && !node.asText().isBlank()) {
        return java.util.Optional.of(node.asText().trim());
      }
    }
    return java.util.Optional.empty();
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private MessagePriority priority(NotificationPublishedEvent event) {
    return switch (event.severity()) {
      case CRITICAL, ERROR -> MessagePriority.HIGH;
      case WARNING, INFO -> MessagePriority.NORMAL;
    };
  }

  private String correlationKey(
      NotificationPublishedEvent event, CommunicationChannel channel, OutboundRecipient recipient) {
    var publication =
        event.publicationId() == null
            ? event.notificationId().value().toString()
            : event.publicationId().value().toString();
    var destination =
        switch (channel) {
          case SLACK, SLACK_INTERNAL, SLACK_TENANT_WEBHOOK -> recipient.channelKey();
          case EMAIL, SMS, WHATSAPP, PUSH -> recipient.to();
        };
    return String.join(":", "notification", publication, channel.name(), destination == null ? "default" : destination);
  }
}
