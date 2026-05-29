package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;
import com.tchalanet.server.platform.address.api.model.AddressInput;

public record CreateOutletRequest(
    String name,
    String slug,
    OutletKind kind,
    String partnerRef,
    SalesZoneId zoneId,
    AddressInput address) {}
