package com.tchalanet.server.core.theme.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.UUID;

@RepositoryRestResource(path = "admin/themes", collectionResourceRel = "themes")
public interface ThemeRestRepository extends JpaRepository<ThemeJpaEntity, UUID>, QuerydslPredicateExecutor<ThemeJpaEntity> {
}

