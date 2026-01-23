package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.catalog.address.application.dto.AddressDto;

public record OutletConfigPatch(
    Boolean salesBlocked,
    String salesBlockReason,
    String timezone,
    String businessDayCutoff,
    Boolean receiptPrintingEnabled,
    String receiptHeaderMessage,
    String receiptFooterMessage,
    Boolean requireOpeningFloat,
    AddressDto address) {}
