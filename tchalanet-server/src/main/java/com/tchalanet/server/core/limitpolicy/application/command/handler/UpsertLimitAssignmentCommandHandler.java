package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpsertLimitAssignmentCommandHandler
    implements CommandHandler<UpsertLimitAssignmentCommand, UpsertLimitAssignmentResult> {

    private final LimitDefinitionReaderPort defReader;
    private final LimitAssignmentReaderPort asgReader;
    private final LimitAssignmentWriterPort asgWriter;

    @Override
    @TchTx
    public UpsertLimitAssignmentResult handle(UpsertLimitAssignmentCommand c) {
        // basic window validation (structural)
        if (c.startsAt() != null && c.endsAt() != null && !c.startsAt().isBefore(c.endsAt())) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }

        // ensure definition exists (tenant scoped by RLS)
        defReader.findById(c.limitDefinitionId())
            .orElseThrow(() -> new IllegalStateException("LimitDefinition not found: " + c.limitDefinitionId()));

        // upsert by natural key (target + defId), active only
        var existing = asgReader.findByNaturalKey(c.target(), c.limitDefinitionId());

        LimitAssignment toSave =
            existing
                .map(a -> new LimitAssignment(
                    a.id(),
                    a.limitDefinitionId(),
                    a.target(),
                    c.enabled(),
                    c.startsAt(),
                    c.endsAt(),
                    c.paramsOverride(),
                    c.appliesToOverride(),
                    null
                ))
                .orElseGet(() -> new LimitAssignment(
                    null,
                    c.limitDefinitionId(),
                    c.target(),
                    c.enabled(),
                    c.startsAt(),
                    c.endsAt(),
                    c.paramsOverride(),
                    c.appliesToOverride(),
                    null
                ));

        LimitAssignment saved = asgWriter.save(toSave);
        return new UpsertLimitAssignmentResult(saved.id());
    }
}
