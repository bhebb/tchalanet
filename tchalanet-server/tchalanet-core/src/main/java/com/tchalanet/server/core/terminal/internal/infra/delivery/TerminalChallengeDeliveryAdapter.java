package com.tchalanet.server.core.terminal.internal.infra.delivery;

import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalChallengeDeliveryPort;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeChannel;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDelivery;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerminalChallengeDeliveryAdapter implements TerminalChallengeDeliveryPort {

    private static final String MESSAGE_TYPE = "terminal.activation.challenge";
    private static final String DEFAULT_SLACK_CHANNEL_KEY = "terminal-activation";

    private final CommunicationApi communicationApi;
    private final TerminalChallengeTestCaptureStore testCaptureStore;
    private final Clock clock;

    @Override
    public TerminalChallengeDelivery deliver(TerminalActivationChallenge challenge, String clearCode) {
        var deliveredAt = Instant.now(clock);
        var deliveryRef = switch (challenge.channel()) {
            case QR -> "qr:" + challenge.id().value();
            case ADMIN_MANUAL -> "admin-manual:" + challenge.id().value();
            case TEST_CAPTURE -> capture(challenge, clearCode, deliveredAt);
            case SMS, EMAIL, SLACK -> enqueue(challenge, clearCode);
        };

        return new TerminalChallengeDelivery(
            challenge.id(),
            challenge.challengeType(),
            challenge.channel(),
            deliveryRef,
            deliveredAt
        );
    }

    private String capture(TerminalActivationChallenge challenge, String clearCode, Instant capturedAt) {
        testCaptureStore.capture(challenge.id(), clearCode, capturedAt);
        return "test-capture:" + challenge.id().value();
    }

    private String enqueue(TerminalActivationChallenge challenge, String clearCode) {
        var messageId = communicationApi.enqueue(new SendOutboundMessageRequest(
            MESSAGE_TYPE,
            communicationChannel(challenge.channel()),
            recipient(challenge.channel(), challenge),
            Locale.FRENCH,
            metadata(challenge, clearCode)
        ));
        return messageId == null ? "communication:pending" : "communication:" + messageId.value();
    }

    private static CommunicationChannel communicationChannel(TerminalChallengeChannel channel) {
        return switch (channel) {
            case SMS -> CommunicationChannel.SMS;
            case EMAIL -> CommunicationChannel.EMAIL;
            case SLACK -> CommunicationChannel.SLACK;
            case QR, ADMIN_MANUAL, TEST_CAPTURE -> throw new IllegalArgumentException(
                "Channel does not use platform.communication: " + channel
            );
        };
    }

    private static OutboundRecipient recipient(TerminalChallengeChannel channel, TerminalActivationChallenge challenge) {
        if (channel == TerminalChallengeChannel.SLACK) {
            return OutboundRecipient.slack(DEFAULT_SLACK_CHANNEL_KEY);
        }
        return new OutboundRecipient(challenge.tenantId(), challenge.userId(), null, null);
    }

    private static Map<String, Object> metadata(TerminalActivationChallenge challenge, String clearCode) {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("templateKey", MESSAGE_TYPE);
        metadata.put("subject", "Terminal activation");
        metadata.put("body", "Terminal activation code: " + clearCode);
        metadata.put("challengeId", challenge.id().value().toString());
        metadata.put("terminalId", challenge.terminalId().value().toString());
        metadata.put("challengeType", challenge.challengeType().name());
        metadata.put("correlationKey", MESSAGE_TYPE + ":" + challenge.id().value());
        metadata.put("priority", "HIGH");
        return Map.copyOf(metadata);
    }
}
