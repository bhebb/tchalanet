package com.tchalanet.server.core.resultslot.internal.port.out;

import com.tchalanet.server.core.resultslot.api.ResultSlotView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Internal port used by core.resultslot module to read ResultSlot data. Not for external use. */
public interface ResultSlotReaderPort {
  Optional<ResultSlotView> findBySlotKey(String slotKey);

  List<ResultSlotView> listActive();

  Optional<ResultSlotView> findById(UUID id);
}
