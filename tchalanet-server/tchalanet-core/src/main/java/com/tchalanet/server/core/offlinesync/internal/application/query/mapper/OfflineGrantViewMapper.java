package com.tchalanet.server.core.offlinesync.internal.application.query.mapper;

import com.tchalanet.server.core.offlinesync.api.query.grant.OfflineGrantView;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;

public final class OfflineGrantViewMapper {

    private OfflineGrantViewMapper() {}

    public static OfflineGrantView toView(OfflineGrant grant) {
        return new OfflineGrantView(
            grant.identity().id(),
            grant.identity().sellerUserId(),
            grant.identity().terminalId(),
            grant.device().deviceId(),
            grant.lifecycle().status(),
            grant.window().validFrom(),
            grant.window().validUntil(),
            grant.window().syncAcceptedUntil(),
            grant.quota().maxTicketCount(),
            grant.quota().maxTotalAmount(),
            grant.quota().consumedTicketCount(),
            grant.quota().consumedTotalAmount(),
            grant.lifecycle().issuedAt()
        );
    }
}
