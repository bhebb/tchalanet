package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.DrawChannelId;


public sealed interface LimitTarget
    permits LimitTarget.TenantTarget,
            LimitTarget.AgentTarget,
            LimitTarget.OutletTarget,
            LimitTarget.TerminalTarget,
            LimitTarget.DrawChannelTarget {

  TargetType type();

  /** target_id est NULL en DB. */
  record TenantTarget() implements LimitTarget {
    @Override public TargetType type() { return TargetType.TENANT; }
  }

  record AgentTarget(AgentId id) implements LimitTarget {
    public AgentTarget {
      if (id == null) throw new IllegalArgumentException("AgentTarget.id is null");
    }
    @Override public TargetType type() { return TargetType.AGENT; }
  }

  record OutletTarget(OutletId id) implements LimitTarget {
    public OutletTarget {
      if (id == null) throw new IllegalArgumentException("OutletTarget.id is null");
    }
    @Override public TargetType type() { return TargetType.OUTLET; }
  }

  record TerminalTarget(TerminalId id) implements LimitTarget {
    public TerminalTarget {
      if (id == null) throw new IllegalArgumentException("TerminalTarget.id is null");
    }
    @Override public TargetType type() { return TargetType.TERMINAL; }
  }

  record DrawChannelTarget(DrawChannelId id) implements LimitTarget {
    public DrawChannelTarget {
      if (id == null) throw new IllegalArgumentException("DrawChannelTarget.id is null");
    }
    @Override public TargetType type() { return TargetType.DRAWCHANNEL; }
  }

  // Factory helpers (optionnel mais pratique)
  static LimitTarget tenant() { return new TenantTarget(); }
  static LimitTarget agent(AgentId id) { return new AgentTarget(id); }
  static LimitTarget outlet(OutletId id) { return new OutletTarget(id); }
  static LimitTarget terminal(TerminalId id) { return new TerminalTarget(id); }
  static LimitTarget drawChannel(DrawChannelId id) { return new DrawChannelTarget(id); }
}
