package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.core.address.application.model.AddressView;
import java.util.UUID;

public record OutletView(
    UUID id,
    UUID tenantId,
    String name,
    String slug,
    Boolean dayClosed,
    Boolean receiptPrintingEnabled,
    AddressView address) {}
