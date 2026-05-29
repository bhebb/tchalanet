package com.tchalanet.server.core.terminal.internal.domain.model.challenge;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class TerminalChallengeDeliveryPolicy {

    private static final Map<TerminalChallengeType, TerminalChallengeChannel> DEFAULT_LIVE_CHANNELS =
        new EnumMap<>(TerminalChallengeType.class);
    private static final Map<TerminalChallengeType, TerminalChallengeChannel> DEFAULT_DEV_CHANNELS =
        new EnumMap<>(TerminalChallengeType.class);
    private static final Map<TerminalChallengeType, TerminalChallengeChannel> DEFAULT_E2E_CHANNELS =
        new EnumMap<>(TerminalChallengeType.class);

    static {
        DEFAULT_LIVE_CHANNELS.put(TerminalChallengeType.POS_PAIRING, TerminalChallengeChannel.QR);
        DEFAULT_LIVE_CHANNELS.put(TerminalChallengeType.MOBILE_OTP, TerminalChallengeChannel.SMS);
        DEFAULT_LIVE_CHANNELS.put(TerminalChallengeType.ADMIN_PAIRING_CODE, TerminalChallengeChannel.ADMIN_MANUAL);

        DEFAULT_DEV_CHANNELS.put(TerminalChallengeType.POS_PAIRING, TerminalChallengeChannel.QR);
        DEFAULT_DEV_CHANNELS.put(TerminalChallengeType.MOBILE_OTP, TerminalChallengeChannel.SLACK);
        DEFAULT_DEV_CHANNELS.put(TerminalChallengeType.ADMIN_PAIRING_CODE, TerminalChallengeChannel.ADMIN_MANUAL);

        DEFAULT_E2E_CHANNELS.put(TerminalChallengeType.POS_PAIRING, TerminalChallengeChannel.TEST_CAPTURE);
        DEFAULT_E2E_CHANNELS.put(TerminalChallengeType.MOBILE_OTP, TerminalChallengeChannel.TEST_CAPTURE);
        DEFAULT_E2E_CHANNELS.put(TerminalChallengeType.ADMIN_PAIRING_CODE, TerminalChallengeChannel.TEST_CAPTURE);
    }

    private TerminalChallengeDeliveryPolicy() {
    }

    public static TerminalChallengeChannel defaultChannel(
        TerminalChallengeType type,
        TerminalChallengeDeliveryMode mode
    ) {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(mode, "mode is required");

        var channels = switch (mode) {
            case LIVE -> DEFAULT_LIVE_CHANNELS;
            case DEV -> DEFAULT_DEV_CHANNELS;
            case E2E -> DEFAULT_E2E_CHANNELS;
        };

        return channels.get(type);
    }

    public static boolean productionSafe(TerminalChallengeChannel channel) {
        return channel != TerminalChallengeChannel.TEST_CAPTURE && channel != TerminalChallengeChannel.SLACK;
    }
}
