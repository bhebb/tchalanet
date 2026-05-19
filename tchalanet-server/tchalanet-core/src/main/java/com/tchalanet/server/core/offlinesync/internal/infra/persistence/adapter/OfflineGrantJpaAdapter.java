package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.offlinesync.api.model.grant.OfflineGrantStatus;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.grant.OfflineGrant;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineGrantJpaRepository;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.mapper.OfflineGrantJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OfflineGrantJpaAdapter implements OfflineGrantReaderPort, OfflineGrantWriterPort {

    private final OfflineGrantJpaRepository repo;
    private final OfflineGrantJpaMapper mapper;

    @Override
    public Optional<OfflineGrant> findById(OfflineGrantId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public OfflineGrant getRequired(OfflineGrantId id) {
        return findById(id)
            .orElseThrow(() -> ProblemRest.notFound("offlinesync.grant.not_found", id));
    }

    @Override
    public Optional<OfflineGrant> findCurrentActive(UserId sellerUserId, TerminalId terminalId, UUID deviceId) {
        return repo.findFirstBySellerUserIdAndTerminalIdAndDeviceIdAndStatusOrderByIssuedAtDesc(
            sellerUserId.value(), terminalId.value(), deviceId, OfflineGrantStatus.ACTIVE.name()
        ).map(mapper::toDomain);
    }

    @Override
    public OfflineGrant save(OfflineGrant grant) {
        var existing = repo.findById(grant.identity().id().value()).orElse(null);
        var entity = mapper.toEntity(grant, existing);
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<OfflineGrant> lockForUpdate(OfflineGrantId id) {
        return repo.findOneByIdForUpdate(id.value()).map(mapper::toDomain);
    }
}
