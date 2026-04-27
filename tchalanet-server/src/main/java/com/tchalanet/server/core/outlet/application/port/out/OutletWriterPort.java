package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.core.outlet.domain.model.Outlet;

public interface OutletWriterPort {
  void save(Outlet outlet);
}
