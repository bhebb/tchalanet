package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.core.session.application.command.model.OpenDueSalesSessionsCommand;
import com.tchalanet.server.core.session.application.command.model.OpenDueSalesSessionsResult;
import com.tchalanet.server.core.session.application.port.out.AutoSessionTargetReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.domain.event.SalesSessionOpenedEvent;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
public class OpenDueSalesSessionsCommandHandler
    implements CommandHandler<OpenDueSalesSessionsCommand, OpenDueSalesSessionsResult> {

    private final AutoSessionTargetReaderPort autoSessionTargetReaderPort;
    private final SalesSessionReaderPort sessionReader;
    private final SalesSessionWriterPort sessionWriter;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public OpenDueSalesSessionsResult handle(OpenDueSalesSessionsCommand command) {
        var now = clock.instant();
        int countTargets = 0;
        int opened = 0;
        int skipped = 0;

        var targets = autoSessionTargetReaderPort.findDueOpenTargets(now);

        for (var target : targets) {
            countTargets++;
            if (sessionReader.existsForBusinessDate(
                target.tenantId(), target.outletId(), target.openedBy(), target.businessDate())) {
                skipped++;
                continue;
            }

            var sessionId = SalesSessionId.of(idGenerator.newUuid());
            var session =
                SalesSession.open(
                    sessionId,
                    target.tenantId(),
                    target.outletId(),
                    target.terminalId(),
                    target.openedBy(),
                    target.businessDate(),
                    now,
                    target.openingFloatCents());

            var saved = sessionWriter.save(session);
            opened++;
            var event =
                new SalesSessionOpenedEvent(
                    EventId.of(idGenerator.newUuid()),
                    now,
                    target.tenantId(),
                    saved.id(),
                    saved.outletId(),
                    saved.terminalId(),
                    target.openedBy());

            AfterCommit.run(() -> events.publish(event));
        }

        return new OpenDueSalesSessionsResult(
            countTargets, opened, skipped);
    }
}
