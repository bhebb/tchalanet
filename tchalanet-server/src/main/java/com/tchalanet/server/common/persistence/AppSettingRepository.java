package com.tchalanet.server.common.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin/app-settings", collectionResourceRel = "app-settings")
public interface AppSettingRepository extends JpaRepository<AppSettingEntity, UUID> {
}
