package com.tchalanet.server.catalog.resultslot.internal.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.resultslot.cache.ResultSlotCacheNames;
import com.tchalanet.server.catalog.resultslot.internal.ResultSlotCatalogImpl.NotFoundException;
import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotJpaEntity;
import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotJpaRepository;
import java.time.Instant;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResultSlotAdminService {

  private final ResultSlotJpaRepository repo;

  @Transactional
  @CacheEvict(cacheNames = {ResultSlotCacheNames.ACTIVE, ResultSlotCacheNames.BY_KEY, ResultSlotCacheNames.BY_ID}, allEntries = true)
  public ResultSlotJpaEntity create(CreateResultSlotRequest req) {
    var e = new ResultSlotJpaEntity();
    apply(req, e);
    // active par défaut si null
    e.setActive(req.active() == null || req.active());
    return repo.save(e);
  }

  @Transactional
  @CacheEvict(cacheNames = {ResultSlotCacheNames.ACTIVE, ResultSlotCacheNames.BY_KEY, ResultSlotCacheNames.BY_ID}, allEntries = true)
  public ResultSlotJpaEntity update(java.util.UUID id, UpdateResultSlotRequest req) {
    var e =
        repo.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("result_slot_not_found", "id=" + id));
    apply(req, e);
    if (req.active() != null) e.setActive(req.active());
    return repo.save(e);
  }

  @Transactional
  @CacheEvict(cacheNames = {ResultSlotCacheNames.ACTIVE, ResultSlotCacheNames.BY_KEY, ResultSlotCacheNames.BY_ID}, allEntries = true)
  public void softDelete(java.util.UUID id) {
    var e =
        repo.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("result_slot_not_found", "id=" + id));
    e.setDeletedAt(Instant.now()); // BaseEntity should have deletedAt
    repo.save(e);
  }

  private static void apply(BaseResultSlotRequest req, ResultSlotJpaEntity e) {
    if (req.slotKey() != null) e.setSlotKey(req.slotKey().trim().toUpperCase());
    if (req.provider() != null) e.setProvider(req.provider().trim().toUpperCase());
    if (req.timezone() != null) e.setTimezone(req.timezone().trim());
    if (req.drawTime() != null) e.setDrawTime(req.drawTime());
    if (req.daysOfWeek() != null) e.setDaysOfWeek(req.daysOfWeek().trim());
    if (req.sortOrder() != null) e.setSortOrder(req.sortOrder());
    if (req.sourceCfg() != null) e.setSourceCfg(req.sourceCfg());
    if (req.projectionCfg() != null) e.setProjectionCfg(req.projectionCfg());
    if (req.notes() != null) e.setNotes(req.notes());
    if (req.labelKey() != null) e.setLabelKey(req.labelKey());
  }

  public sealed interface BaseResultSlotRequest permits CreateResultSlotRequest, UpdateResultSlotRequest {
    String slotKey();
    String provider();
    String timezone();
    LocalTime drawTime();
    String daysOfWeek();
    Integer sortOrder();
    JsonNode sourceCfg();
    JsonNode projectionCfg();
    String notes();
    String labelKey();
    Boolean active();
  }

  public record CreateResultSlotRequest(
      String slotKey,
      String provider,
      String timezone,
      LocalTime drawTime,
      String daysOfWeek,
      Integer sortOrder,
      JsonNode sourceCfg,
      JsonNode projectionCfg,
      String notes,
      String labelKey,
      Boolean active
  ) implements BaseResultSlotRequest {}

  public record UpdateResultSlotRequest(
      String slotKey,
      String provider,
      String timezone,
      LocalTime drawTime,
      String daysOfWeek,
      Integer sortOrder,
      JsonNode sourceCfg,
      JsonNode projectionCfg,
      String notes,
      String labelKey,
      Boolean active
  ) implements BaseResultSlotRequest {}
}
