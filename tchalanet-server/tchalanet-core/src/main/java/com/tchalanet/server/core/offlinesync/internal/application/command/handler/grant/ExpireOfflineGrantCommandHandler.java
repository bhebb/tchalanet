package com.tchalanet.server.core.offlinesync.internal.application.command.handler.grant;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.command.grant.ExpireOfflineGrantCommand;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@UseCase
@RequiredArgsConstructor
@Slf4j
public class ExpireOfflineGrantCommandHandler
    implements CommandHandler<ExpireOfflineGrantCommand, Void> {

    private final OfflineGrantReaderPort grantReader;
    private final OfflineGrantWriterPort grantWriter;

    @Override
    @TchTx
    public Void handle(ExpireOfflineGrantCommand command) {
        var grant = grantReader.getRequired(command.grantId());
        grantWriter.save(grant.expire());
        return null;
    }
}
