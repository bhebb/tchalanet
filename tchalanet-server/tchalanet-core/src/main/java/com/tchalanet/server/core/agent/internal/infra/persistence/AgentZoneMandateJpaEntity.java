package com.tchalanet.server.core.agent.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name="agent_zone_mandate", uniqueConstraints = @UniqueConstraint(name="uq_agent_zone_mandate", columnNames={"agent_id", "zone_id"}))
@Getter
@Setter
public class AgentZoneMandateJpaEntity extends BaseTenantEntity {
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="agent_id", nullable=false)
  private AgentJpaEntity agent;
  @Column(name="zone_id", nullable=false)
  private UUID zoneId;
  @Column(name="can_sell", nullable=false)
  private boolean canSell;
  @Column(name="can_create_sub_agents", nullable=false)
  private boolean canCreateSubAgents;
  @Column(name="can_create_sellers", nullable=false)
  private boolean canCreateSellers;
  @Column(name="can_create_outlets", nullable=false)
  private boolean canCreateOutlets;
  @Column(name="can_manage_terminals", nullable=false)
  private boolean canManageTerminals;
  @Column(name="can_view_reports", nullable=false)
  private boolean canViewReports;
  @Column(name="max_child_agent_depth", nullable=false)
  private int maxChildAgentDepth;
  @Column(name="max_child_agents", nullable=false)
  private int maxChildAgents;
  @Column(name="max_sellers", nullable=false)
  private int maxSellers;
  @Column(name="max_terminals", nullable=false)
  private int maxTerminals;
}
