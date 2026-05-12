package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.common.paging.TchPageMapper;
import com.tchalanet.server.core.outlet.application.port.out.OutletLookupPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.application.query.model.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.application.query.model.OutletSummaryView;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import com.tchalanet.server.core.outlet.domain.model.OutletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OutletPersistenceAdapter
    implements OutletReaderPort, OutletWriterPort, OutletLookupPort {

    private final OutletSpringRepository repo;
    private final OutletPersistenceMapper mapper;

    @Override
    public Optional<Outlet> findById(OutletId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<OutletSummaryView> listSummariesByTenant() {
        return repo.findAll().stream()
            .map(mapper::toSummaryView)
            .toList();
    }

    @Override
    public void save(Outlet outlet) {
        var entity = repo.findById(outlet.id().value()).orElseGet(OutletJpaEntity::new);
        mapper.updateEntity(outlet, entity);
        repo.save(entity);
    }

    @Override
    public void setStatus(OutletId outletId, OutletStatus status, String reason, Instant at, UserId performedBy) {

    }

    @Override
    public void setSalesBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy) {

    }

    @Override
    public void setPayoutBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy) {

    }

    @Override
    public void setOfflineSalesBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy) {

    }

    @Override
    public boolean isSalesBlocked(OutletId outletId) {
        return repo.findById(outletId.value())
            .map(OutletJpaEntity::isSalesBlocked)
            .orElse(false);
    }

    @Override
    public TchPage<OutletSummaryView> search(OutletSearchCriteria criteria, Pageable pageable) {
        Page<OutletJpaEntity> outlets = repo.findAll(OutletSpecifications.matching(criteria), pageable);
        return TchPageMapper.map(outlets, mapper::toSummaryView);
    }

}
