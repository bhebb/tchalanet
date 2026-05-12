package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.outlet.api.query.OutletTerminalView;
import java.util.List;

/** Cross-domain read-side port. Implemented by {@code core.terminal}. */
public interface OutletTerminalReaderPort {

  /** Lists all (non-deleted) terminals attached to the given outlet. */
  List<OutletTerminalView> listTerminalsByOutlet(OutletId outletId);

  /** Counts active terminals attached to the given outlet. */
  long countTerminalsByOutlet(OutletId outletId);
}
