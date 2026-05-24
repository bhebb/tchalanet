package com.tchalanet.server.core.agent.internal.domain.service;

import com.tchalanet.server.core.agent.internal.domain.model.AgentZone;

public class AgentZonePolicy {
  public int computeChildDepth(AgentZone parent) { return parent == null ? 0 : parent.depth() + 1; }
  public void requireActive(AgentZone zone) {
    if (zone == null) throw new IllegalArgumentException("agent_zone.not_found");
    if (!zone.active()) throw new IllegalArgumentException("agent_zone.inactive");
  }
}
