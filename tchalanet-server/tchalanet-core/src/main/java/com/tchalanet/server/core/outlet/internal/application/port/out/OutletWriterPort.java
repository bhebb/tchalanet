package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;

public interface OutletWriterPort {
    void save(Outlet outlet);
}
