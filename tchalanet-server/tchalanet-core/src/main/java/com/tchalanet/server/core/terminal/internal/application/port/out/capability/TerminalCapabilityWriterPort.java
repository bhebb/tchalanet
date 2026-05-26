package com.tchalanet.server.core.terminal.internal.application.port.out.capability;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalCapability;
import java.util.Set;

public interface TerminalCapabilityWriterPort {

    void replace(TenantId tenantId, TerminalId terminalId, Set<TerminalCapability> capabilities);
}
