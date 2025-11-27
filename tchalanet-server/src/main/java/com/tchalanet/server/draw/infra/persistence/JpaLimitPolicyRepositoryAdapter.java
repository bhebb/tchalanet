package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.draw.domain.model.LimitPolicy;
import com.tchalanet.server.draw.domain.ports.LimitPolicyRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaLimitPolicyRepositoryAdapter implements LimitPolicyRepository {

  private final LimitPolicyJpaRepository jpa;

  @Override
  public Optional<LimitPolicy> findById(UUID id) {
    return jpa.findById(id)
        .map(
            e ->
                new LimitPolicy(
                    e.getId(),
                    e.getTenantId(),
                    e.getScope(),
                    e.getTarget(),
                    e.getDailyCap(),
                    com.tchalanet.server.draw.domain.model.LimitPolicy.BreachOutcome.valueOf(
                        e.getOnBreach())));
  }

  @Override
  public LimitPolicy save(LimitPolicy p) {
    var e = new LimitPolicyJpaEntity();
    e.setId(p.id());
    e.setTenantId(p.tenantId());
    e.setScope(p.scope());
    e.setTarget(p.target());
    e.setDailyCap(p.dailyCap());
    e.setOnBreach(p.onBreach().name());
    var saved = jpa.save(e);
    return new LimitPolicy(
        saved.getId(),
        saved.getTenantId(),
        saved.getScope(),
        saved.getTarget(),
        saved.getDailyCap(),
        com.tchalanet.server.draw.domain.model.LimitPolicy.BreachOutcome.valueOf(
            saved.getOnBreach()));
  }
}
