package com.tchalanet.server.catalog.resultslot.internal.write;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCalendarOverrideView;
import com.tchalanet.server.catalog.resultslot.internal.cache.ResultSlotCalendarCacheNames;
import com.tchalanet.server.catalog.resultslot.internal.mapper.ResultSlotCalendarOverrideMapper;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotCalendarOverrideJpaEntity;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotCalendarOverrideJpaRepository;
import com.tchalanet.server.common.types.id.ResultSlotCalendarOverrideId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.catalog.resultslot.internal.web.model.CreateResultSlotCalendarOverrideRequest;
import com.tchalanet.server.catalog.resultslot.internal.web.model.UpdateResultSlotCalendarOverrideRequest;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SUPER_ADMIN writes for the global provider calendar (24h-cached on read). */
@Service
@RequiredArgsConstructor
public class ResultSlotCalendarAdminService {

  private final ResultSlotCalendarOverrideJpaRepository repo;
  private final ResultSlotCalendarOverrideMapper mapper;

  @Transactional
  @CacheEvict(cacheNames = ResultSlotCalendarCacheNames.BY_SLOT, allEntries = true)
  public ResultSlotCalendarOverrideView create(
      ResultSlotId resultSlotId, CreateResultSlotCalendarOverrideRequest req) {
    if (resultSlotId == null) {
      throw new IllegalArgumentException("resultSlotId is required");
    }
    requireExactlyOneShape(req.slotLocalDate(), req.recurringMd());

    var e = new ResultSlotCalendarOverrideJpaEntity();
    e.setResultSlotId(resultSlotId.value());
    e.setSlotLocalDate(req.slotLocalDate());
    e.setRecurringMd(req.recurringMd());
    e.setAvailable(req.available());
    e.setReasonCode(req.reasonCode().trim());
    e.setReasonLabel(blankToNull(req.reasonLabel()));
    return mapper.toView(repo.save(e));
  }

  @Transactional
  @CacheEvict(cacheNames = ResultSlotCalendarCacheNames.BY_SLOT, allEntries = true)
  public ResultSlotCalendarOverrideView update(
      ResultSlotCalendarOverrideId id, UpdateResultSlotCalendarOverrideRequest req) {
    var e = load(id);
    e.setAvailable(req.available());
    e.setReasonCode(req.reasonCode().trim());
    e.setReasonLabel(blankToNull(req.reasonLabel()));
    return mapper.toView(repo.save(e));
  }

  @Transactional
  @CacheEvict(cacheNames = ResultSlotCalendarCacheNames.BY_SLOT, allEntries = true)
  public void softDelete(ResultSlotCalendarOverrideId id) {
    var e = load(id);
    e.setDeletedAt(Instant.now());
    repo.save(e);
  }

  private ResultSlotCalendarOverrideJpaEntity load(ResultSlotCalendarOverrideId id) {
    var uuid = (id == null) ? null : id.value();
    return repo.findByIdAndDeletedAtIsNull(uuid)
        .orElseThrow(() -> new EntityNotFoundException("result_slot_calendar_override_not_found"));
  }

  private static void requireExactlyOneShape(java.time.LocalDate date, String recurringMd) {
    boolean hasDate = date != null;
    boolean hasMd = recurringMd != null && !recurringMd.isBlank();
    if (hasDate == hasMd) {
      throw new IllegalArgumentException(
          "Provide exactly one of slotLocalDate or recurringMd");
    }
  }

  private static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
  }
}
