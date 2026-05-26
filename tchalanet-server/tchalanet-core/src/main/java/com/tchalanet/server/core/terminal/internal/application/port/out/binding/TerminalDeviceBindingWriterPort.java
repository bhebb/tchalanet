package com.tchalanet.server.core.terminal.internal.application.port.out.binding;

import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;

public interface TerminalDeviceBindingWriterPort {

    TerminalDeviceBinding save(TerminalDeviceBinding binding);
}
