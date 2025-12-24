package com.tchalanet.server.core.autonomy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.autonomy.application.command.model.UpsertAutonomyPolicyRuleRuleCommand;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRuleRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@UseCase
@Component
@RequiredArgsConstructor
public class UpsertAutonomyPolicyRuleRuleCommandHandler implements CommandHandler<UpsertAutonomyPolicyRuleRuleCommand, AutonomyPolicyRuleRule> {

    private final AutonomyPolicyRuleRepositoryPort repository;

    @Override
    public AutonomyPolicyRuleRule handle(UpsertAutonomyPolicyRuleRuleCommand command) {
        Optional<AutonomyPolicyRuleRule> existing = repository.findByTarget(command.tenantId(), command.targetType(), command.targetId());

        if (existing.isPresent()) {
            var old = existing.get();
            var updated = new AutonomyPolicyRuleRule(
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
            var newPolicy = new AutonomyPolicyRuleRule(
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
