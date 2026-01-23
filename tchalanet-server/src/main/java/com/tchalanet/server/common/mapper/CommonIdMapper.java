package com.tchalanet.server.common.mapper;

import com.tchalanet.server.common.types.id.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Central helper for ID conversions between UUID and domain wrappers. MapStruct can use this via
 * `uses = CommonIdMapper.class` on @Mapper.
 */
@Component
public class CommonIdMapper {

    // TenantId
    public UUID mapFromTenantId(TenantId id) {
        return id == null ? null : id.value();
    }

    public TenantId mapToTenantId(UUID id) {
        return TenantId.nullableOf(id);
    }

    // UserId
    public UUID mapFromAddressId(AddressId id) {
        return id == null ? null : id.value();
    }

    public UUID mapFromUserId(UserId id) {
        return id == null ? null : id.value();
    }

    public UserId mapToUserId(UUID id) {
        return UserId.nullableOf(id);
    }

    // DrawId
    public UUID mapFromDrawId(DrawId id) {
        return id == null ? null : id.value();
    }

    public DrawId mapToDrawId(UUID id) {
        return DrawId.nullableOf(id);
    }

    // OutletId
    public UUID mapFromOutletId(OutletId id) {
        return id == null ? null : id.value();
    }

    public OutletId mapToOutletId(UUID id) {
        return OutletId.nullableOf(id);
    }

    // TerminalId
    public UUID mapFromTerminalId(TerminalId id) {
        return id == null ? null : id.value();
    }

    public TerminalId mapToTerminalId(UUID id) {
        return TerminalId.nullableOf(id);
    }

    // TicketId
    public UUID mapFromTicketId(TicketId id) {
        return id == null ? null : id.value();
    }

    public TicketId mapToTicketId(UUID id) {
        return TicketId.nullableOf(id);
    }

    // PayoutId
    public UUID mapFromPayoutId(PayoutId id) {
        return id == null ? null : id.value();
    }

    public PayoutId mapToPayoutId(UUID id) {
        return PayoutId.nullableOf(id);
    }

    // AgentId
    public UUID mapFromAgentId(AgentId id) {
        return id == null ? null : id.value();
    }

    public AgentId mapToAgentId(UUID id) {
        return AgentId.nullableOf(id);
    }

    // RoleId
    public UUID mapFromRoleId(RoleId id) {
        return id == null ? null : id.value();
    }

    public RoleId mapToRoleId(UUID id) {
        return RoleId.nullableOf(id);
    }

    // SessionId
    public UUID mapFromSessionId(SessionId id) {
        return id == null ? null : id.value();
    }

    public SessionId mapToSessionId(UUID id) {
        return SessionId.nullableOf(id);
    }
}
