package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.address.application.dto.AddressDto;
import java.util.UUID;

public record CreateOutletCommand(TenantId tenantId, String name, String slug, AddressDto address)
    implements Command<UUID> {}
