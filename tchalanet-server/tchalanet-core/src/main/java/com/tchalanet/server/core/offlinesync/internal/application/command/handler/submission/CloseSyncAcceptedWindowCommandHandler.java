package com.tchalanet.server.core.offlinesync.internal.application.command.handler.submission;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.submission.CloseSyncAcceptedWindowCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionSyncWindowWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CloseSyncAcceptedWindowCommandHandler
    implements CommandHandler<CloseSyncAcceptedWindowCommand, Void> {

    private final OfflineSubmissionSyncWindowWriterPort writer;

    @Override
    @TchTx
    public Void handle(CloseSyncAcceptedWindowCommand command) {
        int closedCount = writer.closeWindowForTenant(command.now());
        if (closedCount > 0) {
            log.info(
                "offlinesync: closing window for {} submissions stuck in RECEIVED past their grant (tenant={})",
                closedCount,
                command.tenantId());
        }
        return null;
    }
}

