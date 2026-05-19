package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionJpaRepository;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper.OfflineSubmissionJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OfflineSubmissionJpaAdapter implements OfflineSubmissionReaderPort, OfflineSubmissionWriterPort {

    private final OfflineSubmissionJpaRepository repo;
    private final OfflineSubmissionJpaMapper mapper;

    @Override
    public Optional<OfflineSubmission> findById(OfflineSubmissionId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public OfflineSubmission getRequired(OfflineSubmissionId id) {
        return findById(id).orElseThrow(() ->
            com.tchalanet.server.common.web.error.ProblemRest.notFound(
                "offlinesync.submission.not_found", id));
    }

    @Override
    public Optional<OfflineSubmission> findByClientSubmissionId(
        TenantId tenantId, OfflineGrantId grantId, String clientSubmissionId
    ) {
        return repo
            .findByTenantIdAndGrantIdAndClientSubmissionId(
                tenantId.value(), grantId.value(), clientSubmissionId)
            .map(mapper::toDomain);
    }

    @Override
    public List<OfflineSubmission> listForSeller(TenantId tenantId, UserId sellerUserId, int limit) {
        int capped = Math.max(1, Math.min(limit, 200));
        return repo
            .findAllByTenantIdAndSellerUserIdOrderByReceivedAtDesc(
                tenantId.value(), sellerUserId.value(), PageRequest.of(0, capped))
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public OfflineSubmission save(OfflineSubmission submission) {
        var existing = repo.findById(submission.identity().id().value()).orElse(null);
        var entity = mapper.toEntity(submission, existing);
        return mapper.toDomain(repo.save(entity));
    }
}
