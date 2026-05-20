package com.tchalanet.server.platform.communication.internal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunicationSettingsJpaRepository
    extends JpaRepository<CommunicationSettingsJpaEntity, UUID> {

  Optional<CommunicationSettingsJpaEntity> findByTenantId(UUID tenantId);
}
