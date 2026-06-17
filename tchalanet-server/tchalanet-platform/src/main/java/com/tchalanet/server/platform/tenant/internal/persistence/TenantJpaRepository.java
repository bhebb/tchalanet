package com.tchalanet.server.platform.tenant.internal.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.tchalanet.server.common.exception.TchNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for TenantJpaEntity.
 * Per DOMAIN_TENANT_CONFIG.md:
 * - Query by ID and code
 * - Check code existence
 * - Provide required active lookup helper (`getRequiredByIdActive`)
 */
public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, UUID> {


    @Query("select t.id from TenantJpaEntity t WHERE LOWER(t.code) = LOWER(:code) and t.deletedAt is null")
    Optional<UUID> findIdByCodeActive(String code);

    @Query("select t from TenantJpaEntity t where t.id = :id and t.deletedAt is null")
    Optional<TenantJpaEntity> findByIdActive(UUID id);

    @Query("select t from TenantJpaEntity t where lower(t.code) = lower(:code) and t.deletedAt is null")
    Optional<TenantJpaEntity> findByCodeActive(@Param("code") String code);

    default TenantJpaEntity getRequiredByIdActive(UUID id) {
        return findByIdActive(id)
            .orElseThrow(() -> new TchNotFoundException(id.toString(), "Tenant not found"));
    }

    @Modifying
    @Query("UPDATE TenantJpaEntity t SET t.defaultCommissionRate = :rate WHERE t.id = :id AND t.deletedAt IS NULL")
    int updateDefaultCommissionRate(@Param("id") UUID id, @Param("rate") BigDecimal rate);
}
