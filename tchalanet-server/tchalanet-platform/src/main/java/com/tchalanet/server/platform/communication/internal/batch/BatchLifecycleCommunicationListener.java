package com.tchalanet.server.platform.communication.internal.batch;

import com.tchalanet.server.common.job.lifecycle.JobLifecycleEvent;
import com.tchalanet.server.common.job.lifecycle.JobLifecycleStatus;
import com.tchalanet.server.common.job.context.JobContextBinder;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.MessagePriority;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchLifecycleCommunicationListener {

  private static final String TEMPLATE_KEY = "batch.lifecycle.slack";
  private static final String DEFAULT_SLACK_CHANNEL_KEY = "batch-draws";

  private final CommunicationApi communicationApi;
  private final BatchCommunicationPolicy policy;
  private final JobContextBinder jobContextBinder;

  @EventListener
  public void on(JobLifecycleEvent event) {
    if (!policy.shouldSend(event)) {
      log.trace(
          "batch.communication.suppressed jobKey={} status={} code={}",
          event.jobKey(),
          event.status(),
          event.code());
      return;
    }

    try {
      bindContext(event);
      communicationApi.enqueue(toRequest(event));
    } catch (Exception e) {
      log.warn(
          "batch.communication.enqueue.failed jobKey={} status={} code={}",
          event.jobKey(),
          event.status(),
          event.code(),
          e);
    } finally {
      jobContextBinder.clear();
    }
  }

  private void bindContext(JobLifecycleEvent event) {
    if (event.tenantId() != null) {
      jobContextBinder.bindTenant(event.tenantId(), "batch-lifecycle-communication-listener");
      return;
    }
    jobContextBinder.bindPlatform("batch-lifecycle-communication-listener");
  }

  private SendOutboundMessageRequest toRequest(JobLifecycleEvent event) {
    var metadata = new LinkedHashMap<String, Object>();
    putIfNotNull(metadata, "eventId", event.eventId() == null ? null : event.eventId().value().toString());
    putIfNotNull(metadata, "occurredAt", event.occurredAt());
    putIfNotNull(metadata, "tenantId", event.tenantId() == null ? null : event.tenantId().value().toString());
    putIfNotNull(metadata, "requestId", event.requestId());
    putIfNotNull(metadata, "jobKey", event.jobKey());
    putIfNotNull(metadata, "status", event.status().name());
    putIfNotNull(metadata, "code", event.code());
    putIfNotNull(metadata, "message", event.message());
    putIfNotNull(metadata, "details", event.details());
    metadata.put("templateKey", TEMPLATE_KEY);
    metadata.put("subject", subject(event));
    metadata.put("title", subject(event));
    metadata.put("body", fallbackBody(event));
    metadata.put("message", fallbackBody(event));
    metadata.put("priority", priority(event).name());
    metadata.put("correlationKey", correlationKey(event));

    return new SendOutboundMessageRequest(
        TEMPLATE_KEY,
        CommunicationChannel.SLACK_INTERNAL,
        new OutboundRecipient(event.tenantId(), null, null, DEFAULT_SLACK_CHANNEL_KEY),
        Locale.FRENCH,
        metadata);
  }

  private String subject(JobLifecycleEvent event) {
    return "Batch " + event.status().name() + " - " + event.jobKey();
  }

  private String fallbackBody(JobLifecycleEvent event) {
    var sb = new StringBuilder();
    sb.append("*Job:* ").append(event.jobKey()).append('\n');
    sb.append("*Status:* ").append(event.status()).append('\n');

    if (event.tenantId() != null) {
      sb.append("*Tenant:* ").append(event.tenantId().value()).append('\n');
    }
    if (event.requestId() != null) {
      sb.append("*Request:* ").append(event.requestId()).append('\n');
    }
    if (event.code() != null) {
      sb.append("*Code:* ").append(event.code()).append('\n');
    }
    if (event.message() != null) {
      sb.append("*Message:* ").append(event.message()).append('\n');
    }

    return sb.toString();
  }

  private MessagePriority priority(JobLifecycleEvent event) {
    if (event.status() == JobLifecycleStatus.FAILED) {
      return MessagePriority.HIGH;
    }
    return MessagePriority.NORMAL;
  }

  private String correlationKey(JobLifecycleEvent event) {
    return String.join(
        ":",
        "job-lifecycle",
        event.jobKey(),
        event.tenantId() == null ? "global" : event.tenantId().value().toString(),
        event.status().name().toLowerCase(Locale.ROOT),
        event.code() == null ? "none" : event.code());
  }

  private static void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
    if (value != null) {
      metadata.put(key, value);
    }
  }
}
