package com.tchalanet.server.core.terminal.internal.domain.model.lifecycle;

import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalCapability;
import java.util.Optional;

public final class TerminalOperationPolicy {

    private TerminalOperationPolicy() {
    }

    public static Optional<TerminalCapability> requiredCapability(TerminalOperation operation) {
        return switch (operation) {
            case SELL_TICKET -> Optional.of(TerminalCapability.SELL_TICKET);
            case SELL_PHONE -> Optional.of(TerminalCapability.SELL_PHONE);
            case PAYOUT_CLAIM -> Optional.of(TerminalCapability.PAYOUT_CLAIM);
            case PRINT_TICKET -> Optional.of(TerminalCapability.PRINT_TICKET);
            case REPRINT_TICKET -> Optional.of(TerminalCapability.REPRINT_TICKET);
            case OFFLINE_GRANT -> Optional.of(TerminalCapability.OFFLINE_SELL);
            case OFFLINE_SYNC -> Optional.of(TerminalCapability.OFFLINE_SYNC);
            case SCAN_TICKET -> Optional.of(TerminalCapability.SCAN_TICKET);
            case CANCEL -> Optional.empty();
        };
    }
}
