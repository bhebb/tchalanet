package com.tchalanet.server.platform.notification.internal.mapper;

import com.tchalanet.server.common.communication.api.CommunicationChannel;
import com.tchalanet.server.common.communication.api.OutboundMessageRequest;
import com.tchalanet.server.common.communication.api.OutboundRecipient;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.api.model.SendNotificationCommand;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OutboundMessageMapper {

    public Optional<OutboundMessageRequest> toOutbound(
        SendNotificationCommand command,
        NotificationRecipient recipient,
        String idempotencyKey
    ) {
        return mapExternalChannel(recipient.channel())
            .map(channel -> new OutboundMessageRequest(
                command.type().name(),
                channel,
                new OutboundRecipient(
                    recipient.tenantId(),
                    recipient.userId(),
                    recipient.to(),
                    recipient.channelKey()
                ),
                command.locale(),
                buildMetadata(command, recipient, idempotencyKey)
            ));
    }

    private Optional<CommunicationChannel> mapExternalChannel(NotificationChannel channel) {
        return switch (channel) {
            case SLACK -> Optional.of(CommunicationChannel.SLACK);
            case EMAIL -> Optional.of(CommunicationChannel.EMAIL);
            case SMS -> Optional.of(CommunicationChannel.SMS);
            case WHATSAPP -> Optional.of(CommunicationChannel.WHATSAPP);
            case WEB, PUSH -> Optional.empty();
        };
    }

    private Map<String, Object> buildMetadata(
        SendNotificationCommand command,
        NotificationRecipient recipient,
        String idempotencyKey
    ) {
        var metadata = new HashMap<String, Object>();

        if (command.context() != null) {
            metadata.putAll(command.context());
        }

        metadata.put("title", command.title());
        metadata.put("message", command.message());
        metadata.put("severity", command.severity().name());
        metadata.put("idempotencyKey", idempotencyKey);

        if (recipient.channelKey() != null) {
            metadata.put("channelKey", recipient.channelKey());
        }
        if (recipient.to() != null) {
            metadata.put("to", recipient.to());
        }
        if (recipient.tenantId() != null) {
            metadata.put("tenantId", recipient.tenantId().value().toString());
        }
        if (recipient.userId() != null) {
            metadata.put("userId", recipient.userId().value().toString());
        }

        if (command.reason() != null) {
            metadata.put("reason", command.reason());
        }

        return metadata;
    }
}

