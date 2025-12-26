package com.tchalanet.server.core.tenant.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin/tenants", collectionResourceRel = "tenants")
public interface TenantRestRepository
    extends JpaRepository<TenantJpaEntity, UUID>, QuerydslPredicateExecutor<TenantJpaEntity> {}
