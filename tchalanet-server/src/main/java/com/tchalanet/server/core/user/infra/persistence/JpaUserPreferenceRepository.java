package com.tchalanet.server.core.user.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserPreferenceRepository extends JpaRepository<UserPreferenceJpaEntity, UUID> {}
