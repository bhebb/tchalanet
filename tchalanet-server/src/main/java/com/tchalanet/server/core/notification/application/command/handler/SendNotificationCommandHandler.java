package com.tchalanet.server.core.notification.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.notification.NotificationGatewayPort;
import com.tchalanet.server.common.notification.model.SendNotificationPayload;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.NotificationChannel;
import com.tchalanet.server.core.notification.application.command.model.SendNotificationCommand;
import com.tchalanet.server.core.notification.application.command.model.SendNotificationResult;
import com.tchalanet.server.core.notification.application.policy.NotificationPolicy;
import com.tchalanet.server.core.notification.domain.model.NotificationRecipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler pour l'envoi de notifications contrôlées.
 * Valide la politique avant de déléguer au gateway.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class SendNotificationCommandHandler
    implements CommandHandler<SendNotificationCommand, SendNotificationResult> {

    private final NotificationGatewayPort notificationGateway;
    private final NotificationPolicy notificationPolicy;

    @Override
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
                sendToRecipient(command, recipient, idempotencyKey);
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

    private void sendToRecipient(
        SendNotificationCommand command,
        NotificationRecipient recipient,
        String idempotencyKey
    ) {
        var data = buildDataMap(command, recipient, idempotencyKey);
        var notificationChannel = mapToCommonChannel(recipient.channel());

        var payload = new SendNotificationPayload(
            command.type(),
            notificationChannel,
            null, // target is null for ops/technical notifications
            command.locale(),
            data
        );

        notificationGateway.send(payload);
    }

    private Map<String, Object> buildDataMap(
        SendNotificationCommand command,
        NotificationRecipient recipient,
        String idempotencyKey
    ) {
        var data = new HashMap<String, Object>();

        // Add command context
        if (command.context() != null) {
            data.putAll(command.context());
        }

        // Add core fields
        data.put("title", command.title());
        data.put("message", command.message());
        data.put("severity", command.severity().name());
        data.put("idempotencyKey", idempotencyKey);

        // Add recipient-specific fields
        if (recipient.channelKey() != null) {
            data.put("channelKey", recipient.channelKey());
        }
        if (recipient.to() != null) {
            data.put("to", recipient.to());
        }
        if (recipient.tenantId() != null) {
            data.put("tenantId", recipient.tenantId().value().toString());
        }
        if (recipient.userId() != null) {
            data.put("userId", recipient.userId().value().toString());
        }

        if (command.reason() != null) {
            data.put("reason", command.reason());
        }

        return data;
    }

    private NotificationChannel mapToCommonChannel(
        com.tchalanet.server.core.notification.domain.model.NotificationChannel domainChannel
    ) {
        return NotificationChannel.valueOf(domainChannel.name());
    }

    private String generateIdempotencyKey(SendNotificationCommand command) {
        return command.type().name() + "_" + UUID.randomUUID();
    }
}

