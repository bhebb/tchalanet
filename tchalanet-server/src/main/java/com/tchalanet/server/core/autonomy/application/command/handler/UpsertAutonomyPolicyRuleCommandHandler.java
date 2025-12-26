package com.tchalanet.server.core.autonomy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.autonomy.application.command.model.UpsertAutonomyPolicyRuleCommand;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@UseCase
@Component
@RequiredArgsConstructor
public class UpsertAutonomyPolicyRuleCommandHandler implements CommandHandler<UpsertAutonomyPolicyRuleCommand, AutonomyPolicyRule> {

    private final AutonomyPolicyRuleRepositoryPort repository;

    @Override
    public AutonomyPolicyRule handle(UpsertAutonomyPolicyRuleCommand command) {
        Optional<AutonomyPolicyRule> existing = repository.findByTarget(command.tenantId(), command.targetType(), command.targetId());

        if (existing.isPresent()) {
            var old = existing.get();
            var updated = new AutonomyPolicyRule(
                old.id(),
                command.tenantId(),
                command.targetType(),
                command.targetId(),
                command.level(),
                command.requireApprovalOnBlock(),
                command.approvalRole(),
                command.enabled(),
                command.startsAt(),
                command.endsAt(),
                old.version() + 1
            );
            return repository.save(updated);
        } else {
            var newPolicy = new AutonomyPolicyRule(
                null, // id will be generated
                command.tenantId(),
                command.targetType(),
                command.targetId(),
                command.level(),
                command.requireApprovalOnBlock(),
                command.approvalRole(),
                command.enabled(),
                command.startsAt(),
                command.endsAt(),
                0L
            );
            return repository.save(newPolicy);
        }
    }
}
