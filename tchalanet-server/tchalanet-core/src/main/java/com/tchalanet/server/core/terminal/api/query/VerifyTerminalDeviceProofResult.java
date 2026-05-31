package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;

public sealed interface VerifyTerminalDeviceProofResult {

    record Trusted(
        TerminalId terminalId,
        TerminalBindingId bindingId,
        OutletId outletId,
        UserId assignedUserId,
        TerminalBindingType bindingType
    ) implements VerifyTerminalDeviceProofResult {}

    record Rejected(String code) implements VerifyTerminalDeviceProofResult {}
}
