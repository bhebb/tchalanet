package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.CloseOutletOpenSalesSessionsCommand;
import com.tchalanet.server.core.session.application.command.model.CloseOutletOpenSalesSessionsResult;
import com.tchalanet.server.core.session.application.port.out.AutoSessionTargetReaderPort;
import com.tchalanet.server.core.session.application.service.SalesSessionAutoCloser;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CloseOutletOpenSalesSessionsCommandHandler
    implements CommandHandler<CloseOutletOpenSalesSessionsCommand, CloseOutletOpenSalesSessionsResult> {

    private final AutoSessionTargetReaderPort autoSessionTargetReader;
    private final SalesSessionAutoCloser autoCloser;

    @Override
    @TchTx
    public CloseOutletOpenSalesSessionsResult handle(CloseOutletOpenSalesSessionsCommand command) {
        var targets =
            autoSessionTargetReader.findOpenCloseTargetsByOutlet(
                command.tenantId(),
                command.outletId(),
                command.closedAt(),
                command.closedBy(),
                command.reason());

        var closedCount = autoCloser.closeTargets(targets, command.closedAt());

        return new CloseOutletOpenSalesSessionsResult(
            targets.size(),
            closedCount,
            command.closedAt());
    }
}
