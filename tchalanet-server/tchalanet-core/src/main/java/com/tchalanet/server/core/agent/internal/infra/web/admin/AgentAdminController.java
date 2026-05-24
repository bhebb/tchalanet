package com.tchalanet.server.core.agent.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.agent.api.query.ListAgentsQuery;
import com.tchalanet.server.core.agent.api.query.model.AgentSummaryView;
import com.tchalanet.server.core.agent.api.command.*;
import com.tchalanet.server.core.agent.api.model.*;
import com.tchalanet.server.core.agent.api.query.GetAgentViewQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/agents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name="Agents • Admin")
public class AgentAdminController {
  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public record CreateAgentRequest(AgentId parentAgentId, @NotBlank String displayName, @NotNull AgentType type, @NotNull AgentZoneId primaryZoneId, UserId ownerUserId, @NotNull List<AgentZoneId> commercialAllowedZoneIds) {}
  public record AssignUserRequest(@NotNull UserId userId, @NotBlank String relation) {}
  public record UpdateStatusRequest(@NotNull AgentStatus status, String reason) {}

  @PostMapping
  @Operation(summary="Create an affiliate agent")
  public ApiResponse<AgentView> create(@CurrentContext TchRequestContext ctx, @Valid @RequestBody CreateAgentRequest req) {
    return ApiResponse.success(commandBus.execute(new CreateAgentCommand(ctx.effectiveTenantIdRequired(), req.parentAgentId(), req.displayName(), req.type(), req.primaryZoneId(), req.ownerUserId(), req.commercialAllowedZoneIds())));
  }

  @GetMapping("/{agentId}")
  public ApiResponse<AgentView> get(@CurrentContext TchRequestContext ctx, @PathVariable AgentId agentId) {
    return ApiResponse.success(queryBus.ask(new GetAgentViewQuery(ctx.effectiveTenantIdRequired(), agentId)));
  }

  @GetMapping
  public ApiResponse<TchPage<AgentSummaryView>> list(@CurrentContext TchRequestContext ctx, @PageableDefault Pageable pageable) {
    return ApiResponse.success(queryBus.ask(new ListAgentsQuery(pageable)));
  }

  @PostMapping("/{agentId}/users")
  public ApiResponse<Void> assignUser(@CurrentContext TchRequestContext ctx, @PathVariable AgentId agentId, @Valid @RequestBody AssignUserRequest req) {
    commandBus.execute(new AssignUserToAgentCommand(ctx.effectiveTenantIdRequired(), agentId, req.userId(), req.relation()));
    return ApiResponse.success(null);
  }

  @PatchMapping("/{agentId}/status")
  public ApiResponse<AgentView> updateStatus(@CurrentContext TchRequestContext ctx, @PathVariable AgentId agentId, @Valid @RequestBody UpdateStatusRequest req) {
    return ApiResponse.success(commandBus.execute(new UpdateAgentStatusCommand(ctx.effectiveTenantIdRequired(), agentId, req.status(), req.reason())));
  }
}
