package com.tchalanet.server.core.offlinesync.application.port.out;

import com.tchalanet.server.common.types.id.TerminalId;

public interface TerminalOfflineCapabilityPort {
  boolean isOfflineSyncEnabled(TerminalId terminalId);
}

