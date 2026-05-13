package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NoopAdminPosSelectionLookup implements AdminPosSelectionLookup {

    @Override
    public Optional<AdminPosSelection> findActive(TenantId tenantId, UserId userId) {
        return Optional.empty();
    }
}
