package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.SalesZoneId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSalesZoneRequest(
    @NotBlank @Size(max = 80) String code,
    @NotBlank @Size(max = 160) String label,
    SalesZoneId parentId) {}
