package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.CloseDueSalesSessionsCommand;
import com.tchalanet.server.core.session.application.command.model.CloseDueSalesSessionsResult;
import com.tchalanet.server.core.session.application.port.out.AutoSessionTargetReaderPort;
import com.tchalanet.server.core.session.application.service.SalesSessionAutoCloser;
import lombok.RequiredArgsConstructor;

import java.time.Clock;


@UseCase
@RequiredArgsConstructor
public class CloseDueSalesSessionsCommandHandler
    implements CommandHandler<CloseDueSalesSessionsCommand, CloseDueSalesSessionsResult> {

    private final AutoSessionTargetReaderPort autoSessionTargetReader;
    private final SalesSessionAutoCloser autoCloser;
    private final Clock clock;

    @Override
    @TchTx
    public CloseDueSalesSessionsResult handle(CloseDueSalesSessionsCommand command) {
        var now = clock.instant();
        var targets = autoSessionTargetReader.findDueCloseTargets(now);

        var closedCount = autoCloser.closeTargets(targets, now);

        return new CloseDueSalesSessionsResult(targets.size(), closedCount, now);
    }
}
