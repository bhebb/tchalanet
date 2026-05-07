package com.tchalanet.server.core.notification.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.communication.api.OutboundMessageGateway;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.notification.application.command.model.SendNotificationCommand;
import com.tchalanet.server.core.notification.application.command.model.SendNotificationResult;
import com.tchalanet.server.core.notification.application.mapper.OutboundMessageMapper;
import com.tchalanet.server.core.notification.application.policy.NotificationPolicy;
import com.tchalanet.server.core.notification.domain.model.NotificationRecipient;
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

    private final OutboundMessageGateway outboundMessageGateway;
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
            .ifPresent(outboundMessageGateway::send);
    }

    private String generateIdempotencyKey(SendNotificationCommand command) {
        return command.type().name() + "_" + UUID.randomUUID();
    }
}
