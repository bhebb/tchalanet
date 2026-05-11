package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.port.out.OutletMembershipReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletUsersQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletUserView;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class ListOutletUsersQueryHandler
    implements QueryHandler<ListOutletUsersQuery, List<OutletUserView>> {

    private final OutletReaderPort outletReader;
    private final OutletMembershipReaderPort membershipReader;

    @Override
    public List<OutletUserView> handle(ListOutletUsersQuery query) {
        outletReader.getRequired(query.outletId());
        return membershipReader.listUsersByOutlet(query.outletId());
    }
}
