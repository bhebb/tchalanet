package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.SendNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for controlled in-app notification creation.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class SendNotificationCommandHandler
    implements CommandHandler<SendNotificationCommand, SendNotificationResult> {

    private final NotificationPolicy notificationPolicy;
    private final NotificationWriterPort notificationWriter;
    private final NotificationDeliveryWriterPort deliveryWriter;
    private final IdGenerator idGenerator;
    private final JsonUtils jsonUtils;
    private final Clock clock;

    @Override
    @TchTx
    public SendNotificationResult handle(SendNotificationCommand command) {
        log.debug("Handling SendNotificationCommand: type={}, severity={}, recipients={}",
            command.type(), command.severity(), command.recipients().size());

        // Validate recipients through policy
        notificationPolicy.validateRecipients(command.recipients());

        // Generate idempotency key if not provided
        var idempotencyKey = command.idempotencyKey() != null
            ? command.idempotencyKey()
            : generateIdempotencyKey(command);

        // Send to each recipient
        for (var recipient : command.recipients()) {
            try {
                createInAppNotification(command, recipient, idempotencyKey);
            } catch (Exception e) {
                log.error("Failed to send notification to recipient: channel={}, error={}",
                    recipient.channel(), e.getMessage(), e);
                return SendNotificationResult.failed(
                    "Failed to send notification: " + e.getMessage(),
                    idempotencyKey
                );
            }
        }

        log.info("Notification sent successfully: type={}, idempotencyKey={}",
            command.type(), idempotencyKey);

        return SendNotificationResult.accepted(idempotencyKey);
    }

    private void createInAppNotification(
        SendNotificationCommand command,
        NotificationRecipient recipient,
        String idempotencyKey
    ) {
        var now = clock.instant();
        var notificationId = NotificationId.of(idGenerator.newUuid());
        var tenantId = recipient.tenantId();
        var audienceType = audienceType(recipient);
        var audienceValue = audienceValue(recipient, audienceType);
        var dedupeKey = idempotencyKey + ":" + recipient.channel() + ":" + audienceType + ":" + audienceValue;

        if (notificationWriter.findByDedupeKey(dedupeKey).isPresent()) {
            return;
        }

        var notification = new Notification(
            notificationId,
            tenantId,
            "SendNotificationCommand",
            command.type().name(),
            dedupeKey,
            audienceType,
            audienceValue,
            command.severity(),
            NotificationKind.INFO,
            NotificationCategory.SYSTEM,
            command.type().name(),
            command.type().name(),
            command.title(),
            command.message(),
            jsonUtils.toJsonNode(command.context()),
            new NotificationAction(null, null),
            NotificationStatus.UNREAD,
            null,
            null,
            null,
            now,
            now);

        var saved = notificationWriter.save(notification);
        deliveryWriter.save(new NotificationDelivery(
            NotificationDeliveryId.of(idGenerator.newUuid()),
            tenantId,
            saved.id(),
            normalizeChannel(recipient.channel()),
            audienceValue,
            NotificationDeliveryStatus.PENDING,
            0,
            now,
            null,
            null,
            null,
            null,
            null,
            notification.payload(),
            now,
            now));
    }

    private NotificationChannel normalizeChannel(NotificationChannel channel) {
        return channel == null ? NotificationChannel.WEB : channel;
    }

    private NotificationAudienceType audienceType(NotificationRecipient recipient) {
        if (recipient.userId() != null) {
            return NotificationAudienceType.USER;
        }
        if (recipient.tenantId() != null) {
            return NotificationAudienceType.TENANT;
        }
        return NotificationAudienceType.PLATFORM;
    }

    private String audienceValue(NotificationRecipient recipient, NotificationAudienceType type) {
        return switch (type) {
            case USER -> recipient.userId().value().toString();
            case TENANT -> recipient.tenantId().value().toString();
            case PLATFORM -> "platform";
            case ROLE, OUTLET, TERMINAL -> "unsupported";
        };
    }

    private String generateIdempotencyKey(SendNotificationCommand command) {
        return command.type().name() + "_" + UUID.randomUUID();
    }
}
