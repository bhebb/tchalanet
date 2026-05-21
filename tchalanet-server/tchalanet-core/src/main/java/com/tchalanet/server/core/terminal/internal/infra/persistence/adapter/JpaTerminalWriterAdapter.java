package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalJpaRepository;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalMapper;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalWriterAdapter implements TerminalWriterPort {

    private final TerminalJpaRepository jpaRepository;
    private final TerminalMapper mapper;
    private final TchContextResolver contextResolver;

    @Override
    public Terminal save(Terminal terminal) {
        var existing = jpaRepository.findByTenantIdAndId(terminal.tenantId().value(), terminal.id().value());
        if (existing.isEmpty()) {
            var entity = mapper.toEntity(terminal);
            return mapper.toDomain(jpaRepository.save(entity));
        }

        var entity = existing.get();
        assertImmutableFields(entity, terminal);
        mapper.updateEntity(terminal, entity);
        return mapper.toDomain(entity);
    }

    @Override
    public void setSalesBlocked(
        TerminalId terminalId,
        boolean blocked,
        String reason,
        Instant at,
        UserId performedBy
    ) {
        requireReasonIfBlocked(blocked, reason, "terminal.sales_block_reason_required");

        var entity = getCurrentTenantTerminal(terminalId);

        entity.setSalesBlocked(blocked);
        entity.setSalesBlockReason(normalizeReason(blocked, reason));
        entity.setSalesBlockedAt(blocked ? at : null);
        entity.setSalesBlockedBy(blocked ? performedBy.value() : null);
    }

    @Override
    public void setPayoutBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy) {

        var entity = getCurrentTenantTerminal(terminalId);

        entity.setPayoutBlocked(blocked);
        entity.setPayoutBlockReason(normalizeReason(blocked, reason));
        entity.setPayoutBlockedAt(blocked ? at : null);
        entity.setPayoutBlockedBy(blocked ? performedBy.value() : null);
    }

    @Override
    public void setOfflineBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy) {
        var entity = getCurrentTenantTerminal(terminalId);

        entity.setOfflineBlocked(blocked);
        entity.setOfflineBlockReason(normalizeReason(blocked, reason));
        entity.setOfflineBlockedAt(blocked ? at : null);
        entity.setOfflineBlockedBy(blocked ? performedBy.value() : null);
    }


    private static void requireReasonIfBlocked(boolean blocked, String reason, String code) {
        if (blocked && (reason == null || reason.isBlank())) {
            throw ProblemRest.badRequest(code);
        }
    }

    private static String normalizeReason(boolean blocked, String reason) {
        if (!blocked) {
            return null;
        }
        return reason == null ? null : reason.trim();
    }

    private TerminalJpaEntity getCurrentTenantTerminal(TerminalId terminalId) {
        UUID tenantId = contextResolver.currentOrThrow().effectiveTenantIdRequired().value();
        return jpaRepository.findByTenantIdAndId(tenantId, terminalId.value())
            .orElseThrow(() -> new IllegalStateException(
                "Terminal update target not found: " + terminalId.value()));
    }

    private static void assertImmutableFields(TerminalJpaEntity entity, Terminal terminal) {
        requireSame("terminalId", entity.getId(), terminal.id().value());
        requireSame("tenantId", entity.getTenantId(), terminal.tenantId().value());
        requireSame("outletId", entity.getOutletId(), terminal.outletId().value());
        requireSame("kind", entity.getKind(), terminal.kind());
    }

    private static void requireSame(String field, Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException(
                "Terminal immutable field changed: "
                    + field
                    + " expected="
                    + actual
                    + " actual="
                    + expected);
        }
    }
}
