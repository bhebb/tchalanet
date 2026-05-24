package com.tchalanet.server.core.agent.internal.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AgentRepository extends JpaRepository<AgentJpaEntity, UUID> {
  // Intentionally no tenant or deletedAt in method signatures: rely on RLS and DB visibility
}
