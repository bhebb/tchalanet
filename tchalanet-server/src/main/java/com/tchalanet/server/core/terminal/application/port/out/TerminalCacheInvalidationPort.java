package com.tchalanet.server.core.terminal.application.port.out;

import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public interface TerminalCacheInvalidationPort {

    void evictTerminal(TerminalId terminalId);

    void evictCurrentForUser(UserId userId);

    void evictTerminalAndUser(TerminalId terminalId, UserId userId);

    void evictTerminalLists();
}
