package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.sellerterminal.internal.application.mapper.SellerTerminalViews;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalWriterPort;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
            e -> SellerTerminalViews.summary(mapper.toDomain(e)));
    }

    @Override
    public SellerTerminalCommissionStatsView commissionStats(TenantId tenantId, BigDecimal tenantDefaultRate) {
        if (tenantDefaultRate == null) {
            Object[] row = (Object[]) repository.commissionStatsNoDefault(tenantId.value());
            if (row == null || row[0] == null) return SellerTerminalCommissionStatsView.empty();
            long total = ((Number) row[0]).longValue();
            BigDecimal min = row[1] != null ? (BigDecimal) row[1] : null;
            BigDecimal max = row[2] != null ? (BigDecimal) row[2] : null;
            BigDecimal avg = row[3] != null ? new BigDecimal(row[3].toString()) : null;
            return new SellerTerminalCommissionStatsView(total, 0L, total, min, max, avg);
        }

        Object[] row = (Object[]) repository.commissionStats(tenantId.value(), tenantDefaultRate);
        if (row == null || row[0] == null) return SellerTerminalCommissionStatsView.empty();

        long total = ((Number) row[0]).longValue();
        long atDefault = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        BigDecimal min = row[2] != null ? (BigDecimal) row[2] : null;
        BigDecimal max = row[3] != null ? (BigDecimal) row[3] : null;
        BigDecimal avg = row[4] != null ? new BigDecimal(row[4].toString()) : null;

        return new SellerTerminalCommissionStatsView(
            total, atDefault, total - atDefault, min, max, avg);
    }

    private Specification<SellerTerminalJpaEntity> tenantSpec(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }
}
