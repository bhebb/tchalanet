package com.tchalanet.server.core.agent.internal.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AgentZoneRepository extends JpaRepository<AgentZoneJpaEntity, UUID> {
  Optional<AgentZoneJpaEntity> findById(UUID id);
  boolean existsByCode(String code);
  List<AgentZoneJpaEntity> findAllByOrderByDepthAscNameAsc();
  List<AgentZoneJpaEntity> findByStatusOrderByDepthAscNameAsc(String status);
}
