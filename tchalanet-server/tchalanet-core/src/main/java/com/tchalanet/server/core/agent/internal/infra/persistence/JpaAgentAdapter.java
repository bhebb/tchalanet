package com.tchalanet.server.core.agent.internal.infra.persistence;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.agent.api.model.AgentZoneStatus;
import com.tchalanet.server.core.agent.internal.application.port.out.*;
import com.tchalanet.server.core.agent.internal.domain.model.*;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaAgentAdapter implements AgentReaderPort, AgentWriterPort {
  private final AgentRepository agentRepo;
  private final AgentZoneRepository zoneRepo;
  private final AgentUserAssignmentRepository assignmentRepo;
  private final Clock clock;
  private final AgentJpaMapper mapper = new AgentJpaMapper();

  @Override public Optional<Agent> findAgent(TenantId t, AgentId id) { return agentRepo.findById(id.value()).map(mapper::toDomain); }
  @Override public Agent getAgentRequired(TenantId t, AgentId id) { return findAgent(t,id).orElseThrow(() -> new IllegalArgumentException("agent.not_found")); }
  @Override public Optional<AgentZone> findZone(TenantId t, AgentZoneId id) { return zoneRepo.findById(id.value()).map(mapper::toDomain); }
  @Override public AgentZone getZoneRequired(TenantId t, AgentZoneId id) { return findZone(t,id).orElseThrow(() -> new IllegalArgumentException("agent_zone.not_found")); }
  @Override public List<AgentZone> listZones(TenantId t, boolean activeOnly) { var rows = activeOnly ? zoneRepo.findByStatusOrderByDepthAscNameAsc(AgentZoneStatus.ACTIVE.name()) : zoneRepo.findAllByOrderByDepthAscNameAsc(); return rows.stream().map(mapper::toDomain).toList(); }
  @Override public List<Agent> listAgents(TenantId t) { return agentRepo.findAll().stream().map(mapper::toDomain).toList(); }
  @Override public Optional<Agent> findAgentForSeller(TenantId t, UserId userId) { return assignmentRepo.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(userId.value()).flatMap(a -> findAgent(t, AgentId.of(a.getAgentId()))); }
  @Override public List<AgentId> agentPath(TenantId t, AgentId leaf) { var out = new ArrayList<AgentId>(); Agent current = getAgentRequired(t, leaf); while (current != null) { out.add(0, current.id()); current = current.parentAgentId()==null ? null : getAgentRequired(t, current.parentAgentId()); } return out; }
  @Override public List<AgentZoneId> zonePath(TenantId t, AgentZoneId leaf) { var out = new ArrayList<AgentZoneId>(); AgentZone current = getZoneRequired(t, leaf); while (current != null) { out.add(0, current.id()); current = current.parentZoneId()==null ? null : getZoneRequired(t, current.parentZoneId()); } return out; }
  @Override public AgentZone saveZone(AgentZone zone) { return mapper.toDomain(zoneRepo.save(mapper.toEntity(zone))); }
  @Override public boolean zoneCodeExists(TenantId t, String code) { return zoneRepo.existsByCode(code.trim().toUpperCase()); }
  @Override public Agent saveAgent(Agent agent) { return mapper.toDomain(agentRepo.save(mapper.toEntity(agent))); }
  @Override public void assignUser(TenantId t, AgentId agentId, UserId userId, String relation) { var e = new AgentUserAssignmentJpaEntity(); e.setTenantId(t.value()); e.setAgentId(agentId.value()); e.setUserId(userId.value()); e.setRelation(relation); e.setActive(true); e.setCreatedAt(Instant.now(clock)); assignmentRepo.save(e); }
  @Override public boolean userAssignmentExists(TenantId t, AgentId agentId, UserId userId, String relation) { return assignmentRepo.existsByAgentIdAndUserIdAndRelation(agentId.value(), userId.value(), relation); }
}
