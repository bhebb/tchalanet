package com.tchalanet.server.platform.notification.internal.batch;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.job.lifecycle.JobLifecycleNotifier;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.platform.notification.api.model.CreateNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.time.Clock;
import java.util.HashMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service orchestrateur pour les notifications techniques batch in-app.
 * Délègue les décisions de politique à BatchNotificationPolicy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchEventNotificationService implements JobLifecycleNotifier {

    private final CommandBus commandBus;
    private final BatchNotificationPolicy policy;
    private final JsonUtils jsonUtils;
    private final Clock clock;

    private void sendBatchNotification(BatchNotification notice) {
        if (!policy.shouldSend(notice)) {
            log.trace("Batch notification suppressed by policy: jobKey={} status={}",
                notice.jobKey(), notice.status());
            return;
        }

        commandBus.execute(toCommand(notice));
        log.debug("Batch notification created: jobKey={} status={} code={}",
            notice.jobKey(), notice.status(), notice.code());
    }

    private CreateNotificationCommand toCommand(BatchNotification n) {
        var data = n.details() == null
            ? new HashMap<String, Object>()
            : new HashMap<>(n.details());

        data.put("requestId", n.requestId());
        data.put("occurredAt", n.occurredAt());
        data.put("tenantId", n.tenantId());
        data.put("jobKey", n.jobKey());
        data.put("status", n.status().name());
        data.put("code", n.code());
        data.put("message", n.message());
        data.put("channelKey", "batch-draws"); // Slack channel key for batch notifications

        // Build title and message for edge
        var title = "Batch " + n.status().name();
        var messageText = buildMessage(n);
        data.put("title", title);
        data.put("message", messageText);
        data.put("severity", n.status() == BatchNotificationStatus.FAILED ? "ERROR" : "WARNING");

        var tenantId = parseTenantId(n.tenantId());
        var audienceType = tenantId == null
            ? NotificationAudienceType.PLATFORM
            : NotificationAudienceType.TENANT;
        var audienceValue = tenantId == null ? "platform" : tenantId.value().toString();

        return new CreateNotificationCommand(
            tenantId,
            "BatchNotification",
            n.jobKey(),
            fingerprint(n),
            audienceType,
            audienceValue,
            n.status() == BatchNotificationStatus.FAILED
                ? NotificationSeverity.ERROR
                : NotificationSeverity.WARNING,
            n.status() == BatchNotificationStatus.FAILED
                ? NotificationKind.SYSTEM_ERROR
                : NotificationKind.WARNING,
            NotificationCategory.BATCH,
            "batch." + n.status().name().toLowerCase(),
            "batch." + n.status().name().toLowerCase(),
            title,
            messageText,
            jsonUtils.toJsonNode(data),
            null,
            null,
            null,
            Set.of(NotificationChannel.WEB)
        );
    }

    private TenantId parseTenantId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TenantId.parse(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring invalid tenant id on batch notification: {}", raw);
            return null;
        }
    }

    private String buildMessage(BatchNotification n) {
        var sb = new StringBuilder();
        sb.append("Job: ").append(n.jobKey());
        if (n.tenantId() != null) {
            sb.append(" | Tenant: ").append(n.tenantId());
        }
        if (n.code() != null) {
            sb.append(" | Code: ").append(n.code());
        }
        if (n.message() != null) {
            sb.append(" | ").append(n.message());
        }
        return sb.toString();
    }

    private String fingerprint(BatchNotification n) {
        return String.join(":",
            "batch",
            n.jobKey(),
            n.tenantId() == null ? "GLOBAL" : n.tenantId(),
            n.status().name(),
            n.code() == null ? "none" : n.code()
        );
    }

    public void started(String jobKey) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.STARTED,
            null,
            null,
            null,
            clock.instant(),
            null
        ));
    }

    public void skipped(String jobKey, String code, String message) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.SKIPPED,
            code,
            message,
            null,
            clock.instant(),
            null
        ));
    }

    public void succeeded(String jobKey) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.SUCCEEDED,
            null,
            null,
            null,
            clock.instant(),
            null
        ));
    }

    public void failed(String jobKey, Throwable e) {
        sendBatchNotification(new BatchNotification(
            jobKey,
            null,
            BatchNotificationStatus.FAILED,
            e.getClass().getSimpleName(),
            e.getMessage(),
            null,
            clock.instant(),
            null
        ));
    }
}
