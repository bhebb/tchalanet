package com.tchalanet.server.core.tenantconfig.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for TenantJpaEntity.
 * Per DOMAIN_TENANT_CONFIG.md:
 * - Query by ID and code
 * - Check code existence
 */
public interface TenantRepository extends JpaRepository<TenantJpaEntity, UUID> {


    @Query("select t.id from TenantJpaEntity t WHERE LOWER(t.code) = LOWER(:code) and t.deletedAt is null")
    Optional<UUID> findIdByCodeActive(String code);

    @Query("select t from TenantJpaEntity t where t.id = :id and t.deletedAt is null")
    Optional<TenantJpaEntity> findByIdActive(UUID id);

}
