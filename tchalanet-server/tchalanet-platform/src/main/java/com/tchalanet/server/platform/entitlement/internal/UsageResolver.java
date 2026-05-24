package com.tchalanet.server.platform.entitlement.internal;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsageResolver {

    private final List<UsageProvider> providers;

    public int currentUsage(TenantId tenantId, String usageKey) {
        var matching = providers.stream()
            .filter(p -> p.supports(usageKey))
            .toList();

        if (matching.isEmpty()) {
            throw ProblemRest.internal(
                "No usage provider found for usageKey: " + usageKey
            );
        }

        if (matching.size() > 1) {
            throw ProblemRest.internal(
                "Multiple usage providers found for usageKey: " + usageKey
            );
        }

        return matching.getFirst().currentUsage(tenantId, usageKey);
    }
}
