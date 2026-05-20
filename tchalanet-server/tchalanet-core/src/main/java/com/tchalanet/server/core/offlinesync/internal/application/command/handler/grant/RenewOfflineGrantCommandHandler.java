package com.tchalanet.server.core.offlinesync.internal.application.command.handler.grant;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.grant.RenewOfflineGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.grant.RequestOfflineGrantCommand;
import com.tchalanet.server.core.offlinesync.api.command.grant.RequestOfflineGrantResult;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

/**
 * Renews an offline grant: revokes the current one (so the device knows to discard its
 * cached codes) and immediately issues a fresh one with the same operational scope.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class RenewOfflineGrantCommandHandler
    implements CommandHandler<RenewOfflineGrantCommand, RequestOfflineGrantResult> {

    private final OfflineGrantReaderPort grantReader;
    private final OfflineGrantWriterPort grantWriter;
    private final CommandBus commandBus;
    private final Clock clock;

    @Override
    @TchTx
    public RequestOfflineGrantResult handle(RenewOfflineGrantCommand command) {
        var current = grantReader.getRequired(command.currentGrantId());

        if (current.status().name().equals("ACTIVE")) {
            grantWriter.save(current.revoke("superseded by renewal", clock.instant()));
        }

        Command<RequestOfflineGrantResult> issue = new RequestOfflineGrantCommand(
            current.tenantId(),
            current.identity().sellerUserId(),
            current.identity().terminalId(),
            current.identity().outletId(),
            current.identity().salesSessionId(),
            current.device().deviceId(),
            current.device().devicePublicKey(),
            current.device().keyId()
        );
        log.info("offlinesync: renewing grant {} for seller {}",
            command.currentGrantId(), current.identity().sellerUserId());
        return commandBus.execute(issue);
    }
}
