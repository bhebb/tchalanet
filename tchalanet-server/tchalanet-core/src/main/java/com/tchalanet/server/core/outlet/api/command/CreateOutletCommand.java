package com.tchalanet.server.core.outlet.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.address.application.model.AddressInput;

public record CreateOutletCommand(
    TenantId tenantId,
    String name,
    String slug,
    AddressId addressId,
    AddressInput addressInput
) implements Command<OutletId> {}
