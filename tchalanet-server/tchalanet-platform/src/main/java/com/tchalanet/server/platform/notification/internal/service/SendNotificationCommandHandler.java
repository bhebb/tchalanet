package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.api.model.SendNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;
import com.tchalanet.server.platform.notification.internal.mapper.OutboundMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    private final CommunicationApi communicationApi;
    private final OutboundMessageMapper outboundMessageMapper;
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
        outboundMessageMapper.toOutbound(command, recipient, idempotencyKey)
            .ifPresent(communicationApi::enqueue);
    }

    private String generateIdempotencyKey(SendNotificationCommand command) {
        return command.type().name() + "_" + UUID.randomUUID();
    }
}
