package com.tchalanet.server.core.pos.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

public interface TerminalReaderPort {

  Optional<Terminal> findById(TenantId tenantId, TerminalId terminalId);

  List<Terminal> listByOutlet(TenantId tenantId, OutletId outletId, PageRequest pageRequest);

  // list all terminals for a tenant (paged)
  List<Terminal> listByTenant(TenantId tenantId, PageRequest pageRequest);
}
