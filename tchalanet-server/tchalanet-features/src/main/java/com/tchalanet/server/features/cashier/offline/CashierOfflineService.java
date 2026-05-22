package com.tchalanet.server.features.cashier.offline;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesCommand;
import com.tchalanet.server.core.offlinesync.api.command.sync.SyncOfflineSalesResult;
import com.tchalanet.server.core.offlinesync.api.query.grant.GetCurrentOfflineGrantQuery;
import com.tchalanet.server.core.offlinesync.api.query.grant.OfflineGrantView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashierOfflineService {

    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public OfflineGrantView currentGrant(TchRequestContext ctx, TerminalId terminalId, UUID deviceId) {
        return queryBus.ask(new GetCurrentOfflineGrantQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.userId(),
            terminalId,
            deviceId
        ));
    }

    public SyncOfflineSalesResult submit(TchRequestContext ctx, CashierOfflineSyncRequest request) {
        var trusted = ctx.operationalContext() != null
            && ctx.operationalContext().trust() == OperationalContextTrust.STRONG;
        return commandBus.execute(new SyncOfflineSalesCommand(
            ctx.effectiveTenantIdRequired(),
            request.grantId(),
            request.clientBatchId(),
            request.batchPayloadHash(),
            request.submissions(),
            trusted
        ));
    }
}
