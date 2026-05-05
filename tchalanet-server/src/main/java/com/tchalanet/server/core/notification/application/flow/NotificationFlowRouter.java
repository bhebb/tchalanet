package com.tchalanet.server.core.notification.application.flow;

import com.tchalanet.server.common.types.enums.NotificationType;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.domain.event.DrawSettledEvent;
import com.tchalanet.server.core.drawresult.domain.event.DrawResultIngestedEvent;
import com.tchalanet.server.core.notification.application.command.model.SendNotificationCommand;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationRecipient;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.infra.config.NotificationFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Route les événements de domaine vers les notifications appropriées.
 * Applique les règles de filtrage et de contrôle du bruit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationFlowRouter {

    private static final String DEFAULT_SLACK_CHANNEL = "batch-draws";
    private static final String DEFAULT_EMAIL = "dev@tchalanet.com";

    private final NotificationFlowProperties flowProperties;

    /**
     * Route un événement DrawResultIngestedEvent (global draw result fetched).
     */
    public List<SendNotificationCommand> routeDrawResultIngested(DrawResultIngestedEvent event) {
        var props = flowProperties.drawResults();
        if (!props.enabled()) {
            log.debug("Draw results notifications disabled");
            return List.of();
        }

        var slotKey = event.resultSlotKey();
        if (!isWatchedSlot(slotKey, props.watchedSlots())) {
            log.trace("Slot {} not watched, skipping notification", slotKey);
            return List.of();
        }

        var commands = new ArrayList<SendNotificationCommand>();

        if (props.slackEnabled()) {
            commands.add(buildSlackDrawResultIngested(event, slotKey));
        }

        if (props.emailDetailEnabled()) {
            commands.add(buildEmailDrawResultIngested(event, slotKey));
        }

        return commands;
    }

    /**
     * Route un événement DrawResultAppliedEvent (result applied to tenant draw).
     */
    public List<SendNotificationCommand> routeDrawResultApplied(DrawResultAppliedEvent event) {
        var props = flowProperties.apply();
        if (!props.enabled()) {
            log.debug("Apply notifications disabled");
            return List.of();
        }

        if (!props.slackInfoEnabled()) {
            log.trace("Apply Slack INFO disabled");
            return List.of();
        }

        return List.of(buildSlackDrawResultApplied(event));
    }

    /**
     * Route un événement DrawSettledEvent (settlement completed).
     */
    public List<SendNotificationCommand> routeDrawSettled(DrawSettledEvent event) {
        var props = flowProperties.settlement();
        if (!props.enabled()) {
            log.debug("Settlement notifications disabled");
            return List.of();
        }

        if (!props.slackInfoEnabled()) {
            log.trace("Settlement Slack INFO disabled");
            return List.of();
        }

        return List.of(buildSlackDrawSettled(event));
    }

    // ==================== Builder Methods ====================

    private SendNotificationCommand buildSlackDrawResultIngested(
        DrawResultIngestedEvent event,
        String slotKey
    ) {
        var context = Map.<String, Object>of(
            "eventId", event.eventId().value().toString(),
            "resultSlotKey", slotKey,
            "resultSlotId", event.resultSlotId().value().toString(),
            "drawResultId", event.drawResultId().value().toString(),
            "drawDate", event.drawDate().toString(),
            "occurredAt", event.drawResultOccurredAt().toString()
        );

        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            DEFAULT_SLACK_CHANNEL,
            null,
            null
        );

        return new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Draw Result Ingested: " + slotKey,
            "✅ Draw result ingested for " + slotKey + " on " + event.drawDate(),
            context,
            null,
            "draw-result-ingested"
        );
    }

    private SendNotificationCommand buildEmailDrawResultIngested(
        DrawResultIngestedEvent event,
        String slotKey
    ) {
        var context = Map.<String, Object>of(
            "eventId", event.eventId().value().toString(),
            "resultSlotKey", slotKey,
            "resultSlotId", event.resultSlotId().value().toString(),
            "drawResultId", event.drawResultId().value().toString(),
            "drawDate", event.drawDate().toString(),
            "occurredAt", event.drawResultOccurredAt().toString()
        );

        var recipient = new NotificationRecipient(
            NotificationChannel.EMAIL,
            DEFAULT_EMAIL,
            null,
            null,
            null
        );

        var message = buildDrawResultDetailedMessage(slotKey, event);

        return new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Draw Result Detail: " + slotKey + " - " + event.drawDate(),
            message,
            context,
            null,
            "draw-result-detail"
        );
    }

    private SendNotificationCommand buildSlackDrawResultApplied(DrawResultAppliedEvent event) {
        var context = Map.<String, Object>of(
            "eventId", event.eventId().value().toString(),
            "drawId", event.drawId().value().toString(),
            "drawResultId", event.drawResultId().value().toString(),
            "drawDate", event.drawDate().toString(),
            "resultSlotId", event.resultSlotId().value().toString()
        );

        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            DEFAULT_SLACK_CHANNEL,
            null,
            null
        );

        return new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Draw Result Applied",
            String.format("✅ Draw result applied for date %s (drawId=%s)",
                event.drawDate(), event.drawId().value()),
            context,
            null,
            "draw-result-applied"
        );
    }

    private SendNotificationCommand buildSlackDrawSettled(DrawSettledEvent event) {
        var context = Map.<String, Object>of(
            "eventId", event.eventId().value().toString(),
            "drawId", event.drawId().value().toString(),
            "drawResultId", event.drawResultId().value().toString(),
            "drawDate", event.drawDate().toString(),
            "resultSlotId", event.resultSlotId().value().toString(),
            "scheduledAt", event.scheduledAt().toString()
        );

        var recipient = new NotificationRecipient(
            NotificationChannel.SLACK,
            null,
            DEFAULT_SLACK_CHANNEL,
            null,
            null
        );

        return new SendNotificationCommand(
            NotificationType.SYSTEM_MESSAGE,
            NotificationSeverity.INFO,
            List.of(recipient),
            Locale.ENGLISH,
            "Draw Settled",
            String.format("💰 Draw settled for date %s (drawId=%s)",
                event.drawDate(), event.drawId().value()),
            context,
            null,
            "draw-settled"
        );
    }

    // ==================== Helper Methods ====================

    private boolean isWatchedSlot(String slotKey, List<String> watchedSlots) {
        if (watchedSlots == null || watchedSlots.isEmpty()) {
            return false;
        }
        return watchedSlots.contains(slotKey);
    }

    private String buildDrawResultDetailedMessage(String slotKey, DrawResultIngestedEvent event) {
        var sb = new StringBuilder();
        sb.append("=== Draw Result Detail ===\n\n");
        sb.append("Slot: ").append(slotKey).append("\n");
        sb.append("Draw Date: ").append(event.drawDate()).append("\n");
        sb.append("Occurred At: ").append(event.drawResultOccurredAt()).append("\n");
        sb.append("Result Slot ID: ").append(event.resultSlotId().value()).append("\n");
        sb.append("Draw Result ID: ").append(event.drawResultId().value()).append("\n");
        sb.append("\n");
        sb.append("TODO: Add source pick3/pick4 when available in event\n");
        sb.append("TODO: Add source URL when available\n");
        sb.append("TODO: Add Haiti projection details when available\n");
        sb.append("\n");
        sb.append("Event ID: ").append(event.eventId().value()).append("\n");
        return sb.toString();
    }
}

