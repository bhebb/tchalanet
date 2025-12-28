package com.tchalanet.server.core.user.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin-user-preferences", collectionResourceRel = "user-preferences")
public interface UserPreferenceRestRepository
    extends JpaRepository<UserPreferenceJpaEntity, UUID> {}
