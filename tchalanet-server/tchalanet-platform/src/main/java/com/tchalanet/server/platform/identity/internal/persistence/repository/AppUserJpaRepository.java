package com.tchalanet.server.platform.identity.internal.persistence.repository;

import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, UUID> {

    Optional<AppUserJpaEntity> findByKeycloakSub(UUID keycloakSub);

    Optional<AppUserJpaEntity> findByEmail(String email);

    Optional<AppUserJpaEntity> findByPhone(String phone);

    Optional<AppUserJpaEntity> findByEmailOrPhone(String email, String phone);

    @Query(
        value =
            """
                select u.* from app_user u
                join tenant_user tu on tu.user_id = u.id
                where tu.tenant_id = :tenantId
                  and tu.deleted_at is null
                  and tu.status = 'ACTIVE'
                  and u.deleted_at is null
                """,
        countQuery =
            """
                select count(*) from app_user u
                join tenant_user tu on tu.user_id = u.id
                where tu.tenant_id = :tenantId
                  and tu.deleted_at is null
                  and tu.status = 'ACTIVE'
                  and u.deleted_at is null
                """,
        nativeQuery = true)
    Page<AppUserJpaEntity> findByTenantMembership(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(
        value =
            """
                    select count(*) from app_user u
                    join tenant_user tu on tu.user_id = u.id
                    where tu.tenant_id = :tenantId
                      and tu.deleted_at is null
                      and tu.status = 'ACTIVE'
                      and u.deleted_at is null
                """,
        nativeQuery = true)
    long countActiveTenantUsers(@Param("tenantId") UUID tenantId);
}
