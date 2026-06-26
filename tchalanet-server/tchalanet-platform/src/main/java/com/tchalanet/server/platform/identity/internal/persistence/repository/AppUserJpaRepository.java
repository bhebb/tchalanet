package com.tchalanet.server.platform.identity.internal.persistence.repository;

import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, UUID> {

    Optional<AppUserJpaEntity> findByEmail(String email);

    Optional<AppUserJpaEntity> findByPhone(String phone);

    Optional<AppUserJpaEntity> findByEmailOrPhone(String email, String phone);

    @Modifying
    @Query(
        value =
            """
                update app_user
                   set last_login_at = :lastLoginAt
                 where id = :appUserId
                   and deleted_at is null
                """,
        nativeQuery = true)
    int touchLastLogin(
        @Param("appUserId") UUID appUserId,
        @Param("lastLoginAt") java.time.Instant lastLoginAt);

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
                select u.* from app_user u
                join tenant_user tu on tu.user_id = u.id
                join tenant_user_role tur on tur.user_id = u.id and tur.tenant_id = tu.tenant_id
                join app_role ar on ar.id = tur.role_id
                where tu.tenant_id = :tenantId
                  and tu.deleted_at is null
                  and tu.status = 'ACTIVE'
                  and tur.deleted_at is null
                  and ar.deleted_at is null
                  and ar.active = true
                  and ar.code = 'TENANT_ADMIN'
                  and u.deleted_at is null
                  and u.status = 'ACTIVE'
                order by lower(coalesce(u.display_name, u.email::text, u.username))
                """,
        nativeQuery = true)
    List<AppUserJpaEntity> findTenantAdminsForNotificationDelivery(@Param("tenantId") UUID tenantId);

    @Query(
        value =
            """
                select u.* from app_user u
                join tenant_user tu on tu.user_id = u.id
                where tu.tenant_id = :tenantId
                  and tu.deleted_at is null
                  and tu.status = 'ACTIVE'
                  and u.deleted_at is null
                  and u.status = 'ACTIVE'
                order by lower(coalesce(u.display_name, u.email::text, u.username))
                """,
        nativeQuery = true)
    List<AppUserJpaEntity> findTenantUsersForNotificationDelivery(@Param("tenantId") UUID tenantId);

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

    @Query(
        value =
            """
                select u.*
                from app_user u
                where u.deleted_at is null
                  and not exists (
                    select 1 from tenant_user_role tur
                    where tur.user_id = u.id and tur.deleted_at is null
                  )
                  and (:q is null or lower(coalesce(u.display_name, u.email::text, u.username)) like :q)
                order by lower(coalesce(u.display_name, u.email::text, u.username))
                limit :size offset :offset
                """,
        nativeQuery = true)
    List<AppUserJpaEntity> findUnassigned(
        @Param("q") String q,
        @Param("size") int size,
        @Param("offset") int offset);

    @Query(
        value =
            """
                select count(*)
                from app_user u
                where u.deleted_at is null
                  and not exists (
                    select 1 from tenant_user_role tur
                    where tur.user_id = u.id and tur.deleted_at is null
                  )
                  and (:q is null or lower(coalesce(u.display_name, u.email::text, u.username)) like :q)
                """,
        nativeQuery = true)
    long countUnassigned(@Param("q") String q);
}
