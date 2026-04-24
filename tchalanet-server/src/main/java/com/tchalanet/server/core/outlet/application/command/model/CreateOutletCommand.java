package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.address.application.model.AddressInput;
import java.util.UUID;

public record CreateOutletCommand(
    TenantId tenantId,
    String name,
    String slug,
    AddressId addressId,
    AddressInput addressInput
) implements Command<UUID> {}
