package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.code.OfflineCode;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineCodeJpaRepository;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper.OfflineCodeJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OfflineCodeJpaAdapter implements OfflineCodeReaderPort, OfflineCodeWriterPort {

    private final OfflineCodeJpaRepository repo;
    private final OfflineCodeJpaMapper mapper;

    @Override
    public Optional<OfflineCode> findById(OfflineCodeId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public OfflineCode getRequired(OfflineCodeId id) {
        return findById(id).orElseThrow(() ->
            com.tchalanet.server.common.web.error.ProblemRest.notFound(
                "offlinesync.code.not_found", id));
    }

    @Override
    public Optional<OfflineCode> findByCode(TenantId tenantId, OfflineGrantId grantId, String code) {
        return repo.findByTenantIdAndGrantIdAndCode(tenantId.value(), grantId.value(), code)
            .map(mapper::toDomain);
    }

    @Override
    public OfflineCode save(OfflineCode code) {
        var existing = repo.findById(code.identity().id().value()).orElse(null);
        var entity = mapper.toEntity(code, existing);
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<OfflineCode> lockForReservation(OfflineCodeId id) {
        return repo.findOneById(id.value()).map(mapper::toDomain);
    }
}
