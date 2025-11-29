package com.tchalanet.server.core.limitpolicy.application;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitPolicy;
import com.tchalanet.server.core.limitpolicy.domain.ports.in.UpsertLimitPolicyUseCase;
import com.tchalanet.server.core.limitpolicy.domain.ports.out.LimitPolicyRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpsertLimitPolicyService implements UpsertLimitPolicyUseCase {

  private final LimitPolicyRepositoryPort limitPolicyRepository;

  @Override
  @Transactional
  public LimitPolicy upsert(UpsertLimitPolicyCommand command) {
    LimitPolicy policy;
    if (command.id() != null) {
      policy =
          limitPolicyRepository
              .findById(command.id())
              .orElseThrow(
                  () -> new IllegalArgumentException("Limit Policy not found: " + command.id()));
      policy.update(
          command.scope(),
          command.target(),
          command.dailyCap(),
          command.maxStakePerLine(),
          command.maxPayoutPerLine(),
          command.onBreach(),
          command.active());
      log.info("Updated Limit Policy {} for tenant {}", policy.getId(), policy.getTenantId());
    } else {
      policy =
          LimitPolicy.create(
              command.tenantId(),
              command.scope(),
              command.target(),
              command.dailyCap(),
              command.maxStakePerLine(),
              command.maxPayoutPerLine(),
              command.onBreach());
      log.info("Created new Limit Policy {} for tenant {}", policy.getId(), policy.getTenantId());
    }
    return limitPolicyRepository.save(policy);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<LimitPolicy> getLimitPolicy(UUID policyId) {
    return limitPolicyRepository.findById(policyId);
  }
}
