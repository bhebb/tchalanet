package com.tchalanet.server.core.terminal.internal.infra.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeChannel;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.MessageId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalChallengeDeliveryAdapterTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final TerminalId TERMINAL_ID = TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final UserId USER_ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final TerminalActivationChallengeId CHALLENGE_ID =
        TerminalActivationChallengeId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final MessageId MESSAGE_ID =
        MessageId.of(UUID.fromString("00000000-0000-0000-0000-000000000005"));
    private static final Instant NOW = Instant.parse("2026-05-26T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void testCaptureStoresClearCodeWithoutCommunication() {
        var communication = new CapturingCommunicationApi();
        var captureStore = new TerminalChallengeTestCaptureStore();
        var adapter = new TerminalChallengeDeliveryAdapter(communication, captureStore, CLOCK);

        var delivery = adapter.deliver(challenge(TerminalChallengeChannel.TEST_CAPTURE), "123456");

        assertThat(delivery.deliveryRef()).isEqualTo("test-capture:" + CHALLENGE_ID.value());
        assertThat(captureStore.find(CHALLENGE_ID)).hasValueSatisfying(captured ->
            assertThat(captured.clearCode()).isEqualTo("123456")
        );
        assertThat(communication.request).isNull();
    }

    @Test
    void smsChallengeEnqueuesCommunicationRequest() {
        var communication = new CapturingCommunicationApi();
        var adapter = new TerminalChallengeDeliveryAdapter(
            communication,
            new TerminalChallengeTestCaptureStore(),
            CLOCK
        );

        var delivery = adapter.deliver(challenge(TerminalChallengeChannel.SMS), "123456");

        assertThat(delivery.deliveryRef()).isEqualTo("communication:" + MESSAGE_ID.value());
        assertThat(communication.request.channel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(communication.request.metadata()).containsEntry("challengeId", CHALLENGE_ID.value().toString());
        assertThat(communication.request.metadata().get("body").toString()).contains("123456");
    }

    private static TerminalActivationChallenge challenge(TerminalChallengeChannel channel) {
        return TerminalActivationChallenge.pending(
            CHALLENGE_ID,
            TENANT_ID,
            TERMINAL_ID,
            USER_ID,
            TerminalChallengeType.MOBILE_OTP,
            channel,
            "hash",
            NOW,
            NOW.plusSeconds(600),
            3
        );
    }

    private static final class CapturingCommunicationApi implements CommunicationApi {
        private SendOutboundMessageRequest request;

        @Override
        public MessageId enqueue(SendOutboundMessageRequest request) {
            this.request = request;
            return MESSAGE_ID;
        }

        @Override
        public SendOutboundMessageResult sendNow(SendOutboundMessageRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
