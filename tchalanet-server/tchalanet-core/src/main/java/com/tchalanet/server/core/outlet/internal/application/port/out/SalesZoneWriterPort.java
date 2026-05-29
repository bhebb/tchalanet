package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.core.outlet.internal.domain.model.SalesZone;

public interface SalesZoneWriterPort {
    SalesZone save(SalesZone zone);
}
