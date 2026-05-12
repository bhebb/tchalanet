package com.tchalanet.server.catalog.resultslot.internal.write;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.cache.ResultSlotCacheNames;
import com.tchalanet.server.catalog.resultslot.internal.mapper.ResultSlotMapper;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotJpaEntity;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotJpaRepository;
import com.tchalanet.server.catalog.resultslot.internal.web.model.CreateResultSlotRequest;
import com.tchalanet.server.catalog.resultslot.internal.web.model.UpdateResultSlotRequest;
import com.tchalanet.server.common.types.id.ResultSlotId;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResultSlotAdminService {

    private final ResultSlotJpaRepository repo;
    private final ResultSlotMapper mapper;

    @Transactional
    @CacheEvict(cacheNames = {ResultSlotCacheNames.ACTIVE, ResultSlotCacheNames.BY_KEY, ResultSlotCacheNames.BY_ID}, allEntries = true)
    public ResultSlotView create(CreateResultSlotRequest req) {
        var e = new ResultSlotJpaEntity();
        apply(req, e);
        e.setActive(req.active() == null || req.active());
        var saved = repo.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {ResultSlotCacheNames.ACTIVE, ResultSlotCacheNames.BY_KEY, ResultSlotCacheNames.BY_ID}, allEntries = true)
    public ResultSlotView update(ResultSlotId id, UpdateResultSlotRequest req) {
        java.util.UUID uuid = (id == null) ? null : id.value();
        var e =
            repo.findByIdAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("result_slot_not_found"));
        apply(req, e);
        if (req.active() != null) e.setActive(req.active());
        var saved = repo.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {ResultSlotCacheNames.ACTIVE, ResultSlotCacheNames.BY_KEY, ResultSlotCacheNames.BY_ID}, allEntries = true)
    public void softDelete(ResultSlotId id) {
        java.util.UUID uuid = (id == null) ? null : id.value();
        var e =
            repo.findByIdAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("result_slot_not_found"));
        e.setDeletedAt(Instant.now()); // BaseEntity should have deletedAt
        repo.save(e);
    }

    private static void apply(com.tchalanet.server.catalog.resultslot.internal.web.model.BaseResultSlotRequest req, ResultSlotJpaEntity e) {
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
}
