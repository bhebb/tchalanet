package com.tchalanet.server.core.pos.application.port.out;

import com.tchalanet.server.core.pos.domain.model.Terminal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

public interface TerminalReaderPort {

  Optional<Terminal> findById(UUID tenantId, UUID terminalId);

  List<Terminal> listByOutlet(UUID tenantId, UUID outletId, PageRequest pageRequest);
}
