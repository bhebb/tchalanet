package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeBatchReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeBatchWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.codebatch.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineCodeBatchJpaRepository;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper.OfflineCodeBatchJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OfflineCodeBatchJpaAdapter implements OfflineCodeBatchReaderPort, OfflineCodeBatchWriterPort {

    private final OfflineCodeBatchJpaRepository repo;
    private final OfflineCodeBatchJpaMapper mapper;

    @Override
    public Optional<OfflineCodeBatch> findById(OfflineCodeBatchId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public OfflineCodeBatch getRequired(OfflineCodeBatchId id) {
        return findById(id).orElseThrow(() ->
            new TchNotFoundException(id.toString(), "offlinesync.code_batch.not_found"));
    }

    @Override
    public OfflineCodeBatch save(OfflineCodeBatch batch) {
        var existing = repo.findById(batch.identity().id().value()).orElse(null);
        var entity = mapper.toEntity(batch, existing);
        return mapper.toDomain(repo.save(entity));
    }
}
