package com.tchalanet.server.core.autonomy.infra.web.admin.mapper;

import com.tchalanet.server.core.autonomy.application.query.model.AutonomyOverviewView;
import com.tchalanet.server.core.autonomy.infra.web.admin.model.AutonomyMetaResponse;
import com.tchalanet.server.core.autonomy.infra.web.admin.model.AutonomyOverviewResponse;
import com.tchalanet.server.core.autonomy.infra.web.admin.model.AutonomyRuleResponse;
import org.springframework.stereotype.Component;

@Component
public class AutonomyAdminWebMapper {

    public AutonomyOverviewResponse toResponse(AutonomyOverviewView view) {
        if (view == null) {
            return null;
        }

        return new AutonomyOverviewResponse(
            view.targetType(),
            view.targetId() == null ? null : view.targetId().value().toString(),
            toRuleResponse(view.rule()),
            toMetaResponse(view.meta()));
    }

    private AutonomyRuleResponse toRuleResponse(
        com.tchalanet.server.core.autonomy.application.query.model.AutonomyRule rule
    ) {
        if (rule == null) {
            return null;
        }

        return new AutonomyRuleResponse(
            rule.level(),
            rule.requireApprovalOnBlock(),
            rule.approvalRole(),
            rule.enabled(),
            rule.startsAt(),
            rule.endsAt());
    }

    private AutonomyMetaResponse toMetaResponse(
        com.tchalanet.server.core.autonomy.application.query.model.AutonomyMeta meta
    ) {
        if (meta == null) {
            return null;
        }

        return new AutonomyMetaResponse(
            meta.configured(),
            meta.deleted(),
            meta.ruleId());
    }
}
