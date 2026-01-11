package com.tchalanet.server.core.resultslot.api;

import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.resultslot.internal.port.out.ResultSlotReaderPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/** Facade publique pour consulter les ResultSlots globalement. */
@Component
@RequiredArgsConstructor
public class ResultSlotCatalog {

  private final ResultSlotReaderPort reader;

  @Cacheable(cacheNames = "resultslot.active", key = "'v1'")
  public List<ResultSlotView> listActive() {
    return reader.listActive();
  }

  @Cacheable(cacheNames = "resultslot.byKey", key = "#slotKey")
  public Optional<ResultSlotView> findBySlotKey(String slotKey) {
    if (slotKey == null || slotKey.isBlank()) return Optional.empty();
    return reader.findBySlotKey(slotKey.trim().toUpperCase());
  }

  @Cacheable(cacheNames = "resultslot.id", key = "#id == null ? '' : #id.toString()")
  public Optional<ResultSlotView> findById(ResultSlotId id) {
    if (id == null) return Optional.empty();
    return reader.findById(id.uuid());
  }
}
