package com.tchalanet.server.core.agent.internal.infra.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface AgentProjectionRepository extends JpaRepository<AgentJpaEntity, UUID> {

  @Query("""
      SELECT new com.tchalanet.server.core.agent.internal.infra.persistence.AgentSummaryProjection(
        a.id,
        a.displayName,
        a.type,
        a.status,
        a.createdAt
      )
      FROM AgentJpaEntity a
      WHERE a.deletedAt IS NULL
    """)
  Page<AgentSummaryProjection> findSummaries(Pageable pageable);
}

