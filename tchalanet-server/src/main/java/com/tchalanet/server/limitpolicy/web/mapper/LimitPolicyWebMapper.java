package com.tchalanet.server.limitpolicy.web.mapper;

import com.tchalanet.server.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.limitpolicy.domain.ports.in.UpsertLimitPolicyUseCase;
import com.tchalanet.server.limitpolicy.web.dto.LimitPolicyRequest;
import com.tchalanet.server.limitpolicy.web.dto.LimitPolicyResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LimitPolicyWebMapper {

  public UpsertLimitPolicyUseCase.UpsertLimitPolicyCommand toUpsertCommand(
      UUID tenantId, LimitPolicyRequest request) {
    return new UpsertLimitPolicyUseCase.UpsertLimitPolicyCommand(
        request.id(),
        tenantId,
        request.scope(),
        request.target(),
        request.dailyCap(),
        request.maxStakePerLine(),
        request.maxPayoutPerLine(),
        request.onBreach(),
        request.active());
  }

  public LimitPolicyResponse toLimitPolicyResponse(LimitPolicy domain) {
    return new LimitPolicyResponse(
        domain.getId(),
        domain.getTenantId(),
        domain.getScope(),
        domain.getTarget(),
        domain.getDailyCap(),
        domain.getMaxStakePerLine(),
        domain.getMaxPayoutPerLine(),
        domain.getOnBreach(),
        domain.isActive(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }
}
