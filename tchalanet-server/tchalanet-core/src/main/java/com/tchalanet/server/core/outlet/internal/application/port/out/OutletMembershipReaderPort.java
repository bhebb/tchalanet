package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.outlet.api.query.OutletUserView;
import java.util.List;

/** Cross-domain read-side port. Implemented by {@code core.tenantuser}. */
public interface OutletMembershipReaderPort {

  /** Lists active users assigned to the given outlet (via tenant_user.outlet_id). */
  List<OutletUserView> listUsersByOutlet(OutletId outletId);

  /** Counts active users assigned to the given outlet. */
  long countUsersByOutlet(OutletId outletId);
}
