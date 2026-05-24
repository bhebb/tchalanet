package com.tchalanet.server.core.agent.internal.infra.persistence;

import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.core.agent.api.model.*;
import com.tchalanet.server.core.agent.internal.domain.model.*;
import java.util.ArrayList;

class AgentJpaMapper {
  AgentZone toDomain(AgentZoneJpaEntity e) {
    return new AgentZone(AgentZoneId.of(e.getId()), TenantId.of(e.getTenantId()), AgentZoneId.nullableOf(e.getParentZoneId()), e.getCode(), e.getName(), e.getZoneType(), AgentZoneStatus.valueOf(e.getStatus()), e.getDepth(), e.getCreatedAt(), e.getUpdatedAt());
  }
  AgentZoneJpaEntity toEntity(AgentZone z) {
    var e = new AgentZoneJpaEntity();
    e.setId(z.id().value()); e.setTenantId(z.tenantId().value()); e.setParentZoneId(z.parentZoneId()==null?null:z.parentZoneId().value());
    e.setCode(z.code()); e.setName(z.name()); e.setZoneType(z.zoneType()); e.setStatus(z.status().name()); e.setDepth(z.depth()); e.setCreatedAt(z.createdAt()); e.setUpdatedAt(z.updatedAt()); return e;
  }
  Agent toDomain(AgentJpaEntity e) {
    var mandates = e.getMandates().stream().map(m -> new AgentZoneMandate(AgentId.of(e.getId()), AgentZoneId.of(m.getZoneId()), m.isCanSell(), m.isCanCreateSubAgents(), m.isCanCreateSellers(), m.isCanCreateOutlets(), m.isCanManageTerminals(), m.isCanViewReports(), m.getMaxChildAgentDepth(), m.getMaxChildAgents(), m.getMaxSellers(), m.getMaxTerminals())).toList();
    return new Agent(AgentId.of(e.getId()), TenantId.of(e.getTenantId()), AgentId.nullableOf(e.getParentAgentId()), e.getDisplayName(), AgentType.valueOf(e.getType()), AgentStatus.valueOf(e.getStatus()), AgentZoneId.of(e.getPrimaryZoneId()), UserId.nullableOf(e.getOwnerUserId()), e.getDepth(), mandates, e.getCreatedAt(), e.getUpdatedAt());
  }
  AgentJpaEntity toEntity(Agent a) {
    var e = new AgentJpaEntity();
    e.setId(a.id().value()); e.setTenantId(a.tenantId().value()); e.setParentAgentId(a.parentAgentId()==null?null:a.parentAgentId().value()); e.setDisplayName(a.displayName()); e.setType(a.type().name()); e.setStatus(a.status().name()); e.setPrimaryZoneId(a.primaryZoneId().value()); e.setOwnerUserId(a.ownerUserId()==null?null:a.ownerUserId().value()); e.setDepth(a.depth()); e.setCreatedAt(a.createdAt()); e.setUpdatedAt(a.updatedAt());
    e.setMandates(new ArrayList<>());
    for (var m : a.mandates()) { var me = new AgentZoneMandateJpaEntity(); me.setAgent(e); me.setTenantId(a.tenantId().value()); me.setZoneId(m.zoneId().value()); me.setCanSell(m.canSell()); me.setCanCreateSubAgents(m.canCreateSubAgents()); me.setCanCreateSellers(m.canCreateSellers()); me.setCanCreateOutlets(m.canCreateOutlets()); me.setCanManageTerminals(m.canManageTerminals()); me.setCanViewReports(m.canViewReports()); me.setMaxChildAgentDepth(m.maxChildAgentDepth()); me.setMaxChildAgents(m.maxChildAgents()); me.setMaxSellers(m.maxSellers()); me.setMaxTerminals(m.maxTerminals()); e.getMandates().add(me); }
    return e;
  }
}
