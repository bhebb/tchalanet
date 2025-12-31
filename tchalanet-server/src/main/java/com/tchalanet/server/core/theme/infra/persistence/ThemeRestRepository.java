package com.tchalanet.server.core.theme.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

// Deprecated: do not expose platform themes via SDR. Use controller + JpaThemeRepository.
@RepositoryRestResource(exported = false, path = "themes", collectionResourceRel = "themes")
public interface ThemeRestRepository extends JpaRepository<ThemeJpaEntity, UUID> {

  // kept methods removed — use JpaThemeRepository directly
}
