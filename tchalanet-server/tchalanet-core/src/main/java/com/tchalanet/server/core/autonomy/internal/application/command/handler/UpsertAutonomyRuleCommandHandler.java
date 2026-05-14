package com.tchalanet.server.core.autonomy.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;
import com.tchalanet.server.common.types.id.AutonomyPolicyRuleId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.autonomy.api.command.UpsertAutonomyRuleCommand;
import com.tchalanet.server.core.autonomy.internal.application.port.out.AutonomyRuleReaderPort;
import com.tchalanet.server.core.autonomy.internal.application.port.out.AutonomyRuleWriterPort;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyTargetId;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpsertAutonomyRuleCommandHandler
    implements CommandHandler<UpsertAutonomyRuleCommand, AutonomyPolicyRuleId> {

    private final AutonomyRuleReaderPort reader;
    private final AutonomyRuleWriterPort writer;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public AutonomyPolicyRuleId handle(UpsertAutonomyRuleCommand cmd) {
        validate(cmd);

        var effectiveTargetUuid = effectiveTargetUuid(cmd);

        var existing = reader
            .findByTargetActiveOnly(cmd.targetType(), effectiveTargetUuid)
            .orElse(null);

        var rule = existing == null
            ? create(cmd, effectiveTargetUuid)
            : update(existing, cmd);

        return writer.save(rule).id();
    }

    private UUID effectiveTargetUuid(UpsertAutonomyRuleCommand cmd) {
        if (cmd.targetType() == AutonomyTargetType.TENANT) {
            return cmd.tenantId().value();
        }

        if (cmd.targetId() == null) {
            throw new IllegalArgumentException("targetId is required for " + cmd.targetType());
        }

        return cmd.targetId().value();
    }

    private void validate(UpsertAutonomyRuleCommand cmd) {
        if (cmd.tenantId() == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (cmd.targetType() == null) {
            throw new IllegalArgumentException("targetType is required");
        }
        if (cmd.level() == null) {
            throw new IllegalArgumentException("level is required");
        }
        if (cmd.requireApprovalOnBlock() && cmd.approvalRole() == null) {
            throw new IllegalArgumentException("approvalRole is required when approval is required");
        }
        if (cmd.startsAt() != null && cmd.endsAt() != null && !cmd.startsAt().isBefore(cmd.endsAt())) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }

    private AutonomyPolicyRule create(
        UpsertAutonomyRuleCommand cmd,
        UUID effectiveTargetUuid
    ) {
        return AutonomyPolicyRule.createNew(
            AutonomyPolicyRuleId.of(idGenerator.newUuid()),
            cmd.targetType(),
            AutonomyTargetId.of(effectiveTargetUuid),
            cmd.level(),
            cmd.requireApprovalOnBlock(),
            cmd.approvalRole(),
            cmd.enabled(),
            cmd.startsAt(),
            cmd.endsAt());
    }

    private AutonomyPolicyRule update(
        AutonomyPolicyRule existing,
        UpsertAutonomyRuleCommand cmd
    ) {
        return AutonomyPolicyRule.update(
            existing.id(),
            existing.targetType(),
            existing.targetId(),
            cmd.level(),
            cmd.requireApprovalOnBlock(),
            cmd.approvalRole(),
            cmd.enabled(),
            cmd.startsAt(),
            cmd.endsAt());
    }
}
