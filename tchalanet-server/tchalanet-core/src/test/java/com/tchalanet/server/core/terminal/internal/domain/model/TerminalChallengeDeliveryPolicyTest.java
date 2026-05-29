package com.tchalanet.server.core.terminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeChannel;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDeliveryMode;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDeliveryPolicy;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import org.junit.jupiter.api.Test;

class TerminalChallengeDeliveryPolicyTest {

    @Test
    void liveMobileOtpDefaultsToSms() {
        assertThat(TerminalChallengeDeliveryPolicy.defaultChannel(
            TerminalChallengeType.MOBILE_OTP,
            TerminalChallengeDeliveryMode.LIVE
        )).isEqualTo(TerminalChallengeChannel.SMS);
    }

    @Test
    void devMobileOtpDefaultsToSlackToAvoidSmsCost() {
        assertThat(TerminalChallengeDeliveryPolicy.defaultChannel(
            TerminalChallengeType.MOBILE_OTP,
            TerminalChallengeDeliveryMode.DEV
        )).isEqualTo(TerminalChallengeChannel.SLACK);
    }

    @Test
    void e2eAlwaysUsesTestCapture() {
        assertThat(TerminalChallengeDeliveryPolicy.defaultChannel(
            TerminalChallengeType.POS_PAIRING,
            TerminalChallengeDeliveryMode.E2E
        )).isEqualTo(TerminalChallengeChannel.TEST_CAPTURE);
        assertThat(TerminalChallengeDeliveryPolicy.defaultChannel(
            TerminalChallengeType.MOBILE_OTP,
            TerminalChallengeDeliveryMode.E2E
        )).isEqualTo(TerminalChallengeChannel.TEST_CAPTURE);
        assertThat(TerminalChallengeDeliveryPolicy.defaultChannel(
            TerminalChallengeType.ADMIN_PAIRING_CODE,
            TerminalChallengeDeliveryMode.E2E
        )).isEqualTo(TerminalChallengeChannel.TEST_CAPTURE);
    }

    @Test
    void testCaptureAndSlackAreNotProductionSafe() {
        assertThat(TerminalChallengeDeliveryPolicy.productionSafe(TerminalChallengeChannel.TEST_CAPTURE)).isFalse();
        assertThat(TerminalChallengeDeliveryPolicy.productionSafe(TerminalChallengeChannel.SLACK)).isFalse();
        assertThat(TerminalChallengeDeliveryPolicy.productionSafe(TerminalChallengeChannel.SMS)).isTrue();
        assertThat(TerminalChallengeDeliveryPolicy.productionSafe(TerminalChallengeChannel.EMAIL)).isTrue();
    }
}
