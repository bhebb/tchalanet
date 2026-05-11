package com.tchalanet.server.core.limitpolicy.application.command.handler.assignment;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.assignment.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.assignment.DeleteLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.application.port.out.assignment.LimitAssignmentWriterPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
public class DeleteLimitAssignmentCommandHandler
    implements CommandHandler<DeleteLimitAssignmentCommand, DeleteLimitAssignmentResult> {

    private final LimitAssignmentWriterPort writer;

    private final Clock clock;

    @Override
    @TchTx
    public DeleteLimitAssignmentResult handle(DeleteLimitAssignmentCommand c) {
        writer.softDelete(c.id(), clock.instant());
        return new DeleteLimitAssignmentResult(c.id());
    }
}
