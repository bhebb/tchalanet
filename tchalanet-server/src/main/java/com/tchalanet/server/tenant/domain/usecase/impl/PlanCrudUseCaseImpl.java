package com.tchalanet.server.tenant.domain.usecase.impl;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.tenant.domain.model.Plan;
import com.tchalanet.server.tenant.domain.ports.PlanRepository;
import com.tchalanet.server.tenant.domain.usecase.PlanCrudUseCase;
import com.tchalanet.server.tenant.infra.persistence.JpaSubscriptionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PlanCrudUseCaseImpl implements PlanCrudUseCase {

  private final PlanRepository planRepository;
  private final JpaSubscriptionRepository subscriptionRepository;
  private final LogAuditEventUseCase auditLog;

  @Override
  @Transactional
  public Plan create(Plan p) {
    return planRepository.save(p);
  }

  @Override
  public Optional<Plan> getByCode(String code) {
    return planRepository.findByCode(code);
  }

  @Override
  public List<Plan> listAll() {
    return planRepository.findAll();
  }

  @Override
  @Transactional
  public Plan update(String code, Plan p) {
    var opt = planRepository.findByCode(code);
    if (opt.isEmpty()) throw new IllegalArgumentException("Plan not found: " + code);
    Plan existing = opt.get();
    existing =
        new Plan(
            existing.id(), existing.code(), p.name(), p.priceAmount(), p.currency(), p.features());
    return planRepository.save(existing);
  }

  @Override
  @Transactional
  public void delete(String code) {
    var opt = planRepository.findByCode(code);
    if (opt.isEmpty()) return;
    Plan plan = opt.get();
    // check if any subscription references this plan
    if (subscriptionRepository.existsByPlanId(plan.id())) {
      throw ProblemRestException.badRequest(
          "Cannot delete plan: there are subscriptions referencing it");
    }
    // soft-delete and log audit
    planRepository.softDeleteByCode(code);
    var ev =
        AuditEvent.of(
            /* tenantId */ null,
            AuditActorType.USER,
            /* actorId */ null,
            AuditEntityType.PLAN,
            plan.id().toString(),
            AuditAction.SOFT_DELETE,
            Map.of("code", plan.code()).toString(),
            null,
            null);
    auditLog.log(ev);
  }
}
