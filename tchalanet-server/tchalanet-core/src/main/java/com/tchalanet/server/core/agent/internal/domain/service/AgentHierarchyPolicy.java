package com.tchalanet.server.core.agent.internal.domain.service;

import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.core.agent.api.model.AgentType;
import com.tchalanet.server.core.agent.internal.domain.model.Agent;
import com.tchalanet.server.core.agent.internal.domain.model.AgentZoneMandate;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class AgentHierarchyPolicy {

  public int childDepth(Agent parent) {
    return parent == null ? 1 : parent.depth() + 1;
  }

  public void requireCanCreateChild(Agent parent, AgentZoneId childPrimaryZoneId, List<AgentZoneId> childAllowedZones) {
    if (parent == null) return;
    if (!parent.activeForSale()) throw new IllegalArgumentException("agent.parent_not_active");
    if (parent.depth() >= Agent.MAX_V1_DEPTH) throw new IllegalArgumentException("agent.max_depth_exceeded_v1");
    var delegatingMandates = parent.mandates().stream()
        .filter(AgentZoneMandate::canDelegate)
        .map(AgentZoneMandate::zoneId)
        .toList();
    if (!delegatingMandates.contains(childPrimaryZoneId)) throw new IllegalArgumentException("agent.zone_not_delegated_by_parent");
    for (var zoneId : childAllowedZones) {
      if (!delegatingMandates.contains(zoneId)) throw new IllegalArgumentException("agent.allowed_zone_not_delegated_by_parent");
    }
  }

  public void requireTypeAllowedAtDepth(AgentType type, int depth) {
    if (depth == 0) throw new IllegalArgumentException("agent.depth_zero_reserved_for_tenant");
    if (depth == 1 && type == AgentType.AFFILIATE) throw new IllegalArgumentException("agent.level1_should_be_parent_affiliate");
    if (depth == 2 && type == AgentType.AFFILIATE_PARENT) throw new IllegalArgumentException("agent.level2_cannot_be_parent_affiliate_v1");
  }
}
