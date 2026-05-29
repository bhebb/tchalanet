package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerminalUsageProvider implements UsageProvider {

    private final TerminalReaderPort terminalReaderPort;

    @Override
    public boolean supports(String usageKey) {
        return UsageKeys.TERMINALS_ACTIVE.equals(usageKey);
    }

    @Override
    public int currentUsage(TenantId tenantId, String usageKey) {
        if (!supports(usageKey)) {
            throw new IllegalArgumentException("Unsupported usageKey: " + usageKey);
        }

        return terminalReaderPort.countActiveByTenant(tenantId);
    }
}
