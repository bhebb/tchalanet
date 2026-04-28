package com.tchalanet.server.core.user.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAppUserRepository extends JpaRepository<AppUserJpaEntity, UUID> {
  Optional<AppUserJpaEntity> findByKeycloakSub(UUID keycloakSub);

  Optional<AppUserJpaEntity> findByEmail(String email);

  Optional<AppUserJpaEntity> findByPhone(String phone);

  Optional<AppUserJpaEntity> findByEmailOrPhone(String email, String phone);

  // Paged
  Page<AppUserJpaEntity> findAll(Pageable pageable);

  @RestResource(path = "by-status", rel = "by-status")
  Page<AppUserJpaEntity> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

  // Users who are active members of a specific tenant (via tenant_user join)
  @Query(
      value = """
          SELECT u.* FROM app_user u
          JOIN tenant_user tu ON tu.user_id = u.id
          WHERE tu.tenant_id = :tenantId
            AND tu.deleted_at IS NULL
            AND tu.status = 'ACTIVE'
            AND u.deleted_at IS NULL
          """,
      countQuery = """
          SELECT count(*) FROM app_user u
          JOIN tenant_user tu ON tu.user_id = u.id
          WHERE tu.tenant_id = :tenantId
            AND tu.deleted_at IS NULL
            AND tu.status = 'ACTIVE'
            AND u.deleted_at IS NULL
          """,
      nativeQuery = true)
  Page<AppUserJpaEntity> findByTenantMembership(@Param("tenantId") UUID tenantId, Pageable pageable);
}
