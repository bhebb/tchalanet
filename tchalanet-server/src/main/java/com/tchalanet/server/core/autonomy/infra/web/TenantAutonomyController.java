package com.tchalanet.server.core.autonomy.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.autonomy.application.command.model.UpsertAutonomyPolicyRuleCommand;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyPolicyRuleQuery;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyPolicyRuleResult;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(value = HttpStatus.OK)
    public GetAutonomyPolicyRuleResult get(@RequestParam TenantId tenantId) {
        return queryBus.send(new GetAutonomyPolicyRuleQuery(
            tenantId, AutonomyTargetType.TENANT, tenantId.uuid()
        ));
    }

    @PutMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public AutonomyPolicyRule upsert(@RequestParam TenantId tenantId, @RequestBody UpsertAutonomyRequest req) {
        return commandBus.send(new UpsertAutonomyPolicyRuleCommand(
            tenantId,
            AutonomyTargetType.TENANT,
            tenantId.uuid(),
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
