package com.tchalanet.server.core.drawresult.internal.infra.notification;

import com.tchalanet.server.core.drawresult.internal.application.port.out.notification.DrawResultFetchNotificationPort;
import com.tchalanet.server.core.drawresult.internal.infra.config.DrawResultsProperties;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "tch.draw.results.notifications.slack",
    name = "enabled",
    havingValue = "true")
@RequiredArgsConstructor
public class SlackDrawResultFetchNotificationAdapter implements DrawResultFetchNotificationPort {

    private static final String TEMPLATE_KEY = "drawresult.fetch.completed.slack";
    private static final String DEFAULT_SLACK_CHANNEL_KEY = "batch-draws";

    private final CommunicationApi communicationApi;

    private static final String SUCCESS_TYPE = "drawresult.fetch.completed.batch.slack";
    private static final String FAILURE_TYPE = "drawresult.fetch.failed.batch.slack";

    private final DrawResultsProperties drawResultsProperties;

    @Override
    public void notifyFetched(DrawResultFetchNotification notification) {
        if (notification == null) {
            return;
        }

        notifyFetchedBatch(List.of(notification));
    }

    @Override
    public void notifyFetchedBatch(List<DrawResultFetchNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        try {
            communicationApi.sendNow(
                slackRequest(
                    SUCCESS_TYPE,
                    "Draw results fetched",
                    buildFetchedBatchMessage(notifications),
                    "drawresult.fetch.completed:%s:%d"
                        .formatted(notifications.getFirst().resultDate(), notifications.size())));

        } catch (Exception e) {
            log.warn(
                "draw-results.fetch slack_batch_notification_failed count={} err={}",
                notifications.size(),
                e.getMessage(),
                e);
        }
    }

    @Override
    public void notifyFetchFailedBatch(DrawResultFetchFailureBatchNotification notification) {
        if (notification == null
            || notification.failures() == null
            || notification.failures().isEmpty()) {
            return;
        }

        try {
            communicationApi.sendNow(
                slackRequest(
                    FAILURE_TYPE,
                    "Draw result fetch failures",
                    buildFetchFailureBatchMessage(notification),
                    "drawresult.fetch.failed:%s:%d"
                        .formatted(notification.baseDate(), notification.totalFailures())));

        } catch (Exception e) {
            log.warn(
                "draw-results.fetch slack_failure_notification_failed failures={} err={}",
                notification.totalFailures(),
                e.getMessage(),
                e);
        }
    }

    private SendOutboundMessageRequest slackRequest(
        String type,
        String title,
        String body,
        String correlationKey) {

        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("title", title);
        metadata.put("subject", title);
        metadata.put("message", body);
        metadata.put("body", body);
        metadata.put("priority", "NORMAL");
        metadata.put("correlationKey", correlationKey);
        metadata.put("slackChannel", DEFAULT_SLACK_CHANNEL_KEY);

        return new SendOutboundMessageRequest(
            type,
            CommunicationChannel.SLACK,
            slackInternalRecipient(),
            Locale.ENGLISH,
            Map.copyOf(metadata));
    }

    private String resolveSlackChannel() {
        var configured = drawResultsProperties.getNotifications().getSlack().getChannel();
        if (configured == null || configured.isBlank()) {
            return DEFAULT_SLACK_CHANNEL_KEY;
        }
        return configured.trim();
    }

    /**
     * Ajuste seulement cette méthode selon la forme exacte de OutboundRecipient.
     * <p>
     * Si OutboundRecipient a un factory, utilise par exemple :
     * return OutboundRecipient.of(SLACK_INTERNAL);
     * <p>
     * Si SlackProvider résout le channel via metadata["slackChannel"], tu peux retourner null.
     */
    private OutboundRecipient slackInternalRecipient() {
        return OutboundRecipient.slack(resolveSlackChannel());
    }

    private String buildFetchedBatchMessage(List<DrawResultFetchNotification> notifications) {
        var first = notifications.getFirst();

        var sb = new StringBuilder();

        sb.append("[INFO] Draw results fetched\n\n");
        sb.append("Date: ").append(first.resultDate()).append('\n');
        sb.append("Results: ").append(notifications.size()).append("\n\n");

        for (var n : notifications) {
            sb.append("- ")
                .append(n.slotKey())
                .append(" • provider=")
                .append(n.provider())
                .append(" • ")
                .append(n.status())
                .append(" • ")
                .append(n.quality())
                .append(" • occurredAt=")
                .append(n.occurredAt());

            if (n.externalGames() != null && !n.externalGames().isEmpty()) {
                sb.append(" • games=").append(String.join(", ", n.externalGames()));
            }

            var projection = n.metadata() == null ? null : n.metadata().get("haitiProjection");
            if (projection != null && !projection.isBlank()) {
                sb.append("\n  Haiti: ").append(projection);
            }

            var sourceResult = n.metadata() == null ? null : n.metadata().get("sourceResult");
            if (sourceResult != null && !sourceResult.isBlank()) {
                sb.append("\n  External: ").append(trimForSlack(sourceResult, 700));
            }

            var sourceHash = n.metadata() == null ? null : n.metadata().get("sourceHash");
            if (sourceHash != null && !sourceHash.isBlank()) {
                sb.append("\n  SourceHash: ").append(sourceHash);
            }

            sb.append('\n');
        }

        return trimForSlack(sb.toString(), 3500);
    }

    private String buildFetchFailureBatchMessage(
        DrawResultFetchFailureBatchNotification notification) {

        var sb = new StringBuilder();

        sb.append("[WARN] Draw result fetch failures\n\n");
        sb.append("Base Date: ").append(notification.baseDate()).append('\n');
        sb.append("Days Back: ").append(notification.daysBack()).append('\n');
        sb.append("Failures: ").append(notification.totalFailures()).append("\n\n");

        for (var f : notification.failures()) {
            sb.append("- ")
                .append(f.slotKey())
                .append(" • provider=")
                .append(f.provider())
                .append(" • date=")
                .append(f.resultDate())
                .append(" • expectedAt=")
                .append(f.expectedOccurredAt())
                .append(" • ")
                .append(f.errorType());

            if (f.message() != null && !f.message().isBlank()) {
                sb.append(": ").append(trimForSlack(f.message(), 240));
            }

            sb.append('\n');
        }

        return trimForSlack(sb.toString(), 3500);
    }

    private static String trimForSlack(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }

        return value.substring(0, max - 3) + "...";
    }
}
