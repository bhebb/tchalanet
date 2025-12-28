package com.tchalanet.server.core.theme.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin-themes", collectionResourceRel = "themes")
public interface ThemeRestRepository extends JpaRepository<ThemeJpaEntity, UUID> {}
