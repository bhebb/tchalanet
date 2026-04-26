package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawCommand;
import com.tchalanet.server.core.draw.application.query.model.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.application.query.model.ListDrawsQuery;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Admin • Draws")
public class DrawAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final DrawAdminWebMapper mapper;
  private final TchContextResolver contextResolver;

  @Operation(summary = "List draws (admin)")
  @GetMapping
  public ApiResponse<List<DrawSummaryResponse>> listDraws() {
    var holder = contextResolver.currentOrNull();
    var tenantId = TenantId.of(holder != null ? holder.tenantUuid() : null);
    List<DrawSummary> summaries = queryBus.send(new ListDrawsQuery(tenantId, null, null, null));
    var responses = summaries.stream().map(mapper::toDrawSummaryResponse).toList();
    return ApiResponse.success(responses);
  }

  @Operation(summary = "Create a draw (admin)")
  @PostMapping
  public ApiResponse<DrawSummaryResponse> createDraw(@RequestBody CreateDrawRequest request) {
    CreateDrawCommand command = mapper.toCreateDrawCommand(request);
    DrawSummary summary = commandBus.send(command);
    return ApiResponse.success(mapper.toDrawSummaryResponse(summary));
  }

  @Operation(summary = "Update a draw (admin)")
  @PutMapping("/{drawId}")
  public ApiResponse<DrawSummaryResponse> updateDraw(
      @PathVariable DrawId drawId,
      @RequestParam TenantId tenantId,
      @RequestBody UpdateDrawRequest request) {
    if (drawId != request.drawId()) {
      throw ProblemRest.badRequest("Path drawId does not match body drawId");
    }
    if (tenantId != request.tenantId()) {
      throw ProblemRest.badRequest("Path tenantId does not match body tenantId");
    }
    var command = mapper.toUpdateDrawCommand(request);
    commandBus.send(command);
    DrawSummary summary = queryBus.send(new GetDrawByIdQuery(drawId));
    return ApiResponse.success(mapper.toDrawSummaryResponse(summary));
  }
}
