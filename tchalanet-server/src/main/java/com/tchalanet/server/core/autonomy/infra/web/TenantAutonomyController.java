package com.tchalanet.server.core.autonomy.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.autonomy.application.command.model.UpsertAutonomyPolicyRuleRuleCommand;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyPolicyRuleQuery;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyPolicyRuleResult;
import com.tchalanet.server.core.autonomy.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/autonomy")
@RequiredArgsConstructor
public class TenantAutonomyController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping
    public GetAutonomyPolicyRuleResult get(@RequestParam UUID tenantId) {
        return queryBus.send(new GetAutonomyPolicyRuleQuery(
            tenantId, AutonomyTargetType.TENANT, tenantId
        ));
    }

    @PutMapping
    public Object upsert(@RequestParam UUID tenantId, @RequestBody UpsertAutonomyRequest req) {
        return commandBus.send(new UpsertAutonomyPolicyRuleRuleCommand(
            tenantId,
            AutonomyTargetType.TENANT,
            tenantId,
            req.level(),
            req.requireApprovalOnBlock(),
            req.approvalRole(),
            req.enabled(),
            req.startsAt(),
            req.endsAt()
        ));
    }

    public record UpsertAutonomyRequest(
        AutonomyLevel level,
        boolean requireApprovalOnBlock,
        ApprovalRole approvalRole,
        boolean enabled,
        Instant startsAt,
        Instant endsAt
    ) {
    }
}
