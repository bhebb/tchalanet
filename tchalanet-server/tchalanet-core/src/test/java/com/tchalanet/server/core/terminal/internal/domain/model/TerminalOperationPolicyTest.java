package com.tchalanet.server.core.terminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.terminal.api.query.TerminalOperation;
import com.tchalanet.server.core.terminal.internal.domain.model.lifecycle.TerminalOperationPolicy;
import org.junit.jupiter.api.Test;

class TerminalOperationPolicyTest {

    @Test
    void mapsBusinessOperationToRequiredTerminalCapability() {
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.SELL_TICKET))
            .contains(TerminalCapability.SELL_TICKET);
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.SELL_PHONE))
            .contains(TerminalCapability.SELL_PHONE);
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.PAYOUT_CLAIM))
            .contains(TerminalCapability.PAYOUT_CLAIM);
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.PRINT_TICKET))
            .contains(TerminalCapability.PRINT_TICKET);
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.REPRINT_TICKET))
            .contains(TerminalCapability.REPRINT_TICKET);
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.OFFLINE_GRANT))
            .contains(TerminalCapability.OFFLINE_SELL);
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.OFFLINE_SYNC))
            .contains(TerminalCapability.OFFLINE_SYNC);
        assertThat(TerminalOperationPolicy.requiredCapability(TerminalOperation.SCAN_TICKET))
            .contains(TerminalCapability.SCAN_TICKET);
    }
}
