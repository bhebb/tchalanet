package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Component
@RequiredArgsConstructor
public class DeleteLimitAssignmentCommandHandler implements CommandHandler<DeleteLimitAssignmentCommand, Void> {

    private final LimitAssignmentReaderPort reader;
    private final LimitAssignmentWriterPort writer;

    @Override
    @Transactional
    public Void handle(DeleteLimitAssignmentCommand cmd) {
        var assignment = reader.findById(cmd.tenantId(), cmd.assignmentId())
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));
        if (!assignment.targetType().equals(cmd.targetType()) || !assignment.targetId().equals(cmd.targetId())) {
            throw new IllegalStateException("Assignment does not belong to the specified target");
        }
        writer.softDelete(cmd.tenantId(), cmd.assignmentId());
        return null;
    }
}
