package com.tchalanet.server.core.limitpolicy.infra.persistence.repository;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitScope;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitPolicyEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringLimitPolicyJpaRepository extends JpaRepository<LimitPolicyEntity, UUID> {
  List<LimitPolicyEntity> findByTenantIdAndActiveTrue(UUID tenantId);

  List<LimitPolicyEntity> findByTenantIdAndScopeAndTargetAndActiveTrue(
      UUID tenantId, LimitScope scope, String target);
}
