package com.tchalanet.server.core.terminal.internal.application.port.out;

import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;

import java.time.Instant;

public interface TerminalWriterPort {

    Terminal save(Terminal terminal);

    void setSalesBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy);

    void setPayoutBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy);

    void setOfflineBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy);
}
