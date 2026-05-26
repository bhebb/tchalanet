package com.tchalanet.server.core.outlet.internal.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletLookupPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import com.tchalanet.server.core.outlet.internal.infra.persistence.OutletPersistenceMapper;
import com.tchalanet.server.core.outlet.internal.infra.persistence.OutletSpecifications;
import com.tchalanet.server.core.outlet.internal.infra.persistence.OutletSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public int countActiveByTenant(TenantId tenantId) {
        return Math.toIntExact(repo.countByTenantIdAndStatus(tenantId.value(), OutletStatus.ACTIVE));
    }

    @Override
    public TchPage<OutletSummaryView> search(OutletSearchCriteria criteria, Pageable pageable) {
        Page page = repo.findAll(OutletSpecifications.matching(criteria), pageable);
        return TchPageMapper.map(page, mapper::toSummaryView);
    }

    private static void assertImmutableFields(
        com.tchalanet.server.core.outlet.internal.infra.persistence.OutletJpaEntity entity,
        Outlet outlet) {
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
}
