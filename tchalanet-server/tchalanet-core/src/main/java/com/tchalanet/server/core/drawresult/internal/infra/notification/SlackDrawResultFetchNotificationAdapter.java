package com.tchalanet.server.core.drawresult.internal.infra.notification;

import com.tchalanet.server.core.drawresult.internal.application.port.out.notification.DrawResultFetchNotificationPort;
import com.tchalanet.server.core.drawresult.internal.infra.config.DrawResultsProperties;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.MessagePriority;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tch.draw.results.notifications.slack",
    name = "enabled",
    havingValue = "true")
public class SlackDrawResultFetchNotificationAdapter implements DrawResultFetchNotificationPort {

  private static final String TEMPLATE_KEY = "drawresult.fetch.completed.slack";
  private static final String DEFAULT_SLACK_CHANNEL_KEY = "general";

  private final CommunicationApi communicationApi;
  private final DrawResultsProperties drawResultsProperties;

  @Override
  public void notifyFetched(DrawResultFetchNotification n) {
    try {
      communicationApi.enqueue(buildSendOutboundMessageRequest(n));
    } catch (Exception ex) {
      // Notifications must never break result fetching.
      log.warn(
          "draw-results.fetch slack_notification_failed provider={} slot={} drawDate={} err={}",
          n.provider(),
          n.slotKey(),
          n.drawDate(),
          ex.getMessage(),
          ex);
    }
  }

  private SendOutboundMessageRequest buildSendOutboundMessageRequest(DrawResultFetchNotification n) {
    var metadata = new LinkedHashMap<String, Object>();
    metadata.put("templateKey", TEMPLATE_KEY);
    metadata.put("priority", resolvePriority().name());
    metadata.put("provider", safe(n.provider()));
    metadata.put("slotKey", safe(n.slotKey()));
    metadata.put("drawDate", String.valueOf(n.drawDate()));
    metadata.put("occurredAt", String.valueOf(n.occurredAt()));
    metadata.put("status", safe(n.status()));
    metadata.put("quality", safe(n.quality()));
    metadata.put("itemCount", String.valueOf(n.itemCount()));
    metadata.put("externalGameCodes", String.join(",", n.externalGameCodes()));
    metadata.put("sourceResult", n.metadata().getOrDefault("sourceResult", ""));
    metadata.put("haitiProjection", n.metadata().getOrDefault("haitiProjection", ""));
    metadata.put("flags", n.metadata().getOrDefault("flags", ""));
    metadata.put("sourceHash", n.metadata().getOrDefault("sourceHash", ""));
    metadata.put("subject", "Draw result fetched - " + safe(n.slotKey()) + " - " + String.valueOf(n.drawDate()));
    metadata.put("title", "Draw result fetched - " + safe(n.slotKey()));
    metadata.put("body", buildFallbackBody(n));
    metadata.put("message", buildFallbackBody(n));
    metadata.put("correlationKey", correlationKey(n));

    return new SendOutboundMessageRequest(
        TEMPLATE_KEY,
        CommunicationChannel.SLACK_INTERNAL,
        OutboundRecipient.slack(resolveSlackChannel()),
        Locale.FRENCH,
        metadata);
  }

  private String buildFallbackBody(DrawResultFetchNotification n) {
    return "*Provider:* "
        + safe(n.provider())
        + "\n*Slot:* "
        + safe(n.slotKey())
        + "\n*Date:* "
        + n.drawDate()
        + "\n*Occurred At:* "
        + n.occurredAt()
        + "\n*Status:* "
        + safe(n.status())
        + "\n*Quality:* "
        + safe(n.quality())
        + "\n*External Games:* "
        + String.join(", ", n.externalGameCodes())
        + "\n*External Count:* "
        + n.itemCount()
        + "\n*Haiti Projection:* "
        + n.metadata().getOrDefault("haitiProjection", "");
  }

  private String correlationKey(DrawResultFetchNotification n) {
    return String.join(
        ":",
        "drawresult-fetch",
        safe(n.provider()),
        safe(n.slotKey()),
        n.drawDate() == null ? "nodate" : n.drawDate().toString());
  }

  private String resolveSlackChannel() {
    var configured = drawResultsProperties.getNotifications().getSlack().getChannel();
    if (configured == null || configured.isBlank()) {
      return DEFAULT_SLACK_CHANNEL_KEY;
    }
    return configured.trim();
  }

  private MessagePriority resolvePriority() {
    var configured = drawResultsProperties.getNotifications().getSlack().getPriority();
    if (configured == null || configured.isBlank()) {
      return MessagePriority.LOW;
    }
    try {
      return MessagePriority.valueOf(configured.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      log.warn(
          "draw-results.fetch invalid_slack_priority configured={} allowed={}",
          configured,
          List.of(MessagePriority.values()));
      return MessagePriority.LOW;
    }
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }
}
