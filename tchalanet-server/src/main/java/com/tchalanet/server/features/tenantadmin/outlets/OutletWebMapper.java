package com.tchalanet.server.features.tenantadmin.outlets;

import com.tchalanet.server.core.outlet.application.query.model.OutletView;
import com.tchalanet.server.features.tenantadmin.outlets.model.OutletResponse;

public final class OutletWebMapper {

  private OutletWebMapper() {}

  public static OutletResponse toResponse(OutletView d) {
    if (d == null) return null;
    return new OutletResponse(d.id(), d.tenantId(), d.name(), d.slug(), d.dayClosed(), d.receiptPrintingEnabled());
  }
}
