package com.tchalanet.server.common.mapper;

import com.tchalanet.server.common.types.id.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Central helper for ID conversions between UUID and domain wrappers.
 * MapStruct can use this via `uses = CommonIdMapper.class` on @Mapper.
 */
@Component
public class CommonIdMapper {

  // TenantId
  public UUID mapFromTenantId(TenantId id) { return id == null ? null : id.uuid(); }
  public TenantId mapToTenantId(UUID id) { return id == null ? null : TenantId.of(id); }

  // UserId
  public UUID mapFromUserId(UserId id) { return id == null ? null : id.uuid(); }
  public UserId mapToUserId(UUID id) { return id == null ? null : UserId.of(id); }

  // DrawId
  public UUID mapFromDrawId(DrawId id) { return id == null ? null : id.uuid(); }
  public DrawId mapToDrawId(UUID id) { return id == null ? null : DrawId.of(id); }

  // OutletId
  public UUID mapFromOutletId(OutletId id) { return id == null ? null : id.uuid(); }
  public OutletId mapToOutletId(UUID id) { return id == null ? null : OutletId.of(id); }

  // TerminalId
  public UUID mapFromTerminalId(TerminalId id) { return id == null ? null : id.uuid(); }
  public TerminalId mapToTerminalId(UUID id) { return id == null ? null : TerminalId.of(id); }

  // TicketId
  public UUID mapFromTicketId(TicketId id) { return id == null ? null : id.uuid(); }
  public TicketId mapToTicketId(UUID id) { return id == null ? null : TicketId.of(id); }

  // PayoutId
  public UUID mapFromPayoutId(PayoutId id) { return id == null ? null : id.uuid(); }
  public PayoutId mapToPayoutId(UUID id) { return id == null ? null : PayoutId.of(id); }

  // AgentId
  public UUID mapFromAgentId(AgentId id) { return id == null ? null : id.uuid(); }
  public AgentId mapToAgentId(UUID id) { return id == null ? null : AgentId.of(id); }

  // RoleId
  public UUID mapFromRoleId(RoleId id) { return id == null ? null : id.uuid(); }
  public RoleId mapToRoleId(UUID id) { return id == null ? null : RoleId.of(id); }

  // SessionId
  public UUID mapFromSessionId(SessionId id) { return id == null ? null : id.uuid(); }
  public SessionId mapToSessionId(UUID id) { return id == null ? null : SessionId.of(id); }
}

