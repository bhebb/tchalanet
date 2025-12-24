package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.CreateLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Component
@RequiredArgsConstructor
public class CreateLimitAssignmentCommandHandler implements CommandHandler<CreateLimitAssignmentCommand, LimitAssignment> {

    private final LimitAssignmentReaderPort reader;
    private final LimitAssignmentWriterPort writer;

    @Override
    @Transactional
    public LimitAssignment handle(CreateLimitAssignmentCommand cmd) {
        if (reader.existsByTenantAndLimitAndTarget(cmd.tenantId(), cmd.limitDefinitionId(), cmd.targetType().name(), cmd.targetId())) {
            throw new IllegalStateException("Assignment already exists");
        }

        var assignment = new LimitAssignment(
                null,
                cmd.tenantId(),
                cmd.limitDefinitionId(),
                cmd.targetType(),
                cmd.targetId(),
                cmd.enabled(),
                cmd.startsAt(),
                cmd.endsAt(),
                0L
        );

        return writer.save(assignment);
    }
}
