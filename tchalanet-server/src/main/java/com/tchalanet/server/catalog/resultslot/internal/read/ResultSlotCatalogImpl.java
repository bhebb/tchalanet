package com.tchalanet.server.catalog.resultslot.internal.read;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotStatsView;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.catalog.resultslot.internal.cache.ResultSlotCacheNames;
import com.tchalanet.server.catalog.resultslot.internal.mapper.ResultSlotMapper;
import com.tchalanet.server.catalog.resultslot.internal.persistence.ResultSlotJpaRepository;
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
public class ResultSlotCatalogImpl implements ResultSlotCatalog {

    private final ResultSlotJpaRepository repo;
    private final ResultSlotMapper mapper;

    @Override
    @Cacheable(cacheNames = ResultSlotCacheNames.ACTIVE)
    public List<ResultSlotView> listActive() {
        var entities = repo.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();
        return mapper.toViews(entities);
    }

    @Override
    @Cacheable(cacheNames = ResultSlotCacheNames.BY_KEY, key = "#slotKey == null ? '' : #slotKey.toLowerCase()")
    public Optional<ResultSlotView> findByKey(String slotKey) {
        if (slotKey == null || slotKey.isBlank()) {
            return Optional.empty();
        }
        return repo.findFirstBySlotKeyIgnoreCaseAndDeletedAtIsNull(slotKey).map(mapper::toView);
    }

    @Override
    public ResultSlotView requireByKey(String slotKey) {
        return findByKey(slotKey).orElseThrow(() -> new EntityNotFoundException("result_slot_not_found with slotKey=" + slotKey));
    }

    @Override
    @Cacheable(cacheNames = ResultSlotCacheNames.BY_ID, key = "#id == null ? '' : #id.toString()")
    public Optional<ResultSlotView> findById(ResultSlotId id) {
        if (id == null) {
            return Optional.empty();
        }
        UUID uuid = id.value();
        return repo.findByIdAndDeletedAtIsNull(uuid).map(mapper::toView);
    }

    @Override
    public ResultSlotStatsView stats() {
        long total = repo.countByDeletedAtIsNull();
        long active = repo.countByActiveTrueAndDeletedAtIsNull();
        return new ResultSlotStatsView((int) total, (int) active);
    }
}
