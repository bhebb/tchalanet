package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;
import com.tchalanet.server.platform.address.api.model.AddressInput;
import jakarta.validation.constraints.NotBlank;

public record CreateOutletRequest(
    // name and slug back NOT NULL columns — validate so a missing value is a clean
    // 400 instead of a 500 (DataIntegrityViolation) at insert time.
    @NotBlank String name,
    @NotBlank String slug,
    OutletKind kind,
    String partnerRef,
    SalesZoneId zoneId,
    AddressInput address) {}
