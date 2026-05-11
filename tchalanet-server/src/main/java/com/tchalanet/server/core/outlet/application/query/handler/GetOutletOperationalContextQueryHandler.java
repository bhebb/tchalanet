package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.port.out.OutletMembershipReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletTerminalReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletOperationalContextQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletOperationalContextView;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOutletOperationalContextQueryHandler
    implements QueryHandler<GetOutletOperationalContextQuery, OutletOperationalContextView> {

    private final OutletReaderPort outletReader;
    private final OutletMembershipReaderPort membershipReader;
    private final OutletTerminalReaderPort terminalReader;

    @Override
    public OutletOperationalContextView handle(GetOutletOperationalContextQuery query) {
        Outlet o = outletReader.getRequired(query.outletId());
        long userCount = membershipReader.countUsersByOutlet(o.id());
        long terminalCount = terminalReader.countTerminalsByOutlet(o.id());
        return new OutletOperationalContextView(
            o.id(),
            o.tenantId(),
            o.name(),
            o.slug(),
            o.dayClosed(),
            o.salesBlocked(),
            o.salesBlockReason(),
            o.salesBlockedAt(),
            o.timezone(),
            o.autoSessionOpenEnabled(),
            o.autoSessionCloseEnabled(),
            userCount,
            terminalCount,
            o.salesCapability());
    }
}
