package com.tchalanet.server.features.tenantadmin.outlets.model;

import java.util.UUID;

public record OutletResponse(
    UUID id,
    UUID tenantId,
    String name,
    String slug,
    Boolean dayClosed,
    Boolean receiptPrintingEnabled
) {}
