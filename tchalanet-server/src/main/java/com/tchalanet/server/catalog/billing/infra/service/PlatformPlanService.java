package com.tchalanet.server.catalog.billing.infra.service;

import com.tchalanet.server.catalog.billing.infra.persistence.PlanJpaEntity;
import com.tchalanet.server.catalog.billing.infra.persistence.repo.PlanJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformPlanService {

  private final PlanJpaRepository repo;

  public List<PlanJpaEntity> list() {
    return repo.findByPublicPlanTrue();
  }

  public PlanJpaEntity get(UUID id) {
    return repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
  }

  @Transactional
  public PlanJpaEntity create(PlanJpaEntity p) {
    p.setId(null);
    return repo.save(p);
  }

  @Transactional
  public PlanJpaEntity update(UUID id, PlanJpaEntity p) {
    var existing =
        repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
    // copy updatable fields
    existing.setCode(p.getCode());
    existing.setName(p.getName());
    existing.setDescription(p.getDescription());
    existing.setPriceAmount(p.getPriceAmount());
    existing.setCurrency(p.getCurrency());
    existing.setBillingFrequency(p.getBillingFrequency());
    existing.setPublicPlan(p.isPublicPlan());
    existing.setFeatures(p.getFeatures());
    return repo.save(existing);
  }

  @Transactional
  public void delete(UUID id) {
    repo.deleteById(id);
  }
}
