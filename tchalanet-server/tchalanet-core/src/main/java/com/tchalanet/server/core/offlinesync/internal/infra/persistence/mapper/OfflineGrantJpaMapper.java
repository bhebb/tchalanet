package com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.model.grant.GrantLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.grant.GrantQuota;
import com.tchalanet.server.core.offlinesync.api.model.grant.GrantValidityWindow;
import com.tchalanet.server.core.offlinesync.api.model.grant.OfflineGrantStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.GrantDevice;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.GrantIdentity;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineGrantJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OfflineGrantJpaMapper {

    public OfflineGrant toDomain(OfflineGrantJpaEntity e) {
        CurrencyCode currency = CurrencyCode.of(e.getCurrency());
        return new OfflineGrant(
            new GrantIdentity(
                OfflineGrantId.of(e.getId()),
                TenantId.of(e.getTenantId()),
                UserId.of(e.getSellerUserId()),
                TerminalId.of(e.getTerminalId()),
                OutletId.of(e.getOutletId()),
                SalesSessionId.of(e.getSalesSessionId())
            ),
            new GrantDevice(e.getDeviceId(), e.getDevicePublicKey(), e.getKeyId()),
            new GrantValidityWindow(e.getValidFrom(), e.getValidUntil(), e.getSyncAcceptedUntil()),
            new GrantQuota(
                e.getMaxTicketCount(),
                new Money(e.getMaxTotalAmount(), currency),
                e.getConsumedTicketCount(),
                new Money(e.getConsumedTotalAmount(), currency)
            ),
            new GrantLifecycle(
                OfflineGrantStatus.valueOf(e.getStatus()),
                e.getIssuedAt(),
                e.getRevokedAt(),
                e.getRevokedReason()
            )
        );
    }

    public OfflineGrantJpaEntity toEntity(OfflineGrant g, OfflineGrantJpaEntity target) {
        OfflineGrantJpaEntity e = target != null ? target : new OfflineGrantJpaEntity();
        e.setId(g.identity().id().value());
        e.setTenantId(g.identity().tenantId().value());
        e.setSellerUserId(g.identity().sellerUserId().value());
        e.setTerminalId(g.identity().terminalId().value());
        e.setOutletId(g.identity().outletId().value());
        e.setSalesSessionId(g.identity().salesSessionId().value());
        e.setDeviceId(g.device().deviceId());
        e.setDevicePublicKey(g.device().devicePublicKey());
        e.setKeyId(g.device().keyId());
        e.setStatus(g.lifecycle().status().name());
        e.setValidFrom(g.window().validFrom());
        e.setValidUntil(g.window().validUntil());
        e.setSyncAcceptedUntil(g.window().syncAcceptedUntil());
        e.setMaxTicketCount(g.quota().maxTicketCount());
        e.setMaxTotalAmount(g.quota().maxTotalAmount().amount());
        e.setCurrency(g.quota().maxTotalAmount().currency().value());
        e.setConsumedTicketCount(g.quota().consumedTicketCount());
        e.setConsumedTotalAmount(g.quota().consumedTotalAmount().amount());
        e.setIssuedAt(g.lifecycle().issuedAt());
        e.setRevokedAt(g.lifecycle().revokedAt());
        e.setRevokedReason(g.lifecycle().revokedReason());
        // token_hash is required by schema but is a legacy column — fall back to empty.
        if (e.getTokenHash() == null) e.setTokenHash("");
        return e;
    }
}
