package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletLookupPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutletPersistenceAdapter
    implements OutletReaderPort, OutletWriterPort, OutletLookupPort {

    private final OutletSpringRepository repo;
    private final OutletPersistenceMapper mapper;
    private final TchContextResolver contextResolver;

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
        var existing = repo.findByTenantIdAndId(outlet.tenantId().value(), outlet.id().value());
        if (existing.isEmpty()) {
            repo.save(mapper.toEntity(outlet));
            return;
        }

        var entity = existing.get();
        assertImmutableFields(entity, outlet);
        mapper.updateEntity(outlet, entity);
    }

    @Override
    public void setStatus(OutletId outletId, OutletStatus status, String reason, Instant at, UserId performedBy) {
        var entity = getCurrentTenantOutlet(outletId);
        entity.setStatus(status);
    }

    @Override
    public void setSalesBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy) {
        if (blocked && (reason == null || reason.isBlank())) {
            throw ProblemRest.badRequest("outlet.sales_block_reason_required");
        }
        var entity = getCurrentTenantOutlet(outletId);
        entity.setSalesBlocked(blocked);
        entity.setSalesBlockReason(normalizeReason(blocked, reason));
        entity.setSalesBlockedAt(blocked ? at : null);
    }

    @Override
    public void setPayoutBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy) {
        var entity = getCurrentTenantOutlet(outletId);
        entity.setPayoutBlocked(blocked);
        entity.setPayoutBlockReason(normalizeReason(blocked, reason));
        entity.setPayoutBlockedAt(blocked ? at : null);
        entity.setPayoutBlockedBy(blocked ? performedBy.value() : null);
    }

    @Override
    public void setOfflineSalesBlocked(OutletId outletId, boolean blocked, String reason, Instant at, UserId performedBy) {
        var entity = getCurrentTenantOutlet(outletId);
        entity.setOfflineSalesBlocked(blocked);
        entity.setOfflineSalesBlockReason(normalizeReason(blocked, reason));
        entity.setOfflineSalesBlockedAt(blocked ? at : null);
        entity.setOfflineSalesBlockedBy(blocked ? performedBy.value() : null);
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

    private OutletJpaEntity getCurrentTenantOutlet(OutletId outletId) {
        UUID tenantId = contextResolver.currentOrThrow().effectiveTenantIdRequired().value();
        return repo.findByTenantIdAndId(tenantId, outletId.value())
            .orElseThrow(() -> new IllegalStateException(
                "Outlet update target not found: " + outletId.value()));
    }

    private static void assertImmutableFields(OutletJpaEntity entity, Outlet outlet) {
        requireSame("outletId", entity.getId(), outlet.id().value());
        requireSame("tenantId", entity.getTenantId(), outlet.tenantId().value());
        requireSame("slug", entity.getSlug(), outlet.slug());
    }

    private static void requireSame(String field, Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException(
                "Outlet immutable field changed: "
                    + field
                    + " expected="
                    + actual
                    + " actual="
                    + expected);
        }
    }

    private static String normalizeReason(boolean blocked, String reason) {
        if (!blocked) {
            return null;
        }
        return reason == null ? null : reason.trim();
    }
}
