package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import org.springframework.stereotype.Service;


@Service
public class OutletMembershipPortImpl implements OutletMembershipPort {
    @Override
    public void assignUserToOutlet(OutletId outletId, UserId userId) {

    }

    @Override
    public void removeUserFromOutlet(OutletId outletId, UserId userId) {

    }
}
