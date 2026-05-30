package com.tchalanet.server.catalog.resultslot.internal.read;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarOverrideView;
import com.tchalanet.server.catalog.resultslot.internal.cache.ResultSlotCalendarCacheNames;
import com.tchalanet.server.catalog.resultslot.internal.mapper.ResultSlotCalendarOverrideMapper;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotCalendarOverrideJpaRepository;
import com.tchalanet.server.common.types.id.ResultSlotId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Read-only, cacheable (24h) access to the global provider calendar. */
@Service
@RequiredArgsConstructor
public class ResultSlotCalendarCatalogImpl implements ResultSlotCalendarCatalog {

  private final ResultSlotCalendarOverrideJpaRepository repo;
  private final ResultSlotCalendarOverrideMapper mapper;

  @Override
  @Cacheable(
      cacheNames = ResultSlotCalendarCacheNames.BY_SLOT,
      key = "#resultSlotId == null ? '' : #resultSlotId.toString()")
  public List<ResultSlotCalendarOverrideView> listBySlot(ResultSlotId resultSlotId) {
    if (resultSlotId == null) {
      return List.of();
    }
    return mapper.toViews(
        repo.findByResultSlotIdAndDeletedAtIsNullOrderBySlotLocalDateAscRecurringMdAsc(
            resultSlotId.value()));
  }
}
