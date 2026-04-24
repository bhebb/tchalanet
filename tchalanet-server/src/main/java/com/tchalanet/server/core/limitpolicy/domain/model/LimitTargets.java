package com.tchalanet.server.core.limitpolicy.domain.model;

import java.util.ArrayList;
import java.util.List;

/** Utility to compute candidate limit targets for a given context in priority order. */
public final class LimitTargets {

  private LimitTargets() {}

  public static List<LimitTarget> forContext(LimitContext ctx) {
    List<LimitTarget> out = new ArrayList<>();
    if (ctx.agentId() != null) out.add(new LimitTarget.AgentTarget(ctx.agentId()));
    if (ctx.terminalId() != null) out.add(new LimitTarget.TerminalTarget(ctx.terminalId()));
    if (ctx.outletId() != null) out.add(new LimitTarget.OutletTarget(ctx.outletId()));
    // drawChannel optional
    if (ctx.drawChannelId() != null) out.add(new LimitTarget.DrawChannelTarget(ctx.drawChannelId()));
    // tenant last
    out.add(new LimitTarget.TenantTarget());
    return out;
  }
}
