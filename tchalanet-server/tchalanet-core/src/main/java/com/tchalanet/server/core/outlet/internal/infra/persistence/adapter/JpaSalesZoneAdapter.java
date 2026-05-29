package com.tchalanet.server.core.outlet.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesZoneReaderPort;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesZoneWriterPort;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesZone;
import com.tchalanet.server.core.outlet.internal.infra.persistence.SalesZonePersistenceMapper;
import com.tchalanet.server.core.outlet.internal.infra.persistence.SalesZoneSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaSalesZoneAdapter implements SalesZoneReaderPort, SalesZoneWriterPort {

    private final SalesZoneSpringRepository repo;
    private final SalesZonePersistenceMapper mapper;

    @Override
    public Optional<SalesZone> findById(TenantId tenantId, SalesZoneId zoneId) {
        return repo.findByTenantIdAndId(tenantId.value(), zoneId.value())
            .map(mapper::toDomain);
    }

    @Override
    public List<SalesZone> findAllByTenant(TenantId tenantId) {
        return repo.findAllByTenantIdOrderByCodeAsc(tenantId.value()).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public SalesZone save(SalesZone zone) {
        var existing = repo.findByTenantIdAndId(zone.tenantId().value(), zone.id().value());

        if (existing.isEmpty()) {
            var entity = mapper.toEntity(zone);
            return mapper.toDomain(repo.save(entity));
        }

        var entity = existing.get();
        mapper.updateEntity(zone, entity);
        return mapper.toDomain(repo.save(entity));
    }
}
