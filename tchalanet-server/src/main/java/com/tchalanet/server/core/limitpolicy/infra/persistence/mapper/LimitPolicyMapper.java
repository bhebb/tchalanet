package com.tchalanet.server.core.limitpolicy.infra.persistence.mapper;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitPolicyEntity;
import org.springframework.stereotype.Component;

@Component
public class LimitPolicyMapper {

  public LimitPolicyEntity toEntity(LimitPolicy domain) {
    LimitPolicyEntity entity = new LimitPolicyEntity();
    entity.setId(domain.getId());
    entity.setTenantId(domain.getTenantId());
    entity.setScope(domain.getScope());
    entity.setTarget(domain.getTarget());
    entity.setDailyCap(domain.getDailyCap());
    entity.setMaxStakePerLine(domain.getMaxStakePerLine());
    entity.setMaxPayoutPerLine(domain.getMaxPayoutPerLine());
    entity.setOnBreach(domain.getOnBreach());
    entity.setActive(domain.isActive());
    // createdAt, updatedAt, version are handled by BaseTenantEntity
    return entity;
  }

  public LimitPolicy toDomain(LimitPolicyEntity entity) {
    return LimitPolicy.load(
        entity.getId(),
        entity.getTenantId(),
        entity.getScope(),
        entity.getTarget(),
        entity.getDailyCap(),
        entity.getMaxStakePerLine(),
        entity.getMaxPayoutPerLine(),
        entity.getOnBreach(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
