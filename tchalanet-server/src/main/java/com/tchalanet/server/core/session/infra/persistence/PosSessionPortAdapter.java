package com.tchalanet.server.core.session.infra.persistence;

import com.tchalanet.server.core.session.application.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.domain.model.PosSessionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosSessionPortAdapter implements PosSessionRepositoryPort {

    private final com.tchalanet.server.core.pos.application.port.out.PosSessionRepositoryPort posPort;

    @Override
    public PosSession save(PosSession session) {
        com.tchalanet.server.core.pos.domain.model.PosSession p = toPosDomain(session);
        com.tchalanet.server.core.pos.domain.model.PosSession saved = posPort.save(p);
        return toSessionDomain(saved);
    }

    @Override
    public Optional<PosSession> findById(UUID id) {
        return posPort.findById(id).map(this::toSessionDomain);
    }

    @Override
    public Optional<PosSession> findOpenByTerminal(UUID tenantId, UUID terminalId) {
        var p = posPort.findByTenantIdAndTerminalIdAndStatus(tenantId, terminalId, com.tchalanet.server.core.pos.domain.model.PosSessionStatus.OPEN);
        return p.map(this::toSessionDomain);
    }

    @Override
    public List<PosSession> findByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, PosSessionStatus status) {
        com.tchalanet.server.core.pos.domain.model.PosSessionStatus ps = mapToPosStatus(status);
        return posPort.findByTenantIdAndUserIdAndStatus(tenantId, userId, ps).stream().map(this::toSessionDomain).collect(Collectors.toList());
    }

    // Mapping helpers
    private com.tchalanet.server.core.pos.domain.model.PosSession toPosDomain(PosSession s) {
        if (s == null) return null;
        com.tchalanet.server.core.pos.domain.model.PosSessionStatus posStatus = mapToPosStatus(s.status());
        java.math.BigDecimal totalTicketsAmount = s.totalStake() != null ? s.totalStake() : null;
        java.math.BigDecimal totalPayoutAmount = s.totalPayout();
        return com.tchalanet.server.core.pos.domain.model.PosSession.load(
            s.id(),
            s.tenantId(),
            s.terminalId(),
            s.userId(),
            posStatus,
            s.openedAt(),
            s.closedAt(),
            s.openedAt(), // lastActivityAt: fallback to openedAt
            s.openingFloat(),
            s.closingAmount(),
            totalTicketsAmount,
            totalPayoutAmount,
            s.grossMargin() != null ? s.grossMargin() : java.math.BigDecimal.ZERO
        );
    }

    private PosSession toSessionDomain(com.tchalanet.server.core.pos.domain.model.PosSession p) {
        if (p == null) return null;
        PosSessionStatus ss = mapToSessionStatus(p.getStatus());
        return new PosSession(
            p.getId(),
            p.getTenantId(),
            null, // outletId unknown here
            p.getTerminalId(),
            p.getUserId(),
            ss,
            p.getOpenedAt(),
            p.getClosedAt(),
            p.getOpeningFloat(),
            p.getClosingAmount(),
            p.getTotalTicketsAmount() != null ? p.getTotalTicketsAmount().longValue() : null,
            p.getTotalTicketsAmount(),
            p.getTotalPayoutAmount(),
            p.getGrossMargin(),
            java.util.Map.of(),
            0L
        );
    }

    private com.tchalanet.server.core.pos.domain.model.PosSessionStatus mapToPosStatus(PosSessionStatus s) {
        if (s == null) return com.tchalanet.server.core.pos.domain.model.PosSessionStatus.OPEN;
        try {
            return com.tchalanet.server.core.pos.domain.model.PosSessionStatus.valueOf(s.name());
        } catch (Exception ex) {
            return com.tchalanet.server.core.pos.domain.model.PosSessionStatus.OPEN;
        }
    }

    private PosSessionStatus mapToSessionStatus(com.tchalanet.server.core.pos.domain.model.PosSessionStatus s) {
        if (s == null) return PosSessionStatus.OPEN;
        try {
            return PosSessionStatus.valueOf(s.name());
        } catch (Exception ex) {
            return PosSessionStatus.OPEN;
        }
    }
}
