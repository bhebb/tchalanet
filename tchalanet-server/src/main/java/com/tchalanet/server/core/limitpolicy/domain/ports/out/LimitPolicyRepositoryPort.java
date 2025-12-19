package com.tchalanet.server.core.limitpolicy.domain.ports.out;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;

import java.util.List;
import java.util.UUID;

public interface LimitPolicyRepositoryPort {
    List<LimitPolicy> findActivePolicies(UUID tenantId);
}
