package com.tchalanet.server.core.terminal.internal.infra.persistence.sellerterminal;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.terminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.sellerterminal.SellerTerminal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaSellerTerminalAdapter implements SellerTerminalReaderPort, SellerTerminalWriterPort {

    private final SellerTerminalJpaRepository repository;
    private final SellerTerminalMapper mapper;

    @Override
    public SellerTerminal save(SellerTerminal terminal) {
        var entity = repository.findById(terminal.id().value())
            .orElseGet(() -> mapper.toNewEntity(terminal));
        mapper.updateEntity(entity, terminal);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<SellerTerminal> findById(TenantId tenantId, SellerTerminalId id) {
        return repository.findByTenantIdAndId(tenantId.value(), id.value())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<SellerTerminal> findByExternalSubject(String provider, String issuer, String externalSubject) {
        return repository.findByExternalSubject(provider, issuer, externalSubject)
            .map(mapper::toDomain);
    }

    @Override
    public TchPage<SellerTerminalSummaryRow> search(
        TenantId tenantId,
        SellerTerminalSearchCriteria criteria,
        TchPageRequest pageRequest) {

        Specification<SellerTerminalJpaEntity> spec =
            tenantSpec(tenantId.value())
                .and(SellerTerminalSpecifications.matching(criteria));

        return TchPageMapper.map(
            repository.findAll(spec, pageRequest.pageable()),
            e -> SellerTerminalSummaryRow.from(mapper.toDomain(e)));
    }

    private Specification<SellerTerminalJpaEntity> tenantSpec(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }
}
