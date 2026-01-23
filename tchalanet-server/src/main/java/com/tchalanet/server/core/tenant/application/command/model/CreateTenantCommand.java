package com.tchalanet.server.core.tenant.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.TenantType;
import com.tchalanet.server.catalog.address.application.dto.AddressDto;
import java.util.UUID;

public record CreateTenantCommand(
    String code, String name, TenantType type, String timezone, String currency, AddressDto address)
    implements Command<UUID> {}
