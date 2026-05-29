package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import jakarta.validation.constraints.Size;

public record UpdateSalesZoneRequest(
    @Size(max = 160) String label,
    Boolean active) {}
