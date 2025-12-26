package com.tchalanet.server.core.billing.infra.persistence.adapter;

import com.tchalanet.server.core.billing.application.port.out.PlanReaderPort;
import com.tchalanet.server.core.billing.domain.model.Plan;
import com.tchalanet.server.core.billing.infra.persistence.mapper.PlanPersistenceMapper;
import com.tchalanet.server.core.billing.infra.persistence.repo.PlanJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanJpaRepositoryAdapter implements PlanReaderPort {

  private final PlanJpaRepository jpa;
  private final PlanPersistenceMapper mapper;

  @Override
  public Optional<Plan> findById(UUID planId) {
    return jpa.findById(planId).map(mapper::toDomain);
  }

  @Override
  public List<Plan> findAllPublic() {
    return jpa.findByPublicPlanTrue().stream().map(mapper::toDomain).toList();
  }
}
