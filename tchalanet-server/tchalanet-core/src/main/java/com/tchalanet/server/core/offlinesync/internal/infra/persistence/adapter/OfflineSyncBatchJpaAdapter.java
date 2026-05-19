package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSyncBatchReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSyncBatchWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.syncbatch.OfflineSyncBatch;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSyncBatchJpaRepository;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper.OfflineSyncBatchJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OfflineSyncBatchJpaAdapter implements OfflineSyncBatchReaderPort, OfflineSyncBatchWriterPort {

    private final OfflineSyncBatchJpaRepository repo;
    private final OfflineSyncBatchJpaMapper mapper;

    @Override
    public Optional<OfflineSyncBatch> findById(OfflineSyncBatchId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public OfflineSyncBatch getRequired(OfflineSyncBatchId id) {
        return findById(id).orElseThrow(() ->
            new TchNotFoundException(id.toString(), "offlinesync.sync_batch.not_found"));
    }

    @Override
    public Optional<OfflineSyncBatch> findByClientBatchId(
        TenantId tenantId, OfflineGrantId grantId, String clientBatchId
    ) {
        return repo.findByTenantIdAndGrantIdAndClientBatchId(
            tenantId.value(), grantId.value(), clientBatchId).map(mapper::toDomain);
    }

    @Override
    public OfflineSyncBatch save(OfflineSyncBatch batch) {
        var existing = repo.findById(batch.identity().id().value()).orElse(null);
        var entity = mapper.toEntity(batch, existing);
        return mapper.toDomain(repo.save(entity));
    }
}
