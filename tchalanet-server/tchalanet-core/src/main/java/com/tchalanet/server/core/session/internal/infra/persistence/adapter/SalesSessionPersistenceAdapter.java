package com.tchalanet.server.core.session.internal.infra.persistence.adapter;

import com.tchalanet.server.common.web.error.NotFoundException;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import com.tchalanet.server.core.session.internal.domain.model.SalesSessionStatus;
import com.tchalanet.server.core.session.internal.infra.persistence.SalesSessionJpaRepository;
import com.tchalanet.server.core.session.internal.infra.persistence.SalesSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adapter for SalesSessionReaderPort and SalesSessionWriterPort using JPA.
 */
@Component
@RequiredArgsConstructor
public class SalesSessionPersistenceAdapter implements SalesSessionReaderPort {

    private final SalesSessionJpaRepository jpaRepository;
    private final SalesSessionMapper mapper;

    @Override
    public Optional<SalesSession> findById(TenantId tenantId, SalesSessionId id) {
        return jpaRepository
            .findByTenantIdAndIdAndDeletedAtIsNull(tenantId.value(), id.value())
            .map(mapper::toDomain);
    }

    @Override
    public SalesSession getById(TenantId tenantId, SalesSessionId id) {
        return findById(tenantId, id)
            .orElseThrow(() -> new NotFoundException("Sales session not found: " + id.value()));
    }

    @Override
    public Optional<SalesSession> findOpenByTerminal(TenantId tenantId, TerminalId terminalId) {
        return jpaRepository
            .findByTenantIdAndTerminalIdAndStatusAndDeletedAtIsNull(
                tenantId.value(), terminalId.value(), SalesSessionStatus.OPEN)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<SalesSession> findCurrentOpenByUser(TenantId tenantId, UserId userId) {
        return jpaRepository
            .findCurrentOpenByUser(tenantId.value(), userId.value())
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsForBusinessDate(
        TenantId tenantId,
        OutletId outletId,
        UserId openedBy,
        LocalDate businessDate) {
        return jpaRepository.existsByTenantIdAndOutletIdAndOpenedByAndBusinessDateAndDeletedAtIsNull(
            tenantId.value(), outletId.value(), openedBy.value(), businessDate);
    }

    @Override
    public List<SalesSession> findOpenedSalesSession(
        TenantId tenantId,
        TerminalId terminalId,
        OutletId outletId,
        UserId userId) {

        if (userId != null) {
            return jpaRepository.findCurrentOpenByUser(tenantId.value(), userId.value()).stream()
                .map(mapper::toDomain)
                .toList();
        }

        if (terminalId != null) {
            return jpaRepository
                .findByTenantIdAndTerminalIdAndStatusAndDeletedAtIsNull(
                    tenantId.value(), terminalId.value(), SalesSessionStatus.OPEN)
                .stream()
                .map(mapper::toDomain)
                .toList();
        }

        if (outletId != null) {
            return jpaRepository
                .findByTenantIdAndOutletIdAndStatusAndDeletedAtIsNull(
                    tenantId.value(), outletId.value(), SalesSessionStatus.OPEN)
                .stream()
                .map(mapper::toDomain)
                .toList();
        }

        return jpaRepository
            .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId.value(), SalesSessionStatus.OPEN)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean hasOpenSessions(TenantId tenantId, OutletId outletId) {
        return jpaRepository.existsByTenantIdAndOutletIdAndStatusAndDeletedAtIsNull(
            tenantId.value(), outletId.value(), SalesSessionStatus.OPEN);
    }

    @Override
    public List<SalesSessionId> findSessionIds(OutletId outletId, Instant from, Instant to) {
        return jpaRepository.findIdsByOutletIdAndOpenedAtBetween(outletId.value(), from, to).stream()
            .map(SalesSessionId::of)
            .toList();
    }
}
