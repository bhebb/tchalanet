package com.tchalanet.server.core.autonomy.application.command.handler;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.application.command.model.UpsertAutonomyPolicyRuleCommand;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyPolicyRuleRepositoryPort;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyPolicyRuleId;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpsertAutonomyPolicyRuleCommandHandler {

    private final AutonomyPolicyRuleRepositoryPort repo;
    private final TchContextResolver tchContextResolver;

    /**
     * Handle upsert command. Tenant is implicit (RLS) — caller must ensure RequestContext is set.
     */
    @Transactional
    public AutonomyPolicyRule handle(UpsertAutonomyPolicyRuleCommand cmd) {
        Assert.notNull(cmd, "command required");

        // Derive effective target id for TENANT when not provided
        UUID effectiveTargetUuid = cmd.getTargetId() == null ? null : cmd.getTargetId().value();
        if (cmd.getTargetType() == AutonomyTargetType.TENANT && effectiveTargetUuid == null) {
            effectiveTargetUuid = tchContextResolver.currentOrThrow().tenantUuid();
        }

        // For non-tenant targets targetId must be provided
        if (cmd.getTargetType() != AutonomyTargetType.TENANT && effectiveTargetUuid == null) {
            throw new IllegalArgumentException("targetId is required for targetType " + cmd.getTargetType());
        }

        // Validate window: startsAt < endsAt if both present
        if (cmd.getStartsAt() != null && cmd.getEndsAt() != null) {
            if (!cmd.getStartsAt().isBefore(cmd.getEndsAt())) {
                throw new IllegalArgumentException("startsAt must be before endsAt");
            }
        }

        // Defaults
        AutonomyLevel levelCommon = cmd.getLevel();
        ApprovalRole approvalRole = cmd.getApprovalRole();
        if (cmd.isRequireApprovalOnBlock() && approvalRole == null) {
            approvalRole = ApprovalRole.OPERATOR;
        }

        // Find existing active rule for this target (upsert-safe read that ignores deleted rows)
        Optional<AutonomyPolicyRule> existingActive = repo.findByTargetActiveOnly(cmd.getTargetType(), effectiveTargetUuid);

        // If active exists -> update it (with optional version check)
        if (existingActive.isPresent()) {
            AutonomyPolicyRule r = existingActive.get();

            // Optimistic version check when provided
            if (cmd.getExpectedVersion() != null && !cmd.getExpectedVersion().equals(r.getVersion())) {
                throw new org.springframework.dao.OptimisticLockingFailureException("Version mismatch");
            }

            r.setLevel(levelCommon);
            r.setRequireApprovalOnBlock(cmd.isRequireApprovalOnBlock());
            r.setApprovalRole(approvalRole);
            r.setEnabled(cmd.isEnabled());
            r.setStartsAt(cmd.getStartsAt());
            r.setEndsAt(cmd.getEndsAt());
            return repo.save(r);
        }

        // No active row exists (there may be soft-deleted rows) -> create a fresh row (no resurrection)
        AutonomyPolicyRule created = new AutonomyPolicyRule();
        created.setId(AutonomyPolicyRuleId.of(UUID.randomUUID()));
        created.setTargetType(cmd.getTargetType());
        created.setTargetId(AutonomyTargetId.of(effectiveTargetUuid));
        created.setLevel(levelCommon);
        created.setRequireApprovalOnBlock(cmd.isRequireApprovalOnBlock());
        created.setApprovalRole(approvalRole);
        created.setEnabled(cmd.isEnabled());
        created.setStartsAt(cmd.getStartsAt());
        created.setEndsAt(cmd.getEndsAt());
        created.setVersion(0L);

        return repo.save(created);
    }
}
