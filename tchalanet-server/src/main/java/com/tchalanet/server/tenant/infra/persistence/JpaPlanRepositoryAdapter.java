package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.tenant.domain.model.Plan;
import com.tchalanet.server.tenant.domain.ports.PlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPlanRepositoryAdapter implements PlanRepository {

  private final JpaPlanRepository jpa;

  @Override
  public Optional<Plan> findById(UUID id) {
    return jpa.findById(id)
        .map(
            e ->
                new Plan(
                    e.getId(),
                    e.getCode(),
                    e.getName(),
                    e.getPriceAmount(),
                    e.getCurrency(),
                    e.getFeatures()));
  }

  @Override
  public Optional<Plan> findPublicByCode(String code) {
    return jpa.findByCodeAndPublicPlanTrue(code)
        .map(
            e ->
                new Plan(
                    e.getId(),
                    e.getCode(),
                    e.getName(),
                    e.getPriceAmount(),
                    e.getCurrency(),
                    e.getFeatures()));
  }

  @Override
  public List<Plan> findPublicPlans() {
    return jpa.findByPublicPlanTrueOrderByPriceAmountAsc().stream()
        .map(
            e ->
                new Plan(
                    e.getId(),
                    e.getCode(),
                    e.getName(),
                    e.getPriceAmount(),
                    e.getCurrency(),
                    e.getFeatures()))
        .collect(Collectors.toList());
  }

  @Override
  public Plan save(Plan plan) {
    PlanJpaEntity e = new PlanJpaEntity();
    if (plan.id() != null) e.setId(plan.id());
    e.setCode(plan.code());
    e.setName(plan.name());
    e.setPriceAmount(plan.priceAmount());
    e.setCurrency(plan.currency());
    e.setFeatures(plan.features());
    var saved = jpa.save(e);
    return new Plan(
        saved.getId(),
        saved.getCode(),
        saved.getName(),
        saved.getPriceAmount(),
        saved.getCurrency(),
        saved.getFeatures());
  }

  @Override
  public void delete(Plan plan) {
    // physical delete removed: prefer soft-delete by updating deleted_at
    jpa.findByCodeAndPublicPlanTrue(plan.code())
        .ifPresent(
            e -> {
              e.setDeletedAt(java.time.Instant.now());
              jpa.save(e);
            });
  }

  @Override
  public List<Plan> findAll() {
    return jpa.findAll().stream()
        .map(
            e ->
                new Plan(
                    e.getId(),
                    e.getCode(),
                    e.getName(),
                    e.getPriceAmount(),
                    e.getCurrency(),
                    e.getFeatures()))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Plan> findByCode(String code) {
    return jpa.findAll().stream()
        .filter(e -> code.equals(e.getCode()))
        .findFirst()
        .map(
            e ->
                new Plan(
                    e.getId(),
                    e.getCode(),
                    e.getName(),
                    e.getPriceAmount(),
                    e.getCurrency(),
                    e.getFeatures()));
  }

  @Override
  public void softDeleteByCode(String code) {
    jpa.findByCodeAndPublicPlanTrue(code)
        .ifPresent(
            e -> {
              e.setDeletedAt(java.time.Instant.now());
              jpa.save(e);
            });
  }
}
