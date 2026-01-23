package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.catalog.address.application.dto.AddressDto;
import java.util.UUID;

public record OutletDto(
    UUID id,
    UUID tenantId,
    String name,
    String slug,
    Boolean dayClosed,
    Boolean receiptPrintingEnabled,
    AddressDto address) {}
