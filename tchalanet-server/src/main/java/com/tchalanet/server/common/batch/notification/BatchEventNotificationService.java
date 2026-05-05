package com.tchalanet.server.common.batch.notification;

import com.tchalanet.server.common.notification.NotificationGatewayPort;
import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.common.types.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;

/**
 * Service orchestrateur pour les notifications techniques batch.
 * Délègue les décisions de politique à BatchNotificationPolicy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchEventNotificationService {

    private final NotificationGatewayPort sender;
    private final BatchNotificationPolicy policy;
    private final Clock clock;

    private void sendBatchNotification(BatchNotification notice) {
        if (!policy.shouldSend(notice)) {
            log.trace("Batch notification suppressed by policy: jobKey={} status={}",
                notice.jobKey(), notice.status());
            return;
        }

        sender.send(toPayload(notice));
        log.debug("Batch notification sent: jobKey={} status={} code={}",
            notice.jobKey(), notice.status(), notice.code());
    }

    private SendNotificationPayload toPayload(BatchNotification n) {
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

        return new SendNotificationPayload(
            NotificationType.BATCH_MESSAGE,
            NotificationChannel.SLACK,
            null, // batch notifications have no specific target
            Locale.ENGLISH,
            data
        );
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
