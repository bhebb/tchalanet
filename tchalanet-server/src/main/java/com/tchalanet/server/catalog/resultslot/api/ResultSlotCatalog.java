package com.tchalanet.server.catalog.resultslot.api;

import com.tchalanet.server.common.types.id.ResultSlotId;
import java.util.List;
import java.util.Optional;

/** Public contract for accessing ResultSlot catalog (read-only). */
public interface ResultSlotCatalog {

  List<ResultSlotView> listActive();

  Optional<ResultSlotView> findByKey(String slotKey);

  ResultSlotView requireByKey(String slotKey);

  Optional<ResultSlotView> findById(ResultSlotId id);
}
