package com.tchalanet.server.common.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin/permissions", collectionResourceRel = "permissions")
public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {}
