package com.tchalanet.server.core.limitpolicy.application.ports.out;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound Port for persisting and retrieving LimitPolicy aggregates. */
public interface LimitPolicyRepositoryPort {
  LimitPolicy save(LimitPolicy policy);

  Optional<LimitPolicy> findById(UUID policyId);

  List<LimitPolicy> findActivePolicies(UUID tenantId);

  List<LimitPolicy> findActivePoliciesByScopeAndTarget(
      UUID tenantId, LimitScope scope, String target);
}
