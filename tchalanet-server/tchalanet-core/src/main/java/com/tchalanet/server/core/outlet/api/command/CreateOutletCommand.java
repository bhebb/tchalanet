package com.tchalanet.server.core.outlet.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;
import com.tchalanet.server.platform.address.api.model.AddressInput;

public record CreateOutletCommand(
    TenantId tenantId,
    String name,
    String slug,
    OutletKind kind,
    String partnerRef,
    SalesZoneId zoneId,
    AddressId addressId,
    AddressInput addressInput
) implements Command<OutletId> {}
