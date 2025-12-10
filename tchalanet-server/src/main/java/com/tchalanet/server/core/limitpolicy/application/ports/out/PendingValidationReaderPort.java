package com.tchalanet.server.core.limitpolicy.application.ports.out;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;

import java.util.List;
import java.util.UUID;

public interface PendingValidationReaderPort {
    List<LimitPolicy> findPendingForTenant(UUID tenantId, UUID approverId);
}
