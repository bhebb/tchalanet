package com.tchalanet.server.core.agent.internal.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AgentUserAssignmentRepository extends JpaRepository<AgentUserAssignmentJpaEntity, UUID> {
  boolean existsByAgentIdAndUserIdAndRelation(UUID agentId, UUID userId, String relation);
  Optional<AgentUserAssignmentJpaEntity> findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);
  List<AgentUserAssignmentJpaEntity> findByAgentIdAndActiveTrue(UUID agentId);
}
