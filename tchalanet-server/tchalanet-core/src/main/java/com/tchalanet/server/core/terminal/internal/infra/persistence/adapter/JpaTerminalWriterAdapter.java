package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalJpaRepository;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JpaTerminalWriterAdapter implements TerminalWriterPort {

    private final TerminalJpaRepository jpaRepository;
    private final TerminalMapper mapper;

    @Override
    public Terminal save(Terminal terminal) {
        var entity =
            jpaRepository.findById(terminal.id().value())
                .orElseGet(TerminalJpaEntity::new);

        mapper.updateEntity(terminal, entity);

        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
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

        var entity =
            jpaRepository.getReferenceById(terminalId.value());

        entity.setSalesBlocked(blocked);
        entity.setSalesBlockReason(normalizeReason(blocked, reason));
        entity.setSalesBlockedAt(blocked ? at : null);
        entity.setSalesBlockedBy(blocked ? performedBy.value() : null);
        jpaRepository.save(entity);
    }

    @Override
    public void setPayoutBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy) {

        var entity =
            jpaRepository.getReferenceById(terminalId.value());

        entity.setPayoutBlocked(blocked);
        entity.setPayoutBlockReason(normalizeReason(blocked, reason));
        entity.setPayoutBlockedAt(blocked ? at : null);
        entity.setPayoutBlockedBy(blocked ? performedBy.value() : null);
        jpaRepository.save(entity);
    }

    @Override
    public void setOfflineBlocked(TerminalId terminalId, boolean blocked, String reason, Instant at, UserId performedBy) {
        var entity =
            jpaRepository.getReferenceById(terminalId.value());

        entity.setOfflineBlocked(blocked);
        entity.setOfflineBlockReason(normalizeReason(blocked, reason));
        entity.setOfflineBlockedAt(blocked ? at : null);
        entity.setOfflineBlockedBy(blocked ? performedBy.value() : null);
        jpaRepository.save(entity);
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
}
