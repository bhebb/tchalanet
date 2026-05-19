package com.tchalanet.server.core.offlinesync.internal.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.grant.RevokeOfflineGrantCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RevokeOfflineGrantCommandHandler
    implements CommandHandler<RevokeOfflineGrantCommand, Void> {

    private final OfflineGrantReaderPort grantReader;
    private final OfflineGrantWriterPort grantWriter;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(RevokeOfflineGrantCommand command) {
        var grant = grantReader.getRequired(command.grantId());
        var revoked = grant.revoke(command.reason(), clock.instant());
        grantWriter.save(revoked);
        log.info("offlinesync: grant {} revoked reason='{}'", command.grantId(), command.reason());
        return null;
    }
}
