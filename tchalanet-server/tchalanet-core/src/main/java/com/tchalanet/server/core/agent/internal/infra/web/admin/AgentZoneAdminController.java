package com.tchalanet.server.core.agent.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.agent.api.command.CreateAgentZoneCommand;
import com.tchalanet.server.core.agent.api.command.SeedDefaultAgentZonesCommand;
import com.tchalanet.server.core.agent.api.model.AgentZoneView;
import com.tchalanet.server.core.agent.api.query.ListAgentZonesQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/agent-zones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
public class AgentZoneAdminController {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    public record CreateZoneRequest(AgentZoneId parentZoneId,
                                    @NotBlank String code,
                                    @NotBlank String name,
                                    @NotBlank String zoneType) {
    }

    @PostMapping
    public ApiResponse<AgentZoneView> create(@CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateZoneRequest req) {
        return ApiResponse.success(commandBus.execute(new CreateAgentZoneCommand(ctx.effectiveTenantIdRequired(), req.parentZoneId(), req.code(), req.name(), req.zoneType())));
    }

    @PostMapping("/seed-haiti")
    public ApiResponse<List<AgentZoneView>> seedHaiti(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(commandBus.execute(new SeedDefaultAgentZonesCommand(ctx.effectiveTenantIdRequired())));
    }

    @GetMapping
    public ApiResponse<List<AgentZoneView>> list(@CurrentContext TchRequestContext ctx, @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponse.success(queryBus.ask(new ListAgentZonesQuery(ctx.effectiveTenantIdRequired(), activeOnly)));
    }
}
