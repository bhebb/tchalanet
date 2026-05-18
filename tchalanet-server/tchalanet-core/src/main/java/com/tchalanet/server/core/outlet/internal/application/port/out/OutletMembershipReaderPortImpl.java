package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.outlet.api.query.OutletUserView;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class OutletMembershipReaderPortImpl implements OutletMembershipReaderPort {
    @Override
    public List<OutletUserView> listUsersByOutlet(OutletId outletId) {
        return List.of();
    }

    @Override
    public long countUsersByOutlet(OutletId outletId) {
        return 0;
    }
}
