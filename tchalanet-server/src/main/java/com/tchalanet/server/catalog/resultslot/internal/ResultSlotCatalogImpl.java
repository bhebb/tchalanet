package com.tchalanet.server.catalog.resultslot.internal;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.cache.ResultSlotCacheNames;
import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotJpaEntity;
import com.tchalanet.server.catalog.resultslot.internal.infra.persistence.ResultSlotJpaRepository;
import com.tchalanet.server.common.types.id.ResultSlotId;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ResultSlotCatalogImpl implements ResultSlotCatalog {

    private final ResultSlotJpaRepository repo;

    @Override
    @Cacheable(cacheNames = ResultSlotCacheNames.ACTIVE)
    public List<ResultSlotView> listActive() {
        return repo.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc().stream()
            .map(ResultSlotCatalogImpl::toView)
            .toList();
    }

    @Override
    @Cacheable(cacheNames = ResultSlotCacheNames.BY_KEY, key = "#slotKey == null ? '' : #slotKey.toLowerCase()")
    public Optional<ResultSlotView> findByKey(String slotKey) {
        if (slotKey == null || slotKey.isBlank()) return Optional.empty();
        return repo.findFirstBySlotKeyIgnoreCaseAndDeletedAtIsNull(slotKey).map(ResultSlotCatalogImpl::toView);
    }

    @Override
    public ResultSlotView requireByKey(String slotKey) {
        return findByKey(slotKey).orElseThrow(() -> new EntityNotFoundException("result_slot_not_found with slotKey=" + slotKey));
    }

    @Override
    @Cacheable(cacheNames = ResultSlotCacheNames.BY_ID, key = "#id == null ? '' : #id.toString()")
    public Optional<ResultSlotView> findById(ResultSlotId id) {
        if (id == null) return Optional.empty();
        UUID uuid = id.value();
        return repo.findByIdAndDeletedAtIsNull(uuid).map(ResultSlotCatalogImpl::toView);
    }

    private static ResultSlotView toView(ResultSlotJpaEntity e) {
        return new ResultSlotView(
            ResultSlotId.of(e.getId()),
            e.getSlotKey(),
            e.getProvider(),
            java.time.ZoneId.of(e.getTimezone()),
            e.getDrawTime(),
            e.getDaysOfWeek(),
            e.isActive(),
            e.getSourceCfg(),
            e.getProjectionCfg(),
            e.getLabelKey());
    }
}
