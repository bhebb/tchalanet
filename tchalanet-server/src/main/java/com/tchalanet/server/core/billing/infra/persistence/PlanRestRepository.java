package com.tchalanet.server.core.billing.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.UUID;

@RepositoryRestResource(path = "admin/plans", collectionResourceRel = "plans")
public interface PlanRestRepository extends JpaRepository<PlanJpaEntity, UUID>, QuerydslPredicateExecutor<PlanJpaEntity> {
}

