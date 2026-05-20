package com.tchalanet.server.core.limitpolicy.internal.application.command.handler.assignment;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentResult;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpsertLimitAssignmentCommandHandler
    implements CommandHandler<UpsertLimitAssignmentCommand, UpsertLimitAssignmentResult> {

    private final LimitAssignmentReaderPort reader;
    private final LimitAssignmentWriterPort writer;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public UpsertLimitAssignmentResult handle(UpsertLimitAssignmentCommand c) {
        validate(c);

        var existing = reader.findByNaturalKey(c.target(), c.ruleKey()).orElse(null);

        var assignment = existing == null
            ? create(c)
            : update(existing, c);

        var saved = writer.save(assignment);
        return new UpsertLimitAssignmentResult(saved.id());
    }

    private LimitAssignment create(UpsertLimitAssignmentCommand c) {
        return LimitAssignment.createNew(
            LimitAssignmentId.of(idGenerator.newUuid()),
            c.ruleKey(),
            c.target(),
            c.enabled(),
            c.onBreach(),
            c.params(),
            c.startsAt(),
            c.endsAt());
    }

    private LimitAssignment update(
        LimitAssignment existing,
        UpsertLimitAssignmentCommand c
    ) {
        return existing.update(
            c.enabled(),
            c.onBreach(),
            c.params(),
            c.startsAt(),
            c.endsAt());
    }

    private void validate(UpsertLimitAssignmentCommand c) {
        if (c.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }

        if (c.ruleKey() == null) {
            throw new IllegalArgumentException("ruleKey is required");
        }

        if (c.target() == null) {
            throw new IllegalArgumentException("limitScopeRef is required");
        }

        if (c.onBreach() == null) {
            throw new IllegalArgumentException("onBreach is required");
        }

        if (c.params() == null) {
            throw new IllegalArgumentException("params is required");
        }

        if (c.startsAt() != null && c.endsAt() != null && !c.startsAt().isBefore(c.endsAt())) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }
}
