package com.tchalanet.server.core.limitpolicy.internal.application.command.handler.offline;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.api.command.offline.UpsertOfflineLimitPolicyCommand;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.offline.TenantOfflinePolicyWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpsertOfflineLimitPolicyCommandHandler
    implements CommandHandler<UpsertOfflineLimitPolicyCommand, OfflineLimitPolicy> {

    private final TenantOfflinePolicyWriterPort policyWriter;

    @Override
    @TchTx
    public OfflineLimitPolicy handle(UpsertOfflineLimitPolicyCommand command) {
        var policy = new OfflineLimitPolicy(
            command.offlineEnabled(),
            command.batchSize(),
            command.validityDuration(),
            command.syncAcceptedExtension(),
            command.maxTicketCount(),
            command.maxTotalAmount()
        );
        var saved = policyWriter.upsert(command.tenantId(), policy);
        log.info("limitpolicy: tenant {} offline policy upserted (enabled={}, batch={}, maxTickets={})",
            command.tenantId(), saved.offlineEnabled(), saved.batchSize(), saved.maxTicketCount());
        return saved;
    }
}
