package com.tchalanet.server.platform.identity.internal.persistence.repository;

import com.tchalanet.server.platform.identity.internal.persistence.entity.UserPreferenceJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceJpaRepository extends JpaRepository<UserPreferenceJpaEntity, UUID> {}
