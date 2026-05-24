package com.tchalanet.server.platform.identity.internal.entitlement;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUsageProvider implements UsageProvider {

    private final AppUserJpaRepository repository;

    @Override
    public boolean supports(String usageKey) {
        return UsageKeys.USERS_ACTIVE.equals(usageKey);
    }

    @Override
    public int currentUsage(TenantId tenantId, String usageKey) {
        if (!supports(usageKey)) {
            throw new IllegalArgumentException("Unsupported usageKey: " + usageKey);
        }

        return Math.toIntExact(repository.countActiveTenantUsers(tenantId.value()));
    }
}
