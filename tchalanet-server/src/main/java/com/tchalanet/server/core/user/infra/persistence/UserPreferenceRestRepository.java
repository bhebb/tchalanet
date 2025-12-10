package com.tchalanet.server.core.user.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@RepositoryRestResource(path = "admin/user-preferences", collectionResourceRel = "user-preferences")
public interface UserPreferenceRestRepository extends JpaRepository<UserPreferenceJpaEntity, UUID>, QuerydslPredicateExecutor<UserPreferenceJpaEntity> {}
