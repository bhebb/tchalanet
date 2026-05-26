package com.tchalanet.server.core.terminal.internal.application.port.out.binding;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import java.util.List;

public interface TerminalDeviceBindingReaderPort {

    List<TerminalDeviceBinding> findActiveByTerminal(TenantId tenantId, TerminalId terminalId);
}
