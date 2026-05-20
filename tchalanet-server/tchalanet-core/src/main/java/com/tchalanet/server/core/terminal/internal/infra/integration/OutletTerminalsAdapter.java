package com.tchalanet.server.core.terminal.internal.infra.integration;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletTerminalReaderPort;
import com.tchalanet.server.core.outlet.api.query.OutletTerminalView;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletTerminalsAdapter implements OutletTerminalReaderPort {

    private final TerminalJpaRepository repo;

    @Override
    public List<OutletTerminalView> listTerminalsByOutlet(OutletId outletId) {
        return repo.findByOutletId(outletId.value()).stream()
            .map(this::toView)
            .toList();
    }

    @Override
    public long countTerminalsByOutlet(OutletId outletId) {
        return repo.countByOutletId(outletId.value());
    }

    private OutletTerminalView toView(TerminalJpaEntity terminal) {
        return new OutletTerminalView(
            TerminalId.of(terminal.getId()),
            OutletId.of(terminal.getOutletId()),
            terminal.getLabel(),
            terminal.getKind().name(),
            terminal.getState().name(),
            terminal.getSyncState(),
            UserId.nullableOf(terminal.getAssignedUserId()),
            terminal.isAutoSessionEnabled(),
            terminal.getLastSeen());
    }
}
