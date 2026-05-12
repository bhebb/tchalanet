package com.tchalanet.server.core.autonomy.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.application.command.model.DeleteAutonomyRuleCommand;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyRuleReaderPort;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyRuleWriterPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class DeleteAutonomyRuleCommandHandler
    implements VoidCommandHandler<DeleteAutonomyRuleCommand> {

    private final AutonomyRuleReaderPort reader;
    private final AutonomyRuleWriterPort writer;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(DeleteAutonomyRuleCommand cmd) {
        var effectiveTargetId =
            cmd.targetType() == AutonomyTargetType.TENANT
                ? cmd.tenantId().value()
                : cmd.targetId().value();

        var rule = reader.findByTargetActiveOnly(cmd.targetType(), effectiveTargetId)
            .orElseThrow(() -> new IllegalArgumentException("autonomy rule not found"));

        writer.softDelete(rule.id(), Instant.now(clock));
    }
}
