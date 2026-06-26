package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;
import org.springframework.stereotype.Component;

@Component
public class SellerTerminalMapper {

    public SellerTerminal toDomain(SellerTerminalJpaEntity e) {
        return new SellerTerminal(
            SellerTerminalId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            e.getTerminalCode(),
            e.getFirstName(),
            e.getLastName(),
            e.getDisplayName(),
            e.getEmail(),
            e.getPhoneNumber(),
            AddressId.nullableOf(e.getAddressId()),
            e.getStatus(),
            e.getCommissionRate(),
            e.getLastSeenAt(),
            e.getActivatedAt(),
            e.getBlockedAt(),
            UserId.nullableOf(e.getBlockedBy()),
            e.getBlockedReason(),
            e.getDisabledAt(),
            e.isMustChangePin(),
            e.getPinResetAt());
    }

    public void updateEntity(SellerTerminalJpaEntity e, SellerTerminal t) {
        e.setTenantId(t.tenantId().value());
        e.setTerminalCode(t.terminalCode());
        e.setFirstName(t.firstName());
        e.setLastName(t.lastName());
        e.setDisplayName(t.displayName());
        e.setEmail(t.email());
        e.setPhoneNumber(t.phoneNumber());
        e.setAddressId(t.addressId() != null ? t.addressId().value() : null);
        e.setStatus(t.status());
        e.setCommissionRate(t.commissionRate());
        e.setLastSeenAt(t.lastSeenAt());
        e.setActivatedAt(t.activatedAt());
        e.setBlockedAt(t.blockedAt());
        e.setBlockedBy(t.blockedBy() != null ? t.blockedBy().value() : null);
        e.setBlockedReason(t.blockedReason());
        e.setDisabledAt(t.disabledAt());
        e.setMustChangePin(t.mustChangePin());
        e.setPinResetAt(t.pinResetAt());
    }

    public SellerTerminalJpaEntity toNewEntity(SellerTerminal t) {
        var e = new SellerTerminalJpaEntity();
        e.setId(t.id().value());
        updateEntity(e, t);
        return e;
    }
}
