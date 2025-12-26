package com.tchalanet.server.core.accesscontrol.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin/tenant-users", collectionResourceRel = "tenant-users")
public interface TenantUserRepository extends JpaRepository<TenantUserEntity, UUID> {

  List<TenantUserEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);
}
